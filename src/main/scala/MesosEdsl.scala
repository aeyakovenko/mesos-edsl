package mesosedsl

import cats.{Monad}
//import org.apache.mesos.*;
import org.apache.mesos.Protos.{TaskStatus};

abstract class Scheduler[T:<Task] extends Monad[Scheduler[T]] {
  case class Single(task:T, rs: List[Resource]) extends Scheduler
  case class Sequence(ls:List[Scheduler]) extends Scheduler
  case class Parallel(ls:List[Scheduler]) extends Scheduler
  case class Resources(task:Scheduler, resources:List[Resources]) extends Scheduler
  def run():Try[Seq[TaskStatus]]
}
abstract class Resource[T] extends Monad[Resource[T]] {
  case class Cpu(cpu: Double) extends Resource
  case class Memory(mem: Double) extends Resource
  case class When(seconds: Int) extends Resource
  case class Many(ls:List[ResourceT]) extends Resource
  def with(sched:Schedule[T]):Schedule[T]
}
