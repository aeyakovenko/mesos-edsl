package org.apache.mesos.edsl

import org.apache.mesos.{Protos => P}
import org.apache.{mesos => M}
import org.apache.mesos.edsl.{data => D}
import org.apache.mesos.edsl.{control => C}
import cats.free.{Trampoline}
import cats.implicits.function0Instance //Comonad[Function0]
import scala.collection.JavaConversions._

package object monad {
  /**
   * type alias for the monad stack that represents the stateful scheduler
   */
  type SchedulerM[A] = C.ErrorTStateT[Trampoline, D.SchedulerState, A]
  /**
   * error with a message
   * @param msg the error message
   */
  def bail[A](msg:String):SchedulerM[A] = C.bail(msg)
  /**
   * lift the value into the SchedulerM monad
   */
  def pure[A](a:A):SchedulerM[A] = C.pure(a)
  /**
   * transform the state
   */
  def state[A](f: D.SchedulerState => (D.SchedulerState,A)):SchedulerM[A] = C.state(f)
  /**
   * get the sate
   */
  def get:SchedulerM[D.SchedulerState] = state({ s => (s,s)})
  /**
   * save the state
   */
  def put(s:D.SchedulerState):SchedulerM[Unit] = state({ _ => (s,())})

  //todo: replace this with WriterT/LogT
  /**
   * log the line
   */
  def logln(msg: String):SchedulerM[Unit] = pure(())
  //_ <- pure( println("launched!") )

  implicit class SchedulerMRun[A](val v: SchedulerM[A]) extends AnyVal {
    /**
     * run method
     */
    //todo: Comonid shoudl be the correct typeclass to upack the value out of the monad
    def run(start: D.SchedulerState): Either[String,A] = v.toEither.run(start).run._2
  }

  //todo: generalize this
  implicit class SchedulerMFilter[A](val xort: SchedulerM[A]) extends AnyVal {
    /**
     * filter method for the monad container, this is used by the `for` expression sugar
     */
    def filter(f: A => Boolean): SchedulerM[A] = xort.flatMap(a => if (f(a)) pure(a) else bail(s"SchedulerM[A] filter failed at $a"))
  }
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
   * many1 combinator
   * @return 0 or more results of the compuation 
   */
  def many[A](s:SchedulerM[A]):SchedulerM[List[A]] = many1(s) orElse pure(List[A]())

  /**
   * read the event and put it in the lookahead state
   */
  def readEvent:SchedulerM[D.SchedulerEvents] = for {
    state <- get
    event <- pure ( state.channel.read )
    _ <- put(state.copy(lookahead = Some(event)) )
  } yield(event)

  /**
   * @return the lookahead event and fail if it doesn't exist
   */
  def peekEvent:SchedulerM[D.SchedulerEvents] = for {
    state <- get
    Some(event) <- pure(state.lookahead)
  } yield(event)

  /**
   * @return the lookahead event or the event from the channel
   */
  def nextEvent:SchedulerM[D.SchedulerEvents] = peekEvent orElse readEvent

  /**
   * process the background events in the background until we read a task event
   * @return the next event
   */
  def nextTaskEvent:SchedulerM[D.SchedulerEvents] = for { 
    _ <- many(updateOffers)
    e <- nextEvent
  } yield(e)

  /**
   * mark the current lookahead event as "done" by removing it from the state
   */
  def consumeEvent:SchedulerM[Unit] = for {
    state <- get
    _ <- put(state.copy(lookahead = None))
  } yield(())

  /**
   * consumes the Registered event from the scheduler
   */
  def registered:SchedulerM[Unit] = for {
    D.Registered(_,_) <- nextEvent
    _ <- consumeEvent
  } yield(())

  /**
   * consumes the Disconnected event from the scheduler
   */
  def disconnected:SchedulerM[Unit] = for {
    D.Disconnected() <- nextEvent
    _ <- consumeEvent
  } yield(())

  /**
   * consumes the ResourceOffer event and updates the current state of offer list
   */
  def offerAdded:SchedulerM[Unit] = for {
    D.ResourceOffer(offers) <- nextEvent
    _ <- consumeEvent
    state <- get
    _ <- put(state.copy(offers = state.offers ++ offers))
  } yield(())

  /**
   * consumes the OfferRescinded event and updates the current state of offer list
   */
  def offerRescinded:SchedulerM[Unit] = for {
    D.OfferRescinded(id) <- nextEvent
    _ <- consumeEvent
    _ <- removeOffer(id)
  } yield(())

  /**
   * removes the offer
   * @param id the offer id
   */
  def removeOffer(id:P.OfferID):SchedulerM[Unit] = for {
    state <- get
    _ <- put(state.copy(offers = state.offers.filter({ o => o.getId != id })))
  } yield(())

  /**
   * process the offerAdded or offerRescinded events
   */
  def updateOffers:SchedulerM[Unit] = offerAdded orElse offerRescinded

  /**
   * check the offer list if it satisfies the task
   * @param t the TaskInfo structure to check
   * @param offers the offer list to check against
   */
  implicit class TaskInfoOfferSatisfy(val t: P.TaskInfo) extends AnyVal {
    def satisfy(offers:List[P.Offer]): List[P.Offer] = offers.filter({ o =>
      def satisfyResource(r:P.Resource):Boolean = { o.getResourcesList().map({ x =>
        x match {
          case x if x.getName != r.getName => false
          case x if x.getType != r.getType => false
          case x if x.getType == P.Value.Type.SCALAR && x.getScalar.getValue < r.getScalar.getValue => false
          case _ => true
        }
      }).foldLeft(false)(_ || _)
      }
      t.getResourcesList().map(satisfyResource).foldLeft(true)(_ && _)
    })
  }

  /**
   * launch the task
   * @param t the task to launch
   * @returns the task with the updated SlaveID where it was launched
   */
  def launch(t:P.TaskInfo):SchedulerM[P.TaskInfo] = for {
    state <- get
    //lifts the 'head on empty list' error into the scheduler monad
    offer :: _ <- pure( t.satisfy(state.offers) )
    task = t.toBuilder.setSlaveId(offer.getSlaveId).build()
    _ <- pure( state.driver.launchTasks(List(offer.getId), List(task)) )
    _ <- removeOffer(offer.getId)
    _ <- logln("launched!")
  } yield(task)

  /**
   * stop the driver
   */
  def stop():SchedulerM[_] = for {
    state <- get
    _ <- pure ( state.driver.stop() )
  } yield(())

  /**
   * shutdown the driver and disconnect
   */
  def shutdown():SchedulerM[_] = for {
    _ <- stop()
    //_ <- disconnected todo: shoudl we see this event on stop?
  } yield(())

  /**
   * consume the frame work message from the task
   * @param t the task expecting the framework message
   * @return the data byte array from the message
   */
  def recvTaskMsg(t:P.TaskInfo):SchedulerM[Array[Byte]] = for {
    D.FrameworkMessage(eid, sid, data) <- nextTaskEvent
    if(eid == t.getExecutor.getExecutorId && sid == t.getSlaveId)
      _ <- consumeEvent
  } yield(data)

  /**
   * send a task message to the task
   * @param t the task to send the message to
   * @param data the message data
   */
  def sendTaskMsg(t:P.TaskInfo, data:Array[Byte]):SchedulerM[_] = for {
    state <- get
    _ <- pure( state.driver.sendFrameworkMessage(t.getExecutor.getExecutorId, t.getSlaveId, data) )
  } yield(())

  /**
   * consume the TASK_RUNNING status update
   */
  def isRunning(t:P.TaskInfo):SchedulerM[_] = for {
    D.StatusUpdate(s) <- nextTaskEvent
    if s.getTaskId == t.getTaskId
    if s.getState == P.TaskState.TASK_RUNNING 
    _ <- consumeEvent
    _ <- logln("task isRunning!")
  } yield(())

  /**
   * consume the TASK_FINISHED status update
   */
  def isFinished(t:P.TaskInfo):SchedulerM[_] = for {
    D.StatusUpdate(s) <- nextTaskEvent
    if s.getTaskId == t.getTaskId
    if s.getState == P.TaskState.TASK_FINISHED 
    _ <- consumeEvent
    _ <- logln("task isFinished!")
  } yield(())

}
