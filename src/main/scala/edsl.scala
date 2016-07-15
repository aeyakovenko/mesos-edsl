package org.apache.mesos.edsl

import org.apache.mesos.{Protos => P}
import org.apache.{mesos => M}
import org.apache.mesos.edsl.{data => D}
import org.apache.mesos.edsl.{control => C}
import cats.free.{Trampoline}
import cats.implicits.function0Instance //Comonad[Function0]
import scala.collection.JavaConversions._
import cats._
import cats.implicits._

package object monad {
  type SchedulerM[A] = C.ErrorTStateT[Trampoline, D.SchedulerState, A]
	//todo: any way to auto derive these?
  def bail[A](msg:String):SchedulerM[A] = C.bail(msg)
  def pure[A](a:A):SchedulerM[A] = C.pure(a)
  def state[A](f: D.SchedulerState => (D.SchedulerState,A)):SchedulerM[A] = C.state(f)
  def get:SchedulerM[D.SchedulerState] = state({ s => (s,s)})
  def put(s:D.SchedulerState):SchedulerM[Unit] = state({ _ => (s,())})

	implicit class SchedulerMRun[A](val v: SchedulerM[A]) extends AnyVal {
		def run(start: D.SchedulerState): Either[String,A] = v.toEither.run(start).run._2
	}

	//todo: generalize this
	implicit class SchedulerMFilter[A](val xort: SchedulerM[A]) extends AnyVal {
		def filter(f: A => Boolean): SchedulerM[A] = xort.flatMap(a => if (f(a)) pure(a) else bail(s"SchedulerM[A] filter failed at $a"))
	}
  //combinators
  def retry[A](n:Int, s:SchedulerM[A]):SchedulerM[A] = {
    if(n < 0) {
      bail("retry failed")
    } else {
      s orElse retry(n - 1, s)
    }
  }
  def optional[A](s:SchedulerM[A]):SchedulerM[Option[A]] = s.map(Some[A]) orElse pure(None)

  def many1[A](s:SchedulerM[A]):SchedulerM[List[A]] = for {
    a <- s
    rest <- many1(s) orElse pure(List[A]())
  } yield(a :: rest)

  def many[A](s:SchedulerM[A]):SchedulerM[List[A]] = many1(s) orElse pure(List[A]())

  def readEvent:SchedulerM[D.SchedulerEvents] = for {
    state <- get
    event <- pure ( state.channel.read )
		_ <- put(state.copy(lookahead = Some(event)) )
  } yield(event)

  def peekEvent:SchedulerM[D.SchedulerEvents] = for {
    state <- get
    Some(event) <- pure(state.lookahead)
  } yield(event)

  def nextEvent:SchedulerM[D.SchedulerEvents] = peekEvent orElse readEvent

  def nextTaskEvent:SchedulerM[D.SchedulerEvents] = for { 
      _ <- many(updateOffers)
      e <- nextEvent
  } yield(e)

  def consumeEvent:SchedulerM[Unit] = for {
    state <- get
		_ <- put(state.copy(lookahead = None))
  } yield(())

  def registered:SchedulerM[Unit] = for {
    D.Registered(_,_) <- nextEvent
    _ <- consumeEvent
  } yield(())

  def disconnected:SchedulerM[Unit] = for {
    D.Disconnected() <- nextEvent
    _ <- consumeEvent
  } yield(())

  def offerAdded:SchedulerM[Unit] = for {
    D.ResourceOffer(offers) <- nextEvent
    _ <- consumeEvent
		state <- get
		_ <- put(state.copy(offers = state.offers ++ offers))
	} yield(())

  def offerRescinded:SchedulerM[Unit] = for {
    D.OfferRescinded(id) <- nextEvent
    _ <- consumeEvent
    _ <- removeOffer(id)
	} yield(())

  def removeOffer(id:P.OfferID):SchedulerM[Unit] = for {
		state <- get
		_ <- put(state.copy(offers = state.offers.filter({ o => o.getId != id })))
  } yield(())

  def consumeAndBail[A]:SchedulerM[A] = for {
    _ <- consumeEvent 
    a <- bail[A]("consumeAndBail")
  } yield(a)

  def updateOffers:SchedulerM[Unit] = offerAdded orElse offerRescinded

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
	def launch(t:P.TaskInfo):SchedulerM[P.TaskInfo] = for {
		state <- get
		offer :: _ <- pure( t.satisfy(state.offers) )
		task = t.toBuilder.setSlaveId(offer.getSlaveId).build()
    _ <- pure( state.driver.launchTasks(List(offer.getId), List(task)) )
    _ <- removeOffer(offer.getId)
    _ <- pure( println("launched!") )
	} yield(task)

	def taskStatus(t: P.TaskInfo):SchedulerM[P.TaskStatus] = for {
		D.StatusUpdate(status) <- nextTaskEvent
		if status.getTaskId == t.getTaskId
    _ <- consumeEvent
    _ <- pure( println("got taskStatus!") )
	} yield(status)

	def stop():SchedulerM[_] = for {
		state <- get
		_ <- pure ( state.driver.stop() )
	} yield(())

	def shutdown():SchedulerM[_] = for {
    _ <- stop()
    _ <- disconnected
  } yield(())

  def recvTaskMsg(t:P.TaskInfo):SchedulerM[Array[Byte]] = for {
    D.FrameworkMessage(eid, sid, data) <- nextTaskEvent
    if(eid == t.getExecutor.getExecutorId && sid == t.getSlaveId)
    _ <- consumeEvent
  } yield(data)

  def sendTaskMsg(t:P.TaskInfo, data:Array[Byte]):SchedulerM[_] = for {
    state <- get
    _ <- pure( state.driver.sendFrameworkMessage(t.getExecutor.getExecutorId, t.getSlaveId, data) )
  } yield(())

  def isRunning(t:P.TaskInfo):SchedulerM[_] = for {
    s <- taskStatus(t)
    if s.getState == P.TaskState.TASK_RUNNING 
    _ <- pure( println("task isRunning! ") )
  } yield(())


}
