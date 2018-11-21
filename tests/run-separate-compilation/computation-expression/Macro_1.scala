import scala.quoted._
import scala.tasty.Tasty

object Macro {

  abstract class Symantics {
    def __if[T](cond: Boolean, thenp: => T, elsep: => T): T
  }

  inline def virtualize(symantics: Symantics)(x: => Any): Any = ~virtualizeImpl('(x), '(symantics))

  def virtualizeImpl(x: Expr[Any], sym: Expr[Symantics])(implicit tasty: Tasty): Expr[Any] = {
    import tasty._

    def recurse[T: scala.quoted.Type](x: Term): Expr[T] = {
      x match {
        case Term.Literal(Constant.Int(value)) => '((~value.toExpr).asInstanceOf[T])
        case Term.Block(Nil, e) => recurse(e)
        case Term.Inlined(_, Nil, e) => recurse(e)
        case Term.If(cond, thenp, elsep) => {
          '((~sym).__if(~recurse[Boolean](cond), ~recurse[T](thenp), ~recurse[T](elsep)))
        }
        case _ => '((~x.show.toExpr).asInstanceOf[T])
      }
    }


    recurse[Any](x.toTasty)
  }
}