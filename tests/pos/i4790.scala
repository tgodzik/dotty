import scala.collection.immutable

class Test {
  def foo(as: immutable.Seq[Int]) = {
    val List(_, bs: _*) = as
    val cs: collection.Seq[Int] = bs
  }
}
