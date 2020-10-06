package hello

import scala.scalajs.js

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

