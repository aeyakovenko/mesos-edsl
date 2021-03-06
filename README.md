mesos edsl
==========

A tiny bare bones Embedded Domain Specific Language for dealing with the Mesos framework.

SchedulerM
----------

SchedulerM is a monad for handling the scheduler state.  It's a combination of `XorT[(StateT[F, S, ?], String, A]`.
* XorT is used to handle error conditions and fall back to alternatives
* StateT is used for keeping track of the scheduler state

The operations on the state are greedy.  Like a greedy parser, any changes to the state cannot be backtracked. This is a requirement because the operations dealing with mesos cannot be backtracked, so the combinators should succeed on their entire operation if the consume a single event, or fail the operation.

Internally the implementation works very similarly to a recursive decent parser that uses a blocking channel to read events.  The State keeps track of the last read event until the parser indicates that it's consumed.  So it behaves similarly to a `LR(1)` parser.

Combinators
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

A simple set of combinators is used to build up the higher level Mesos specific operations.

Scripting Mesos
---------------

Using scala's `for` expressions is an easy way to chain the scheduler events together or handle and recover from errors.

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
  command(newTask(cpu = 10, mem = 2048, "uname -a")) orElse command(newTask(cpu = 1, mem = 128, "uname -a"))
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
Adding more awesomeness
-----------------------

The awesome part of using Monad Transformers is that you can always add more Monads to the stack for some realy advanced features.

* Use the Free Monad to remove the blocking channel read call, and instead feed each event to an object and check if its done.  This would enable the user to create "background" scripts that can consume events until they succeed
* Another Free Monad could also be used to add live debugging capabilities to the scheduler.

Running
--------
* Install the vagrant environment [here](https://github.com/dcos/dcos-vagrant)
  * Use the 1.7 dcos configuration
  * Deploy with ` vagrant up m1 a1 p1 boot`
  * The `dcos-vagrant` cloned directory will be mounted on all the machines at `/vagrant`
* Clone this repository
* Create a jar of the project with `sbt assembly`
  * The jar will be created in `target/scala-2.11/mesos-edsl-assembly-1.0.jar`
* Move the jar to the `dcos-vagrant` directory
* ssh into m1 with `vagrant ssh m1`
* Run the scheduler `java -cp mesos-edsl-assembly-1.0.jar org.apache.mesos.edsl.SchedulerMTest`
* expecting output `Right(Linux a1.dcos 3.10.0-327.18.2.el7.x86_64 #1 SMP Thu May 12 11:03:55 UTC 2016 x86_64 x86_64 x86_64 GNU/Linux)`

References
-----------
* simple example of a mesos framework <https://github.com/larioj/SleepFramework>
* mesos apis <http://www.apache.org/dist/mesos/0.28.2/mesos-0.28.2.tar.gz>
