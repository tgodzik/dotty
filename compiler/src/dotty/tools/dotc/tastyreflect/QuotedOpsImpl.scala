package dotty.tools.dotc.tastyreflect

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.core.Flags._
import dotty.tools.dotc.core.Symbols.defn
import dotty.tools.dotc.core.StdNames.nme
import dotty.tools.dotc.core.quoted.PickledQuotes
import dotty.tools.dotc.core.Types

trait QuotedOpsImpl extends scala.tasty.reflect.QuotedOps with CoreImpl {

  def QuotedExprDeco[T](x: scala.quoted.Expr[T]): QuotedExprAPI = new QuotedExprAPI {
    def unseal(implicit ctx: Context): Term = PickledQuotes.quotedExprToTree(x)
  }

  protected def unsealType(tpe: quoted.Type[_])(implicit ctx: Context): TypeTree =
    PickledQuotes.quotedTypeToTree(tpe)

  protected def sealTerm(term: Term, tpt: TypeTree)(implicit ctx: Context): scala.quoted.Expr[_] = {
    def etaExpand(term: Term): Term = term.tpe.widen match {
      case mtpe: Types.MethodType if !mtpe.isParamDependent =>
        val closureResType = mtpe.resType match {
          case t: Types.MethodType => t.toFunctionType()
          case t => t
        }
        val closureTpe = Types.MethodType(mtpe.paramNames, mtpe.paramInfos, closureResType)
        val closureMethod = ctx.newSymbol(ctx.owner, nme.ANON_FUN, Synthetic | Method, closureTpe)
        tpd.Closure(closureMethod, tss => etaExpand(new tpd.TreeOps(term).appliedToArgs(tss.head)))
      case _ => term
    }

    val expectedType = tpt.tpe
    val expanded = etaExpand(term)
    if (expanded.tpe <:< expectedType) {
      new scala.quoted.Exprs.TastyTreeExpr(expanded)
    } else {
      throw new scala.tasty.TastyTypecheckError(
        s"""Term: ${term.show}
           |did not conform to type: ${expectedType.show}
           |""".stripMargin
      )
    }
  }

  def TypeToQuoteDeco(tpe: Types.Type): TypeToQuotedAPI = new TypeToQuotedAPI {
    def seal(implicit ctx: Context): quoted.Type[_] = {
      val dummySpan = ctx.owner.span // FIXME
      new scala.quoted.Types.TreeType(tpd.TypeTree(tpe).withSpan(dummySpan))
    }
  }
}
