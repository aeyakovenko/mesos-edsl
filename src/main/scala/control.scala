package org.apache.mesos.edsl.control

import cats.{Applicative,Functor}
import cats.data.{XorT,StateT}

package object Control {
  //type SchedulerStateM[X] = StateT[Trampoline, D.SchedulerState, X]
  //type SchedulerM [A] = XorT[StateX, String, A]
  //type ParserM[S, A] = XorT[StateT[Trampoline, S, ?], String, A]
	//def get[S]: ParserM[S, S] = Xor.right[String,S](State.get[S])

  type ErrorTStateT[F[_], S, A] = XorT[({type l[X] = StateT[F, S, X]})#l, String, A]
	def bail[F[_], S, A](msg: String)(implicit I1: Applicative[F], I2: Functor[({type l[X] = StateT[F, S, X]})#l]): ErrorTStateT[F, S, A] =
    XorT.left[({type l[X] = StateT[F, S, X]})#l, String, A](StateT.pure[F, S, String](msg))

}
