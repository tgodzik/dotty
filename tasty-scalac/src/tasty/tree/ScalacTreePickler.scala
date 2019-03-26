package tasty.tree.terms

import dotty.tools.dotc.core.tasty.TastyFormat.SELECT
import tasty.ScalacConversions
import tasty.binary.SectionWriter
import tasty.names.ScalacWriterNamePool
import tasty.tree.types.{ScalacConstantWriter, ScalacTypeWriter}
import tasty.tree.{ScalacModifierWriter, TreeWriter}

import scala.tools.nsc.Global

final class ScalacTreeWriter(nameSection: ScalacWriterNamePool,
                             underlying: SectionWriter)
                            (implicit val g: Global)
  extends TreeWriter[Global#Tree, Global#Name](nameSection, underlying) with ScalacConversions {

  override protected type Type = Global#Type
  override protected type Modifier = Global#Symbol
  override protected type Constant = Global#Constant

  override protected val typeWriter: ScalacTypeWriter = new ScalacTypeWriter(nameSection, underlying)

  override protected val constantWriter: ScalacConstantWriter = new ScalacConstantWriter(nameSection, underlying)

  override protected val modifierWriter: ScalacModifierWriter = new ScalacModifierWriter(nameSection, underlying)

  override protected def dispatch(tree: Global#Tree): Unit = {
    val symbol = tree.symbol
    // symbol might be null
    lazy val owner = symbol.owner

    tree match {
      case g.PackageDef(id, statements) => writePackageDef(id, statements)
      case g.ClassDef(mods, name, tparams, impl) => writeTypeDef(name, impl, Seq(tree.symbol))
      case g.Template(parents, self, body) =>
        // TODO type parameters and parameters and self

        // need to write the super constructor call as parent_term
        def parentConstructor = {
          body.find(_.symbol.isPrimaryConstructor).map {
            case defdef: g.DefDef => defdef.rhs.asInstanceOf[Global#Block].stats.head
          }.toSeq
        }

        val augmentedParents = parentConstructor ++ parents
        writeTemplate(Nil, Nil, augmentedParents, None, body)

      case tree@g.DefDef(mods, name, tparams, vparams, tpt, rhs) =>
        val returnType = if (tree.symbol.isConstructor) g.TypeTree(g.definitions.UnitTpe) else tpt
        val body = if (tree.symbol.isPrimaryConstructor) None // TODO: Check if there's no information lost here
        else Some(tree.rhs)
        val name = if (symbol.isConstructor && owner.isTrait) g.nme.CONSTRUCTOR // FIXME: this is not enough, if trait is PureInterface, no $init$ is generated at all
        else symbol.name

        writeDefDef(name, tparams, vparams, returnType, body, Seq(symbol))

      case g.Ident(name) =>
        val isNonWildcardTerm = tree.isTerm && name != g.nme.WILDCARD
        if (isNonWildcardTerm) {
          // The type of a tree Ident should be a TERMREF
          val tp1 = tree.tpe match {
            case _: g.TypeRef => g.SingleType(owner.thisType, symbol)
            case _: g.MethodType => g.SingleType(owner.thisType, symbol) // Happens on calls to LabelDefs
            case t => t
          }
          typeWriter.write(tp1)
        }
        else if (tree.isTerm) writeIdent(name, tree.tpe)
        else ??? // TODO writeIdentTpt(name, tree.tpe)

      case g.Select(qualifier, name) =>
        val appliesTypesToConstructor = symbol.isConstructor && owner.typeParams.nonEmpty

        if (appliesTypesToConstructor) {
          val g.TypeRef(_, _, targs) = qualifier.tpe.widen
          ??? // TODO writeTypeApply(tree, targs)
        } else if (symbol.hasPackageFlag && !symbol.isRoot) {
          writePackageRef(g.TermName(tree.toString()))
        } else if (symbol.isConstructor) {
          writeConstructor(g.TermName("<init>"), owner.typeOfThis)
        } else writeSelect(name, qualifier)

      case g.Apply(fun, args) => writeApply(fun, args)

      case g.Block(stats, expr) => writeBlock(expr, stats)

      case g.TypeTree() => typeWriter.write(tree.tpe)

      case g.Literal(constant) => constantWriter.write(constant)

      case _ => throw new UnsupportedOperationException(s"Cannot write tree: [${tree.getClass}: $tree")
    }
  }

  private def writeConstructor(initName: Global#Name, tpe: Global#Type): Unit = tagged(SELECT) {
    writeName(initName)
    writeNew(tpe)
  }
}
