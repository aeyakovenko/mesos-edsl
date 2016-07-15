mesos edsl
==========

A tiny Embedded Domain Specific Language for dealing with Mesos frameworks

SchedulerM
----------

SchedulerM is a monad for handling the scheduler state.  It's a combination of `XorT[(StateT[F, S, ?], String, A]`.
* XorT is used to handle error conditions and fall back to alternatives
* StateT is used for keeping track of the scheduler state

The operations on the state are greedy.  Like a greedy parser, any changes to the state cannot be backtracked. This is a requirement because the operations dealing with mesos cannot be backtracked, so the combinator's should succeed on their entire operation if the consume a single event, or fail the operation.

combinators
-----------
These combinators are used to compose user defined computations

```scala
  /**
   * optional combinator
   * @return the computation in a Some if it succeeds and None if it fails
   */
  def optional[A](s:SchedulerM[A]):SchedulerM[Option[A]] = s.map(Some[A]) orElse pure(None)

  /**
   * many1 combinator
   * @return 1 or more results of the computation
   */
  def many1[A](s:SchedulerM[A]):SchedulerM[List[A]] = for {
    a <- s
    rest <- many1(s) orElse pure(List[A]())
  } yield(a :: rest)

  /**
   * many combinator
   * @return 0 or more results of the compuation 
   */
  def many[A](s:SchedulerM[A]):SchedulerM[List[A]] = many1(s) orElse pure(List[A]())
```

A simple set of combinators is used to build up the higher level Mesos specific operations

Scripting Mesos
---------------

Using scala's `for` expressions is an easy way to chain the scheduler events together.

```scala
import org.apache.mesos.edsl.{monad => E}
import org.apache.mesos.{Protos => P}
//script
def command(s:P.TaskInfo):E.SchedulerM[String] = for {
  t <- E.launch(s)
  _ <- E.isRunning(t)
  r <- E.recvTaskMsg(t)  //gets the output from our command executor
  _ <- E.isFinished(t)
} yield(new String(r))

val programs:E.SchedulerM[String]  = {
  command(newTask(10, 2048, "uname -a")) orElse command(newTask(1, 128, "uname -a"))
}

val script:E.SchedulerM[String] = for {
  _ <- E.logln("starting...")
  _ <- E.registered
  _ <- E.logln("registered!")
  _ <- E.updateOffers
  _ <- E.logln("gotOffers!")
  s <- programs
  _ <- E.logln("ran program!")
  _ <- E.logln(s"program, output: $s")
  _ <- E.shutdown
} yield(s)

//run the script
val s = script.run(D.SchedulerState(driver, channel, List(), None))
println(s)
```

References
-----------
* simple example of a mesos framework <https://github.com/larioj/SleepFramework>
* mesos apis <http://www.apache.org/dist/mesos/0.28.2/mesos-0.28.2.tar.gz>
