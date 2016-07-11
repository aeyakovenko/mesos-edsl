package org.apache.mesos.edsl.scheduler

import org.apache.mesos.edsl.{data => D}
import org.apache.{mesos => M}
import org.apache.mesos.{Protos => P}
//import scala.collection.{JavaConverters => J}
import scala.concurrent.{Channel}

class Scheduler(executor: P.ExecutorInfo, channel: Channel[D.SchedulerEvents]) extends M.Scheduler {
  override def resourceOffers(driver: M.SchedulerDriver, offers: java.util.List[P.Offer]): Unit =
    channel.write(D.ResourceOffer(offers))

  override def offerRescinded(driver: M.SchedulerDriver, offerId: P.OfferID): Unit =
    channel.write(D.OfferRescinded(offerId))

  override def disconnected(driver: M.SchedulerDriver): Unit =
    channel.write(D.Disconnected())

  override def reregistered(driver: M.SchedulerDriver, masterInfo: P.MasterInfo): Unit =
    channel.write(D.Reregistered(masterInfo))

  override def slaveLost(driver: M.SchedulerDriver, slaveId: P.SlaveID): Unit =
    channel.write(D.SlaveLost(slaveId))

  override def error(driver: M.SchedulerDriver, message: String): Unit =
    channel.write(D.Error(message))

  override def statusUpdate(driver: M.SchedulerDriver, status: P.TaskStatus): Unit =
    channel.write(D.StatusUpdate(status))

  override def frameworkMessage(driver: M.SchedulerDriver, executorId: P.ExecutorID, slaveId: P.SlaveID, data: Array[Byte]): Unit =
    channel.write(D.FrameworkMessage(executorId, slaveId, data))

  override def registered(driver: M.SchedulerDriver, frameworkId: P.FrameworkID, masterInfo: P.MasterInfo): Unit =
    channel.write(D.Registered(frameworkId, masterInfo))

  override def executorLost(driver: M.SchedulerDriver, executorId: P.ExecutorID, slaveId: P.SlaveID, status: Int): Unit =
    channel.write(D.ExecutorLost(executorId, slaveId, status))

}
