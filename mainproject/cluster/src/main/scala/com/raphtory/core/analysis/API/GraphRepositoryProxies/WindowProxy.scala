package com.raphtory.core.analysis.API.GraphRepositoryProxies

import akka.actor.ActorContext
import com.raphtory.core.analysis.API.VertexVisitor
import com.raphtory.core.analysis.API.{ManagerCount, WorkerID}
import com.raphtory.core.model.graphentities.Vertex
import com.raphtory.core.storage.EntityStorage

import scala.collection.mutable.ArrayBuffer
import scala.collection.parallel.{ParIterable, ParSet}
import scala.collection.parallel.mutable.ParTrieMap

class WindowProxy(jobID:String, superstep:Int, timestamp:Long, windowSize:Long, workerID:Int,storage:EntityStorage,managerCount:ManagerCount) extends LiveProxy(jobID,superstep,timestamp,windowSize,workerID,storage,managerCount) {

  private var setWindow = windowSize
  private var keySet:ParTrieMap[Long,Vertex] = storage.vertices.filter(v=> v._2.aliveAtWithWindow(timestamp,windowSize))
  private var TotalKeySize = keySet.size
  var timeTest = ArrayBuffer[Long]()

  override def getVerticesSet(): ParTrieMap[Long,Vertex] = keySet


  override def job() = jobID+timestamp+setWindow


  override def getVertex(v : Vertex)(implicit context : ActorContext, managerCount : ManagerCount) : VertexVisitor = {
     new VertexVisitor(v.viewAtWithWindow(timestamp,setWindow),job(),superstep,this,timestamp,setWindow)

  }

  override def latestTime:Long = timestamp

  def shrinkWindow(newWindowSize:Long) = {
    setWindow = newWindowSize
    keySet = keySet.filter(v=> (v._2).aliveAtWithWindow(timestamp,setWindow))
    TotalKeySize = keySet.size + TotalKeySize
    //println(s"$workerID $timestamp $newWindowSize keyset prior $x keyset after ${keySet.size}")
  }

  override def checkVotes(workerID: Int):Boolean = {
    TotalKeySize == voteCount.get
  }
}