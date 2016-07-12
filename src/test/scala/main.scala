import org.scalacheck._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import scala.util.{Try,Success,Failure}
import org.apache.mesos.edsl.{control => C}
import cats.free.{Trampoline}
import cats.implicits.function0Instance //Comonad[Function0]
import cats._
import cats.data._

object test extends Properties("edsl") {
  type TestM[A] = C.ErrorTStateT[Trampoline, Int, A]
  def bail[A](msg:String):TestM[A] = C.bail(msg)
  def pure[A](a:A):TestM[A] = C.pure(a)
  def state[A](f: Int => (Int,A)):TestM[A] = C.state(f)
  def get:TestM[Int] = state({ s => (s,s)})
  def put(v:Int):TestM[_] = state({ _ => (v,())})

	implicit class TestMFilter[A](val xort: TestM[A]) extends AnyVal {
		def filter(f: A => Boolean): TestM[A] = xort.flatMap(a => if (f(a)) pure(a) else bail("filter failed"))
	}

  def inc: TestM[Int] =
    for {
      v <- get
      _ <- put(v + 1)
    } yield(v)

	def run[A](script:TestM[A], start: Int): (Int,Either[String, A]) = script.toEither.run(start).run

  property("run") = forAll { (a: Int) =>
		//should be greedy, so state gets incremented
		run(inc, a) == (a + 1,Right(a))
  }

  def failure: TestM[Int] =
    for {
      v <- inc
			_ <- bail[Int]("foobar")
    } yield(v)

  property("bail") = forAll { (a: Int) =>
		//should be greedy during failure, so state gets incremented
		run(failure, a) == (a + 1 ,Left("foobar"))
	}

  def choice: TestM[Int] =
    for {
      v <- failure orElse inc
    } yield(v)

  property("orElse") = forAll { (a: Int) =>
		run(choice, a) == (a + 2 ,Right(a + 1))
	}
  def five: TestM[Boolean] =
		for {
			5 <- get
		} yield(true)

	property("filter") = forAll { (a: Boolean) =>
		run(five, 4) == (4,Left("filter failed"))
	}
	property("filter") = forAll { (a: Boolean) =>
		run(five, 5) == (5,Right(true))
	}


}
