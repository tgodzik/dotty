import scala.quoted._

object Test {

  def f[T: Type](implicit ctx: QuoteContext) = {
    implicitly[Type[Int]]
    implicitly[Type[List[Int]]]
    implicitly[Type[T]]
    implicitly[Type[List[T]]]
  }
}
