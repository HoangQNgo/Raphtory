package com.raphtory.core.components.Spout

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Timers
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import com.raphtory.core.components.Spout.SpoutTrait.CommonMessage.IsSafe
import com.raphtory.core.components.Spout.SpoutTrait.CommonMessage.StartSpout
import com.raphtory.core.components.Spout.SpoutTrait.CommonMessage.StateCheck
import com.raphtory.core.components.Spout.SpoutTrait.DomainMessage
import com.raphtory.core.model.communication._
import kamon.Kamon

import scala.concurrent.ExecutionContext
//import kamon.metric.CounterMetric
//import kamon.metric.GaugeMetric

import scala.concurrent.duration._
import scala.language.postfixOps

// TODO Add val name which sub classes that extend this trait must overwrite
//  e.g. BlockChainSpout val name = "Blockchain Spout"
//  Log.debug that read 'Spout' should then read 'Blockchain Spout'
trait SpoutTrait[Domain <: DomainMessage, Out <: SpoutGoing] extends Actor with ActorLogging with Timers {
  // todo: wvv should assign the dispatcher when create the actor
  // implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("spout-dispatcher")
  implicit val executionContext: ExecutionContext = context.system.dispatcher

  private val spoutTuples = Kamon.counter("Raphtory_Spout_Tuples").withTag("actor", self.path.name)
  private var count       = 0
  private def recordUpdate(): Unit = {
    spoutTuples.increment()
    count += 1
  }

  private val mediator = DistributedPubSub(context.system).mediator

  override def preStart() {
    log.debug("Spout is being started.")
    mediator ! DistributedPubSubMediator.Put(self)
    context.system.scheduler.scheduleOnce(7 seconds, self, StateCheck)
    context.system.scheduler.scheduleOnce(1 seconds, self, IsSafe)
  }

  final override def receive: Receive = work(false)

  private def work(safe: Boolean): Receive = {
    case StateCheck => processStateCheckMessage(safe)
    case ClusterStatusResponse(clusterUp) =>
      context.become(work(clusterUp))
      context.system.scheduler.scheduleOnce(1 second, self, StateCheck)
    case IsSafe    => processIsSafeMessage(safe)
    case StartSpout => startSpout()
    case x: Domain  => handleDomainMessage(x)
    case unhandled => log.error(s"Unable to handle message [$unhandled].")
  }

  def startSpout(): Unit

  def handleDomainMessage(message: Domain): Unit

  private def processStateCheckMessage(safe: Boolean): Unit = {
    log.debug(s"Spout is handling [StateCheck] message.")
    if (!safe) {
      val sendMessage = ClusterStatusRequest()
      val sendPath    = "/user/WatchDog"
      log.debug(s"Sending DPSM message [$sendMessage] to path [$sendPath].")
      mediator ! DistributedPubSubMediator.Send(sendPath, sendMessage, localAffinity = false)
    }
  }

  private def processIsSafeMessage(safe: Boolean): Unit = {
    log.debug(s"Spout is handling [IsSafe] message.")
    if (safe)
      self ! StartSpout
    else
      context.system.scheduler.scheduleOnce(delay = 1 second, receiver = self, message = IsSafe)
  }

  protected def sendTuple(command: Out): Unit = {
    log.debug(s"The command [$command] received for send.")
    recordUpdate()
    val message =
      if (count % 100 == 0)
        AllocateTrackedTuple(System.currentTimeMillis(), command)
      else
        AllocateTuple(command)
    mediator ! DistributedPubSubMediator.Send(s"/user/router/routerWorker_${count % 10}", message, localAffinity = false)
  }
}

object SpoutTrait {
  object CommonMessage {
    case object StartSpout
    case object StateCheck
    case object IsSafe
  }
  trait DomainMessage
}
