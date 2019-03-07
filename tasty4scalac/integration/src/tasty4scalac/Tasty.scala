package tasty4scalac

trait Tasty

object Tasty {
  def apply(bytes: Array[Byte]): Tasty = Binary(bytes)

  case class Binary(bytes: Array[Byte]) extends Tasty

}
