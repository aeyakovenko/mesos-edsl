package org.apache.mesos.edsl.data

import org.apache.{mesos => M}
import org.apache.mesos.{Protos => P}
import scala.collection.JavaConverters._ //implicits

sealed abstract class SchedulerEvents
final case class ResourceOffer(offers: java.util.List[P.Offer]) extends SchedulerEvents
final case class OfferRescinded(offer: P.OfferID) extends SchedulerEvents
final case class Disconnected() extends SchedulerEvents
final case class Reregistered(masterInfo: P.MasterInfo) extends SchedulerEvents
final case class SlaveLost(slaveID: P.SlaveID) extends SchedulerEvents
final case class Error(message: String) extends SchedulerEvents
final case class StatusUpdate(status: P.TaskStatus) extends SchedulerEvents
final case class FrameworkMessage(status: P.ExecutorID, slaveId: P.SlaveID, data: Array[Byte]) extends SchedulerEvents
final case class Registered(frameworkId: P.FrameworkID, masterInfo: P.MasterInfo) extends SchedulerEvents
final case class ExecutorLost(executorId: P.ExecutorID, slaveID: P.SlaveID, status: Int) extends SchedulerEvents

abstract class Executor {
  def execute: Unit
}

sealed abstract class Task

final case class Single   (executor:  Executor, rs:List[Resource])    extends Task
final case class Parallel (tasks: List[Task])                         extends Task

sealed abstract class Resource

final case class Cpu(cpu: Double)             extends Resource
final case class Memory(mem: Double)          extends Resource
final case class When(secs: Int)              extends Resource

final case class SchedulerState(driver: M.SchedulerDriver)
