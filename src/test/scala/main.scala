import org.scalacheck._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import scala.util.{Try,Success,Failure}
import org.apache.mesos.edsl.{control => C}
import cats.free.{Trampoline}
import cats.implicits.function0Instance //Comonad[Function0]
import cats._
import cats.implicits._
import cats.{Alternative}

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

	def run[A](script:TestM[A], start: Int): (Int,Either[String, A]) = script.toEither.run(start).run

  property("control.run") = forAll { (a: Int) =>
		run(inc, a) == (a + 1,Right(a))
  }

  def failure: TestM[Int] =
    for {
      v <- inc
			_ <- bail[Int]("foobar")
    } yield(v)

  property("control.bail") = forAll { (a: Int) =>
		run(failure, a) == (a + 1 ,Left("foobar"))
	}

  def choice: TestM[Int] =
    for {
      v <- failure orElse inc
    } yield(v)

  property("control.choice") = forAll { (a: Int) =>
		run(choice, a) == (a + 2 ,Right(a + 1))
	}

}
