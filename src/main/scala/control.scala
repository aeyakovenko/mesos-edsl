package org.apache.mesos.edsl.control

import cats.{Applicative,Functor}
import cats.data.{XorT,StateT}

package object Control {
  // trans lift tests, https://github.com/typelevel/cats/blob/master/tests/src/test/scala/cats/tests/TransLiftTests.scala
  // addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0")
  // removes the type lambdas and adds ?

//  type StateX[X] = StateT[Trampoline, Int, X]
//  type XorStateX[A] = XorT[StateX, String, A]
//	def bail[A](msg:String): XorStateX[A] = XorT.left[StateX,String,A](StateT.pure(msg))
//
//	def state[A](f: (Int) => (Int,A)): XorStateX[A] =
//		XorT.right[StateX,String,A](StateT.apply[Trampoline, Int, A]({ s => Trampoline.done(f(s)) }))
//
  type ErrorTStateT[F[_], S, A] = XorT[({type l[X] = StateT[F, S, X]})#l, String, A]
	def bail[F[_], S, A](msg: String)(implicit I1: Applicative[F], I2: Functor[({type l[X] = StateT[F, S, X]})#l]): ErrorTStateT[F, S, A] =
    XorT.left[({type l[X] = StateT[F, S, X]})#l, String, A](StateT.pure[F, S, String](msg))

	def right[F[_], S, A](value: A)(implicit I1: Applicative[F], I2: Functor[({type l[X] = StateT[F, S, X]})#l]): ErrorTStateT[F, S, A] =
    XorT.right[({type l[X] = StateT[F, S, X]})#l, String, A](StateT.pure[F, S, A](value))

	def state[F[_], S, A](f: (S) => (S,A))(implicit I1: Applicative[F], I2: Functor[({type l[X] = StateT[F, S, X]})#l]): ErrorTStateT[F, S, A] =
		XorT.right[({type l[X] = StateT[F, S, X]})#l,String,A](StateT.apply[F, S, A]({ s => I1.pure(f(s)) }))

}
