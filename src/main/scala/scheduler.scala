package org.apache.mesos.edsl.scheduler

import org.apache.mesos.edsl.{data => D}
import org.apache.{mesos => M}
import org.apache.mesos.{Protos => P}
import scala.concurrent.{Channel}

/**
 * Scheduler implemenation that passes all the events to the channel
 * @param channel the channel processed by the SchedulerM monad
 */
class Scheduler(channel: Channel[D.SchedulerEvents]) extends M.Scheduler {
  //todo: use a real log api instead of println(s"scheduler event: $msg")
  def logln(msg: String):Unit = ()
  override def resourceOffers(driver: M.SchedulerDriver, offers: java.util.List[P.Offer]): Unit = {
    logln("resource offer")
    channel.write(D.ResourceOffer(offers))
  }

  override def offerRescinded(driver: M.SchedulerDriver, offerId: P.OfferID): Unit = {
    logln("offerRescinded")
    channel.write(D.OfferRescinded(offerId))
  }

  override def disconnected(driver: M.SchedulerDriver): Unit = {
    logln("disconnected")
    channel.write(D.Disconnected())
  }

  override def reregistered(driver: M.SchedulerDriver, masterInfo: P.MasterInfo): Unit = {
    logln("reregistered")
    channel.write(D.Reregistered(masterInfo))
  }

  override def slaveLost(driver: M.SchedulerDriver, slaveId: P.SlaveID): Unit = {
    logln("slaveLost")
    channel.write(D.SlaveLost(slaveId))
  }

  override def error(driver: M.SchedulerDriver, message: String): Unit = {
    logln(s"error $message")
    channel.write(D.Error(message))
  }

  override def statusUpdate(driver: M.SchedulerDriver, status: P.TaskStatus): Unit = {
    logln(s"statusUpdate")
    channel.write(D.StatusUpdate(status))
  }

  override def frameworkMessage(driver: M.SchedulerDriver, executorId: P.ExecutorID, slaveId: P.SlaveID, data: Array[Byte]): Unit = {
    logln("frameworkMessage")
    channel.write(D.FrameworkMessage(executorId, slaveId, data))
  }

  override def registered(driver: M.SchedulerDriver, frameworkId: P.FrameworkID, masterInfo: P.MasterInfo): Unit = {
    logln("registered")
    channel.write(D.Registered(frameworkId, masterInfo))
  }

  override def executorLost(driver: M.SchedulerDriver, executorId: P.ExecutorID, slaveId: P.SlaveID, status: Int): Unit = {
    logln(s"executorLost")
    channel.write(D.ExecutorLost(executorId, slaveId, status))
  }

}
