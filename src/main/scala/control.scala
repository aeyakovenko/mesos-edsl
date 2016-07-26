package org.apache.mesos.edsl

import cats.{Applicative,Functor}
import cats.data.{XorT,StateT}
import cats.free.Free
import cats.free.Free.liftF

package object control {
  type ErrorTStateT[F[_], S, A] = XorT[({type l[X] = StateT[F, S, X]})#l, String, A]
  def bail[F[_], S, A](msg: String)(implicit I1: Applicative[F], I2: Functor[({type l[X] = StateT[F, S, X]})#l]): ErrorTStateT[F, S, A] =
    XorT.left[({type l[X] = StateT[F, S, X]})#l, String, A](StateT.pure[F, S, String](msg))

  def pure[F[_], S, A](a: A)(implicit I1: Applicative[F], I2: Functor[({type l[X] = StateT[F, S, X]})#l]): ErrorTStateT[F, S, A] =
    XorT.right[({type l[X] = StateT[F, S, X]})#l, String, A](StateT.pure[F, S, A](a))

  def state[F[_], S, A](f: (S) => (S,A))(implicit I1: Applicative[F], I2: Functor[({type l[X] = StateT[F, S, X]})#l]): ErrorTStateT[F, S, A] =
    XorT.right[({type l[X] = StateT[F, S, X]})#l,String,A](StateT.apply[F, S, A]({ s => I1.pure(f(s)) }))

	//todo: figure how to define a generalized filter method
	//implicit class ErrorTStateTFilter[F[_], S, A](val xort: ErrorTStateT[F, S, A]) extends AnyVal {
	//  def filter(f: A => Boolean): ErrorTStateT[F, S, A] = xort.flatMap(a => if (f(a)) pure(a) else bail("filter failed"))
	//}

}


package object world {
  sealed trait KVStoreA[A]
  case class Put[T](key: String, value: T) extends KVStoreA[Unit]
  case class Get[T](key: String) extends KVStoreA[Option[T]]
  case class Delete(key: String) extends KVStoreA[Unit]

  // Put returns nothing (i.e. Unit).
  def put[T](key: String, value: T): world.KVStore[Unit] =
    liftF[KVStoreA, Unit](Put[T](key, value))
  
  // Get returns a T value.
  def get[T](key: String): KVStore[Option[T]] =
    liftF[KVStoreA, Option[T]](Get[T](key))
}
