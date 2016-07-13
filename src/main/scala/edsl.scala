package org.apache.mesos.edsl

import org.apache.mesos.{Protos => P}
import org.apache.{mesos => M}
import org.apache.mesos.edsl.{data => D}
import org.apache.mesos.edsl.{control => C}
import cats.free.{Trampoline}
import cats.implicits.function0Instance //Comonad[Function0]
import scala.collection.JavaConversions._

package object monad {
  type SchedulerM[A] = C.ErrorTStateT[Trampoline, D.SchedulerState, A]
	//todo: any way to auto derive these?
  def bail[A](msg:String):SchedulerM[A] = C.bail(msg)
  def pure[A](a:A):SchedulerM[A] = C.pure(a)
  def state[A](f: D.SchedulerState => (D.SchedulerState,A)):SchedulerM[A] = C.state(f)
  def get:SchedulerM[D.SchedulerState] = state({ s => (s,s)})
  def put(s:D.SchedulerState):SchedulerM[_] = state({ _ => (s,())})

	implicit class SchedulerMRun[A](val v: SchedulerM[A]) extends AnyVal {
		def run(start: D.SchedulerState): Either[String,A] = v.toEither.run(start).run._2
	}

	//todo: generalize this
	implicit class SchedulerMFilter[A](val xort: SchedulerM[A]) extends AnyVal {
		def filter(f: A => Boolean): SchedulerM[A] = xort.flatMap(a => if (f(a)) pure(a) else bail("filter failed"))
	}

  def nextEvent:SchedulerM[D.SchedulerEvents] = for {
    state <- get
    event = state.channel.read
  } yield(event)

  def registered:SchedulerM[_] = for {
    D.Registered(_,_) <- nextEvent
  } yield(())

  def addOffers:SchedulerM[_] = for {
    D.ResourceOffer(offers) <- nextEvent
		state <- get
		_ <- put(state.copy(offers = state.offers ++ offers))
	} yield(())

  def removeOffers:SchedulerM[_] = for {
    D.OfferRescinded(id) <- nextEvent
		state <- get
		_ <- put(state.copy(offers = state.offers.filter({ o => o.getId != id })))
	} yield(())

	implicit class TaskInfoOfferSatisfy(val t: P.TaskInfo) extends AnyVal {
		def satisfy(offers:List[P.Offer]): List[P.Offer] = offers.filter({ o => 
			def satisfyResource(r:P.Resource):Boolean = o.getResourcesList().filter({ x => 
					x match {
						case x if x.getName != r.getName => false
						case x if x.getType != r.getType => false
						case x if x.getType == P.Value.Type.SCALAR && x.getScalar.getValue < r.getScalar.getValue => false
						case _ => true
					}
				}).foldLeft(false)(_ || _)
			t.getResourcesList().map(satisfyResource).foldLeft(true)(_ && _)
		})
	}

//	def launch(t:P.TaskInfo):SchedulerM[_] = for {
//		state <- get
//		offer :: _ <- pure( state.offers.filter({ o => o.getCpu >= t.cpu && o.getMem >= t.mem }) ) 
//		_ <- pure( t.setSlaveId(offer.getSlaveId) ) 
//    _ <- pure( state.driver.launchTasks(List(offer.getId).asJava, List(t).asJava) )
//	} yield(())
//
}

//case class StateData(ch:Channel[D.SchedulerEvents], q:Queue[D.SchedulerEvents], cache:List[D.SchedulerEvents], dr:M.MesosChedulerDriver)
//
//type SchedulerMonad[A] = XortT[State[StateData], String, A]
//
//def pop[D.SchedulerEvent]: SchedulerMonad[D.SchedulerEvent]  = for {
//  State(ch,q,cache,dr) <- get
//  (ev,nc,nq) <- for { 
//                  (ev,nq) <- q.dequeue 
//                } yield(ev,nc,nq) <|> for {
//                  ev <- ch.recieve
//                  nc = ev :: cache
//                } yield(ev,nc,q)
//  put(State(ch,nq,nc,dr))
//} yield(ev)
//
////for launching tasks in parallel, peek the offers, then try each task parallel 
//def peek[D.SchedulerEvent]: SchedulerMonad[D.SchedulerEvent]  = for {
//  State(ch,q,cache,dr) <- get
//  (nq,nc) <- if(q.isEmpty) for(ev <- ch.recieve) yield(q.enqueue(ev), ev::cache)
//             else pure(q,cache)
//  ev = nq.head
//  put(State(ch,nq,cache,dr))
//} yield(ev)
//
//def par[A](a: SchedulerMonad[A], b: SchedulerMonad[A]): (SchedulerMonad[A],SchedulerMonad[A]) = for {
//  State(ch,q,ca,dr) <- get
//  put(State(ch,Queue(ch.reverse),Nil,dr))
//  res1 <- for {
//    rv <- a 
//  } yield(Right) <|> for {
//    State(_,fq,fca,_) <- get
//    put(State(ch,Queue((fca ++ ca).reverse), Nil, dr))
//    rv <- b
//  } yield(Right(rv))
//  rv <- res1 match {
//    Left(r) => for (res2 <- b) yield(r,res2)
//    Right(r) => for(res2 <- a) yield(res2,r)
//  }
//} yield(rv)
