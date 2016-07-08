package org.apache.mesos.edsl.scheduler

import org.apache.mesos.edsl.{data => D}
import org.apache.{mesos => M}
import org.apache.mesos.{Protos => P}
import scala.collection.{JavaConverters => J}
import scala.concurrent.{Channel}

class Scheduler(executor: M.ExecutorInfo, channel: Channel[D.SchedulerEvents]) extends M.Scheduler {
  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]): Unit =
    channel.write(D.ResourceOffers(offers))

  override def offerRescinded(driver: SchedulerDriver, offerId: OfferID): Unit =
    channel.write(D.OfferRescinded(offerId))

  override def disconnected(driver: SchedulerDriver): Unit =
    channel.write(D.Disconnected())

  override def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo): Unit =
    channel.write(D.Reregistered(masterInfo))

  override def slaveLost(driver: SchedulerDriver, slaveId: SlaveID): Unit =
    channel.write(D.SlaveLost(slaveId))

  override def error(driver: SchedulerDriver, message: String): Unit =
    channel.write(D.Error(message))

  override def statusUpdate(driver: SchedulerDriver, status: TaskStatus): Unit =
    channel.write(D.StatusUpdate(status))

  override def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]): Unit =
    channel.write(D.FrameworkMessage(executorId, slaveId, data))

  override def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo): Unit =
    channel.write(D.Registered(frameworkId, masterInfo))

  override def executorLost(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, status: Int): Unit =
    channel.write(D.ExecutorLost(executorId, slaveId, status)

}
