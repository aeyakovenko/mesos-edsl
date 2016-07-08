package mesosedsl

import cats.{Monad}
import org.apache.{mesos => M}
import org.apache.mesos.Protos.{TaskStatus}
import scala.util.{Try}

sealed abstract class Scheduler[T]                                           extends Monad[Scheduler] {
  //applicative
  def pure[A](x: A): Scheduler[A] = Single(Some(x), Nil)

  //monad
  def flatMap[A, B](fa: Scheduler[A])(f: A => Scheduler[B]): Scheduler[B] = {
    fa match {
      case Single(Some(x), xrs) => f(x) match {
        case Single(Some(y), yrs) => Single(Some(y), xrs ++ yrs)
        case Single(None,_) => Single(None,Nil)
        case a => a
      }
      case Single(None, rs) => Single(None, rs)
      case Sequence(ts) => Sequence(for(v <- ts) yield(flatMap(v)(f)))
      case Parallel(ts) => Parallel(for(v <- ts) yield(flatMap(v)(f)))
    }
  }

  def schedule(): Try[Seq[TaskStatus]] = {
    class MesosScheduler(executor: M.ExecutorInfo) extends M.Scheduler {
    }
    self match {
      Single
    }
  }
}

final case class Single[T]   (task:  Option[T],          rs:List[Resource])      extends Scheduler[T]
final case class Sequence[T] (tasks: List[Scheduler[T]])                         extends Scheduler[T]
final case class Parallel[T] (tasks: List[Scheduler[T]])                         extends Scheduler[T]


sealed abstract class Resource

final case class Cpu(cpu: Double)             extends Resource
final case class Memory(mem: Double)          extends Resource
final case class When(secs: Int)              extends Resource

abstract class Task {
  def execute(): Unit
}

