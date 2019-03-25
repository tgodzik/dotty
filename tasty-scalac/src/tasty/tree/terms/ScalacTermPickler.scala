package tasty.tree.terms

import tasty.ScalacConversions
import tasty.binary.BinaryOutput
import tasty.names.ScalacNamePickler
import tasty.tree.types.ScalacTypePickler

import scala.tools.nsc.Global

final class ScalacTermPickler(val namePool: ScalacNamePickler, val output: BinaryOutput)(implicit val g: Global)
  extends TermPickler with ScalacConversions {
  type Term = Global#Tree
  override protected type Name = Global#Name
  override protected type Type = Global#Type

  private val typePickler = new ScalacTypePickler(namePool, output)

  override protected def pickle(term: Term): Unit = {
    val symbol = term.symbol
    val owner = symbol.owner

    term match {
      case g.Ident(name) =>
        val isNonWildcardTerm = term.isTerm && name != g.nme.WILDCARD
        if (isNonWildcardTerm) {
          // The type of a term Ident should be a TERMREF
          val tp1 = term.tpe match {
            case _: g.TypeRef => g.SingleType(owner.thisType, symbol)
            case _: g.MethodType => g.SingleType(owner.thisType, symbol) // Happens on calls to LabelDefs
            case t => t
          }
          pickleType(tp1)
        }
        else if (term.isTerm) onIdent(name, term.tpe)
      // TODO else onIdentTpt(name, tree.tpe)

      case g.Select(qualifier, name) =>
        val appliesTypesToConstructor = symbol.isConstructor && owner.typeParams.nonEmpty

        if (appliesTypesToConstructor) {
          val g.TypeRef(_, _, targs) = qualifier.tpe.widen
          // TODO onTypeApply(tree, targs)
        } else if (symbol.hasPackageFlag && !symbol.isRoot) {
          picklePackageRef(g.TermName(term.toString()))
        } else onSelect(name, qualifier)

      case g.TypeTree() => pickleType(term.tpe)

      case _ => throw new UnsupportedOperationException(s"Cannot pickle term: [${term.getClass}: $term")
    }
  }

  override protected def pickleType(t: Global#Type): Unit = typePickler.pickleType(t)
}
