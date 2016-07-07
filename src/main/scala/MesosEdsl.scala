package mesosedsl

import cats.{Monad}
//import org.apache.mesos.*;
import org.apache.mesos.Protos.{TaskStatus}
import scala.util.{Try}

class Scheduler[T]                                                extends Monad[Scheduler] {
  case class Single(task:T)                                       extends Scheduler[T]
  case class Sequence(tls:List[Scheduler[T]])                     extends Scheduler[T]
  case class Parallel(tls:List[Scheduler[T]])                     extends Scheduler[T]
  case class Resources(task:Scheduler[T], rls:List[Resource[T]])  extends Scheduler[T]

  def schedule(): Try[Seq[TaskStatus]] = error("unimplemented")
  def pure[A](x: A): Scheduler[A] = error("unimplemented")
  def flatMap[A, B](fa: Scheduler[A])(f: A => Scheduler[B]): Scheduler[B] = error("unimplemented")
}

class Resource[T]                         extends Monad[Resource] {
  case class Cpu(cpu: Double)             extends Resource[T]
  case class Memory(mem: Double)          extends Resource[T]
  case class When(secs: Int)              extends Resource[T]
  case class Many(rls:List[Resource[T]])  extends Resource[T]

  def withResource(sched: Scheduler[T]): Scheduler[T] = error("unimplemented")

  def pure[A](x: A): Resource[A] = error("unimplemented")
  def flatMap[A, B](fa: Resource[A])(f: A => Resource[B]): Resource[B] = error("unimplemented")

}

class Task {
  def execute(): Unit = error("unimplemented")
}

//object Scheduler {
//	//parallel builder, launch the tasks in parallel 
//	def <||> (a: Scheduler[T], b: Scheduler[T]): Scheduler[T]
//  def task(task:T): Scheduler[T]
//
//	//sequential builder
//	def flatMap(builder:Scheduler[Scheduler[T]]):Scheduler[T]
//
//}


