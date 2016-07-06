mesos combinators
=================

ideas
=====

```scala

//example script for launching some commands
for {
	List(s1,s2) <- for {
			a:Scheduler[StatusCode] = for {
				status <- cmd("exit 1")
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
	case class One[T](task: T)

  //many tasks to be launched at once
	case class Parallel(tasks: List[Scheduler[T]])

  //a sequence of tasks
	case class Seq(tasks: List[Scheduler[T]])

	//runs the monad, tells mesos to start executing tasks
  //returns a Try with the result
	def run():Try[T]

	//parallel builder, launch the tasks in parallel 
	def <||> (a: Scheduler[T], b: Scheduler[T]): Scheduler[T]

	//sequential builder
	def flatMap(builder:Scheduler[Scheduler[T]]):Scheduler[T]
}

//todo: is there a newtype/unboxxed tagged type?
class StatysCode(value: Int) extends AnyVal

//one task
class Task {
  def execute():StatusCode
}

//constraints
cpu <&&> memory <|> cpu

class System(cmd:String) extends Task {
  //implements the system call to cmd, sends the result back as a framework message
}

//executor for the 'system' call
def cmd(cmd: String):Scheduler[System] = One(System(cmd))
```	

