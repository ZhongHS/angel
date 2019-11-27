/*
 * Tencent is pleased to support the open source community by making Angel available.
 *
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/Apache-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.tencent.angel.spark.ml.graph.commonfriends

class CommonFriendsPartitionV2(val edges: Array[(Int, Int)]) extends Serializable {

  lazy val numEdges: Int = {
    val numEdges = edges.length
    println(s"number of edges on partition = $numEdges")
    numEdges
  }

  def makeBatchIterator(batchSize: Int): Iterator[(Int, Int)] = new Iterator[(Int, Int)] {
    var index = 0

    override def next(): (Int, Int) = {
      val preIndex = index
      index = index + batchSize
      (preIndex, math.min(index, numEdges))
    }

    override def hasNext: Boolean = {
      index < numEdges
    }
  }

  def runEachPartition(psModel: CommonFriendsPSModel, batchSize: Int): Iterator[((Int, Int), Int)] = {
    var curTime = System.currentTimeMillis()
    println(s"num of edges: $numEdges")

    makeBatchIterator(batchSize).flatMap { case (startIdx, endIdx) =>
      println(s"process a batch of ${startIdx} to ${endIdx - 1} edges")
      val pullNodes = edges.slice(startIdx, endIdx).flatMap( t => Array(t._1, t._2)).distinct
      val neighborsNodesMap = psModel.getNeighborTable(pullNodes)
      (startIdx until endIdx).toIterator.flatMap { idx =>
        val edge = edges(idx)
        val srcNode = edge._1
        val dstNode = edge._2
        val srcNeighbors = neighborsNodesMap.get(srcNode)
        val dstNeighbors = neighborsNodesMap.get(dstNode)
        Iterator.single(((srcNode, dstNode), srcNeighbors.intersect(dstNeighbors).length))
      }
    }
  }

}