mesos combinators
=================

ideas
=====
scheduler {
	List(s1,s2) <- seq {
			a = seq(for {
				status <- cmd("exit 1")
				assert(status == 1)
				List(s1,s2) <- cmd("exit 1") || cmd("exit 2")
				assert(s1 == 1 && s2 == 2)
			})
			b = seq(for {
				s4 <- cmd("exit 4")
				assert(s4 == 4)
		  })
			a || b
		}
} . run

* cmd is a executor for the "system" call to the shell
```scala
	//TODO: unboxed tagged type
	class StatusCode(value: Int) extends AnyVal

	//monad for the execution environment
	class Exec[T] extends Monad[Exec[T]] {
		case class One[T](task: T)
		case class Parallel(tasks: List[Exec[T]])
		case class Seq(tasks: List[Exec[T]])

		//runs the monad, tells mesos to start executing tasks
		def run():Try[T]

		//parallel builder, launch the tasks in parallel 
		def || (a: Exec[T], b: Exec[T]): Exec[List[T]]
		def || (a: Exec[T], ls: Exec[List[T]]): Exec[List[T]]

		//sequential builder
		def seq(builder:Exec[Exec[T]]):Exec[T] = flatMap
	}
	
	//executor for the 'system' call
	def cmd(cmd: String):Exec[StatusCode]
```	

