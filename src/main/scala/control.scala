package org.apache.mesos.edsl

import cats.{Applicative,Functor}
import cats.data.{XorT,StateT}

package object control {
  type ErrorTStateT[F[_], S, A] = XorT[({type l[X] = StateT[F, S, X]})#l, String, A]
  def bail[F[_], S, A](msg: String)(implicit I1: Applicative[F], I2: Functor[({type l[X] = StateT[F, S, X]})#l]): ErrorTStateT[F, S, A] =
    XorT.left[({type l[X] = StateT[F, S, X]})#l, String, A](StateT.pure[F, S, String](msg))

  def state[F[_], S, A](f: (S) => (S,A))(implicit I1: Applicative[F], I2: Functor[({type l[X] = StateT[F, S, X]})#l]): ErrorTStateT[F, S, A] =
    XorT.right[({type l[X] = StateT[F, S, X]})#l,String,A](StateT.apply[F, S, A]({ s => I1.pure(f(s)) }))

}

