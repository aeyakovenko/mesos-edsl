package org.apache.mesos.edsl

import org.apache.mesos.edsl.{data => D}
import cats.{Monad,<|>,State}
import cats.data.XorT
import org.apache.{mesos => M}

import org.apache.mesos.Protos.{TaskStatus}
import scala.util.{Try}
import scala.concurrent.{Channel}
import import cats.state.{State, get, put}


object ParserM {
  type ParserM[S, A] = XorT[({type l[X] = StateT[Trampoline, S, X]})#l, String, A]
  //type ParserM[S, A] = XorT[StateT[Trampoline, S, ?], String, A]
	//def get[S]: ParserM[S, S] = Xor.right[String,S](State.get[S])
	def bail[S,A](msg: String): ParserM[S, A] = XorT.left[({type l[X] = StateT[Trampoline, S, X]})#l, String, A](msg)
}
case class StateData(ch:Channel[D.SchedulerEvents], q:Queue[D.SchedulerEvents], cache:List[D.SchedulerEvents], dr:M.MesosChedulerDriver)

type SchedulerMonad[A] = XortT[State[StateData], String, A]

def pop[D.SchedulerEvent]: SchedulerMonad[D.SchedulerEvent]  = for {
  State(ch,q,cache,dr) <- get
  (ev,nc,nq) <- for { 
                  (ev,nq) <- q.dequeue 
                } yield(ev,nc,nq) <|> for {
                  ev <- ch.recieve
                  nc = ev :: cache
                } yield(ev,nc,q)
  put(State(ch,nq,nc,dr))
} yield(ev)

//for launching tasks in parallel, peek the offers, then try each task parallel 
def peek[D.SchedulerEvent]: SchedulerMonad[D.SchedulerEvent]  = for {
  State(ch,q,cache,dr) <- get
  (nq,nc) <- if(q.isEmpty) for(ev <- ch.recieve) yield(q.enqueue(ev), ev::cache)
             else pure(q,cache)
  ev = nq.head
  put(State(ch,nq,cache,dr))
} yield(ev)

def par[A](a: SchedulerMonad[A], b: SchedulerMonad[A]): (SchedulerMonad[A],SchedulerMonad[A]) = for {
  State(ch,q,ca,dr) <- get
  put(State(ch,Queue(ch.reverse),Nil,dr))
  res1 <- for {
    rv <- a 
  } yield(Right) <|> for {
    State(_,fq,fca,_) <- get
    put(State(ch,Queue((fca ++ ca).reverse), Nil, dr))
    rv <- b
  } yield(Right(rv))
  rv <- res1 match {
    Left(r) => for (res2 <- b) yield(r,res2)
    Right(r) => for(res2 <- a) yield(res2,r)
  }
} yield(rv)
