package scala.tasty.reflect

/** Extension methods on scala.quoted.{Expr|Type} to convert to scala.tasty.Tasty objects */
trait QuotedOps extends Core {

  trait QuotedExprAPI {
    /** View this expression `Expr[T]` as a `Term` */
    def unseal(implicit ctx: Context): Term
  }
  implicit def QuotedExprDeco[T](expr: quoted.Expr[T]): QuotedExprAPI

  implicit class QuotedTypeDeco[T <: AnyKind](tpe: quoted.Type[T]) {
    /** View this expression `Type[T]` as a `TypeTree` */
    def unseal(implicit ctx: Context): TypeTree = unsealType(tpe)
  }

  protected def unsealType(tpe: quoted.Type[_])(implicit ctx: Context): TypeTree

  implicit class TermToQuoteDeco(term: Term) {
    /** Convert `Term` to an `Expr[T]` and check that it conforms to `T` */
    def seal[T: scala.quoted.Type](implicit ctx: Context): scala.quoted.Expr[T] =
      // Note that T should not be poly-kinded in this method
      sealTerm(term, unsealType(implicitly[scala.quoted.Type[T]])).asInstanceOf[scala.quoted.Expr[T]]
  }

  protected def sealTerm(term: Term, tpt: TypeTree)(implicit ctx: Context): scala.quoted.Expr[_]

  trait TypeToQuotedAPI {
    /** Convert `Type` to an `quoted.Type[T]` */
    def seal(implicit ctx: Context): scala.quoted.Type[_]
  }
  implicit def TypeToQuoteDeco(tpe: Type): TypeToQuotedAPI
}
