package hello

import scala.scalajs.js

@js.native
trait ObjectConstructor extends js.Any{
  def assign[T, U](target: T, source: U): T with U = js.native
}

trait MyTrait {
  val x = 7
  def foo(y: Int) = x
}

object HelloWorld extends MyTrait {
  def main(args: Array[String]): Unit = {
    println("hello dotty.js!")
    println(foo(4))
  }
}

