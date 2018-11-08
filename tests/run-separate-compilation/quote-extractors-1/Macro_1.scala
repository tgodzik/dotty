import scala.quoted._

import scala.tasty._

object Macros {
  import Extractors._

  inline def lift[T](inline lang: Lang[Expr[T]])(x: => Any): T = ~liftImpl('(x))(Tasty.macroContext,lang)

  private def liftImpl[T](x: Expr[Any])(implicit tasty: Tasty, lang: Lang[Expr[T]]): Expr[T] = {
    import tasty._

    val IsInt = new QuotedTypedAs[Int]

    x match {
      case QuotedConstant(v: Int) => lang.num(v)
      case QuotedConstant(v: Boolean) => lang.bool(v)

      case QuotedTyped(ExprAndType(expr, tpt)) => // case '{ ~expr: ~tpt } =>
//        '{ val a: ~tpt = ~expr } // test type binding

       lang.typed(liftImpl(expr), "type goes here") // TODO lift types

      case QuotedIf(cond, thenp, elsep) => // case '{ if (~cond) ~thenp else ~elsep } =>
        lang.if_(liftImpl(cond), liftImpl(thenp), liftImpl(elsep))

      case IsInt(x) => // case '{ ~of[Int](expr) } => // case of[Int](expr) =>
        lang.typed(lang.error(), "Int")

      case expr => // case '{ ~expr } => // cancelation
        lang.error()
    }
  }
}

trait Lang[T] {
  def num(v: Int): T
  def bool(v: Boolean): T
  def if_(cond: T, thenp: T, elsep: T): T
  def typed(expr: T, tpt: String): T // TODO lift types
  def error(): T
}

case object StringLang extends Lang[Expr[String]] {
  def num(v: Int): Expr[String] = v.toString.toExpr
  def bool(v: Boolean): Expr[String] = v.toString.toExpr
  def if_(cond: Expr[String], thenp: Expr[String], elsep: Expr[String]): Expr[String] = '(s"if (${~cond}) ${~thenp} else ${~elsep}")
  def typed(expr: Expr[String], tpt: String): Expr[String] = '(s"(${~expr}: ${~tpt.toExpr})") // TODO lift types
  def error(): Expr[String] = '("<error>")
}


object Extractors {

  object QuotedConstant {
    def unapply[T: Type](expr: Expr[T])(implicit tasty: Tasty): Option[T] = {
      import tasty._
      normailize(tasty)(expr.toTasty) match {
        case Term.Literal(c) => Some(c.value.asInstanceOf[T])
        case _  => None
      }
    }
  }

  class QuotedTypedAs[T] {
    def unapply[U >: T](expr: Expr[U])(implicit t: Type[T], tasty: Tasty): Option[Expr[T]] = {
      import tasty._
      if (expr.toTasty.tpe <:< t.toTasty.tpe) Some(expr.asInstanceOf[Expr[T]])
      else None
    }
  }

  // Helper to bind the types
  class ExprAndType[T](e: Expr[_], t: Type[_]) {
    type U <: T
    def expr: Expr[U] = e.asInstanceOf[Expr[U]]
    def tpe: Type[U] = t.asInstanceOf[Type[U]]
  }

  object ExprAndType {
    def unapply[T](arg: ExprAndType[T]): Option[(Expr[arg.U], Type[arg.U])] = Some(arg.expr, arg.tpe)
  }

  object QuotedTyped {
    def unapply[T](expr: Expr[T])(implicit tasty: Tasty): Option[ExprAndType[T]] = {
      import tasty._
      normailize(tasty)(expr.toTasty) match {
        case Term.Typed(expr, tpt) => Some(new ExprAndType[T](expr.toExpr2, tpt.toType))
        case _  => None
      }
    }
  }

  object QuotedIf {
    def unapply[T: Type](expr: Expr[T])(implicit tasty: Tasty): Option[(Expr[Boolean], Expr[T], Expr[T])] = {
      import tasty._
      normailize(tasty)(expr.toTasty) match {
        case Term.If(cond, thenp, elsep) => Some((cond.toExpr[Boolean], thenp.toExpr[T], elsep.toExpr[T]))
        case _  => None
      }
    }
  }

  private def normailize(tasty: Tasty)(tree: tasty.Term): tasty.Term = {
    import tasty._
    tree match {
      case Term.Block(Nil, e) => normailize(tasty)(e)
      case Term.Inlined(_, Nil, e) => normailize(tasty)(e)
      case _  => tree
    }
  }



}
