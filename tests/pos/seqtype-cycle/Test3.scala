package scala

class specialized(types: Int*) extends scala.annotation.StaticAnnotation

package collection {
  class Foo[@specialized(1, 2) A]

  trait Seq[A] extends Foo[A]

  trait SeqFactory[CC[X] <: Seq[X]]
  object Seq extends SeqFactory[Seq]
}
