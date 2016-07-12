mesos combinators
=================

wrap the scheduler/executor classes into combinators that can be composed 

ideas
=====

scheduling tasks
----------------

```scala

//example script for launching some commands
for {
  List(s1,s2) <- for {
      a:Scheduler[StatusCode] = for {
        status <- task(cmd("exit 1"))
        assert(status == 1)
        List(s1,s2) <- cmd("exit 1") || cmd("exit 2")
        assert(s1 == 1 && s2 == 2)
      }
      b:Scheduler[StatusCode] = for {
        s4 <- cmd("exit 4")
        assert(s4 == 4)
      }
      a <||> b
    }
  List(s1,s2)
} . run

//interface

//monad for the scheduler/driver
class Scheduler[T:<Task] extends Monad[Scheduler[T]] {
  //one task
  case class Task[T](task: T, rs: List[Resource] = Nil)

  //many tasks to be launched at once
  case class Parallel(tasks: List[Scheduler[T]])

  //a sequence of tasks
  case class Sequence(tasks: List[Scheduler[T]])

  //runs the monad, tells mesos to start executing tasks
  //returns a Try with the result
  def run():Try[T]
}

object Scheduler {
  //parallel builder, launch the tasks in parallel 
  def <||> (a: Scheduler[T], b: Scheduler[T]): Scheduler[T]
  def task(task:T): Scheduler[T]

  //sequential builder
  def flatMap(builder:Scheduler[Scheduler[T]]):Scheduler[T]

  def cpu(cores:Int):Scheduler[Resource[T]]
  def memory(bytes:Int):Scheduler[Resource[T]]
  def when(seconds:Int):Scheduler[Resource[T]]
}

//todo: is there a newtype/unboxxed tagged type?
class StatusCode(value: Int) extends AnyVal

//one task
class Task[R] {
  def execute():R
}

class Command(cmd:String) extends Task {
  //implements the system call to cmd, sends the result back as a framework message
}
object Command {
  def cmd(cmd: String):Scheduler[System] = Command(cmd)
}
```

free monad
----------

* https://github.com/typelevel/cats/blob/d14fc0aad45acbf86cf1b87824d0c55390b91e53/docs/src/main/tut/freemonad.md

filtering for resource constraints
----------------------------------
```scala
val a:Scheduler[Command] = for {
  (cpu(2) <&&> memory(100) <&&> when(10)) <|> (cpu(1) <&&> memory(50)) withResource cmd("exit 5")
}
```

* cpu memory and when combinators allow the user to wait for a specific resource until its available then execute it

kind project plugin
--------------------
removes the type lambdas and replaces them with a '?'
* addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0")

trans lift test
----------------

* addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0")

sources
-------
* git clone https://github.com/larioj/SleepFramework
* wget http://www.apache.org/dist/mesos/0.28.2/mesos-0.28.2.tar.gz
