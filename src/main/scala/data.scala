package org.apache.mesos.edsl.data

import org.apache.{mesos => M}
import org.apache.mesos.Protos.{TaskStatus}
import scala.collection.{JavaConverters => J}

sealed abstract case class SchedulerEvents;
final case class ResourceOffer(offers: J.util.List[M.Offer]) extends SchedulerEvents
final case class OfferRescinded(offer: M.OfferID) extends SchedulerEvents
final case class Disconnected extends SchedulerEvents
final case class Reregistered(masterInfo: M.MasterInfo) extends SchedulerEvents
final case class SlaveLost(slaveID: M.SlaveID) extends SchedulerEvents
final case class Error(message: String) extends SchedulerEvents
final case class StatusUpdate(status: M.TaskStatus) extends SchedulerEvents
final case class FrameworkMessage(status: M.ExecutorID, slaveId: M.SlaveID, data: Array[Byte]) extends SchedulerEvents
final case class Registered(frameworkId: M.FrameworkID, masterInfo: M.MasterInfo) extends SchedulerEvents
final case class ExecutorLost(executorId: M.ExecutorID, slaveID: M.SlaveID, status: Int) extends SchedulerEvents
