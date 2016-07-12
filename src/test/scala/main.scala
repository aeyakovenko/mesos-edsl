import org.scalacheck._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import scala.util.{Try,Success,Failure}
import org.apache.mesos.edsl.{control => C}
import cats.free.{Trampoline}
import cats.implicits.function0Instance //Comonad[Function0]

object test extends Properties("edsl") {
  type TestM[A] = C.ErrorTStateT[Trampoline, Int, A]
  def bail[A](msg:String):TestM[A] = C.bail(msg)
  def state[A](f: Int => (Int,A)):TestM[A] = C.state(f)
  def get:TestM[Int] = state({ s => (s,s)})
  def put(v:Int):TestM[_] = state({ _ => (v,())})

  def inc: TestM[Int] =
    for {
      v <- get
      _ <- put(v + 1)
    } yield(v)

  property("control.run") = forAll { (a: Int) =>
		inc.toEither.run(1).run == (2,Right(1))
  }
}
