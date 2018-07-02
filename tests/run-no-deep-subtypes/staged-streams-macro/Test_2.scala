import scala.quoted._

import StagedStreams._

object Test {

  def f: Unit = {
    new Foo(3).foo(4, (x: Expr[Foo]) => '((~x).bar()))
  }

//  val arr = Array(1, 2, 3)
//  def test1() = Stream
//    .of(arr)
//  .fold('{0}, ((a: Expr[Int], b : Expr[Int]) => '{ ~a + ~b }))

//  def test2() = Stream
//    .of('{Array(1, 2, 3)})
//  .map((a: Expr[Int]) => '{ ~a * 2 })
//  .fold('{0}, ((a: Expr[Int], b : Expr[Int]) => '{ ~a + ~b }))
//
//  def test3() = Stream
//    .of('{Array(1, 2, 3)})
//  .flatMap((d: Expr[Int]) => Stream.of('{Array(1, 2, 3)}).map((dp: Expr[Int]) => '{ ~d * ~dp }))
//  .fold('{0}, ((a: Expr[Int], b : Expr[Int]) => '{ ~a + ~b }))
//
//  def test4() = Stream
//    .of('{Array(1, 2, 3)})
//  .filter((d: Expr[Int]) => '{ ~d % 2 == 0 })
//  .fold('{0}, ((a: Expr[Int], b : Expr[Int]) => '{ ~a + ~b }))
//
//  def test5() = Stream
//    .of('{Array(1, 2, 3)})
//  .take('{2})
//  .fold('{0}, ((a: Expr[Int], b : Expr[Int]) => '{ ~a + ~b }))
//
//  def test6() = Stream
//    .of('{Array(1, 1, 1)})
//  .flatMap((d: Expr[Int]) => Stream.of('{Array(1, 2, 3)}).take('{2}))
//  .take('{5})
//  .fold('{0}, ((a: Expr[Int], b : Expr[Int]) => '{ ~a + ~b }))
//
//  def test7() = Stream
//    .of('{Array(1, 2, 3)})
//  .zip(((a : Expr[Int]) => (b : Expr[Int]) => '{ ~a + ~b }), Stream.of('{Array(1, 2, 3)}))
//  .fold('{0}, ((a: Expr[Int], b : Expr[Int]) => '{ ~a + ~b }))
//
//  def test8() = Stream
//    .of('{Array(1, 2, 3)})
//  .zip(((a : Expr[Int]) => (b : Expr[Int]) => '{ ~a + ~b }), Stream.of('{Array(1, 2, 3)}).flatMap((d: Expr[Int]) => Stream.of('{Array(1, 2, 3)}).map((dp: Expr[Int]) => '{ ~d + ~dp })))
//  .fold('{0}, ((a: Expr[Int], b : Expr[Int]) => '{ ~a + ~b }))
//
//  def test9() = Stream
//    .of('{Array(1, 2, 3)}).flatMap((d: Expr[Int]) => Stream.of('{Array(1, 2, 3)}).map((dp: Expr[Int]) => '{ ~d + ~dp }))
//  .zip(((a : Expr[Int]) => (b : Expr[Int]) => '{ ~a + ~b }), Stream.of('{Array(1, 2, 3)}) )
//  .fold('{0}, ((a: Expr[Int], b : Expr[Int]) => '{ ~a + ~b }))
//
//  def test10() = Stream
//    .of('{Array(1, 2, 3)}).flatMap((d: Expr[Int]) => Stream.of('{Array(1, 2, 3)}).map((dp: Expr[Int]) => '{ ~d + ~dp }))
//  .zip(((a : Expr[Int]) => (b : Expr[Int]) => '{ ~a + ~b }), Stream.of('{Array(1, 2, 3)}).flatMap((d: Expr[Int]) => Stream.of('{Array(1, 2, 3)}).map((dp: Expr[Int]) => '{ ~d + ~dp })) )
//  .fold('{0}, ((a: Expr[Int], b : Expr[Int]) => '{ ~a + ~b }))

//  def main(args: Array[String]): Unit = {
//    implicit val toolbox: scala.quoted.Toolbox = dotty.tools.dotc.quoted.Toolbox.make
//
//    println(test1().show)
//    println(test1().run)
//    println
//    println(test2().run)
//    println
//    println(test3().run)
//    println
//    println(test4().run)
//    println
//    println(test5().run)
//    println
//    println(test6().run)
//    println
//    println(test7().run)
//    println
//    println(test8().run)
//    println
//    println(test9().run)
//    println
//    println(test10().run)
//  }
}