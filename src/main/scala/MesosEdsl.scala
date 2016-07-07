package mesosedsl

import cats.{Monad}
//import org.apache.mesos.*;
import org.apache.mesos.Protos.{TaskStatus}

class Scheduler[T <: T] extends Monad[Scheduler[T]] {
  case class Single(task:T, rs: List[Resource[T]]) extends Scheduler[T]
  case class Sequence(ls:List[Scheduler[T]]) extends Scheduler[T]
  case class Parallel(ls:List[Scheduler[T]]) extends Scheduler[T]
  case class Resources(task:Scheduler[T], resources:List[Resource[T]]) extends Scheduler[T]
  def run():Try[Seq[TaskStatus]]
}

class Resource[T :< T] extends Monad[Resource[T]] {
  case class Cpu(cpu: Double) extends Resource[T]
  case class Memory(mem: Double) extends Resource[T]
  case class When(seconds: Int) extends Resource[T]
  case class Many(ls:List[Resource[T]]) extends Resource[T]
  def with(sched:Schedule[T]):Schedule[T]
}

abstract class Task {
  def execute():Unit
}


