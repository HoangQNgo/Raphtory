package com.raphtory.examples.gabMining.actors

import com.raphtory.core.analysis.Analyser
import com.raphtory.core.components.AnalysisManager.LiveAnalysisManager
import com.raphtory.examples.gabMining.analysis.GabMiningDensityAnalyser
import com.raphtory.examples.gabMining.utils.writeToFile

import scala.collection.mutable.ArrayBuffer

class GabMiningDensityLAM(jobID: String) extends LiveAnalysisManager(jobID){
  val writing=new writeToFile()
  override protected def defineMaxSteps(): Int = 1

  override protected def generateAnalyzer: Analyser = new GabMiningDensityAnalyser()

  override protected def processResults(): Unit = {

    var totalVertices=0
    var totalEdges=0

    var finalists=results.asInstanceOf[ArrayBuffer[(Int,Int)]]

    println("*********INSIDE LAM: " + finalists)

    for (verticesAndEdges <- finalists){
      totalVertices+=verticesAndEdges._1
      totalEdges+=verticesAndEdges._2

    }
    val density : Double= (totalEdges.toDouble/(totalVertices.toDouble*(totalVertices.toDouble-1)))
    //println(f"Total vertices: "+ totalVertices + " Total edges: "+ totalEdges + " Density: "+density)
    var text= totalVertices + ","+ totalEdges + ","+density

    writing.writeLines("Density3.csv",text)


  }

  override protected def processOtherMessages(value: Any): Unit = ""
}
