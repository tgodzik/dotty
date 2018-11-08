package scala.tasty.reflect

/** Extension methods on scala.quoted.{Expr|Type} to convert to scala.tasty.Tasty objects */
trait QuotedOps extends TastyCore {

  trait QuotedExprAPI {
    def toTasty(implicit ctx: Context): Term
  }
  implicit def QuotedExprDeco[T](expr: quoted.Expr[T]): QuotedExprAPI

  trait QuotedTypeAPI {
    def toTasty(implicit ctx: Context): TypeTree
  }
  implicit def QuotedTypeDeco[T](tpe: quoted.Type[T]): QuotedTypeAPI

  trait TermToQuotedAPI {
    def toExpr[T: scala.quoted.Type](implicit ctx: Context): scala.quoted.Expr[T]
    def toExpr2(implicit ctx: Context): scala.quoted.Expr[Any]
  }
  implicit def TermToQuoteDeco(term: Term): TermToQuotedAPI

  trait TypeToQuotedAPI {
    def toType(implicit ctx: Context): scala.quoted.Type[_]
  }
  implicit def TypeToQuoteDeco(tpe: Type): TypeToQuotedAPI
  implicit def TypeTreeToQuoteDeco(tpt: TypeTree): TypeToQuotedAPI

}
