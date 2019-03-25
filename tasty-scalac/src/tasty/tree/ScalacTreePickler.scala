package tasty.tree

import tasty.binary.BinaryOutput
import tasty.names.ScalacNamePickler
import tasty.tree.terms.ScalacTermPickler

import scala.tools.nsc.Global

final class ScalacTreePickler(val namePool: ScalacNamePickler,
                              val output: BinaryOutput)(implicit g: Global) extends TreePickler {
  override type Tree = Global#Tree
  override protected type Name = Global#Name
  override protected type Term = Global#Tree
  override protected type Type = Global#Type
  override protected type Modifier = Global#Symbol

  private val termPickler = new ScalacTermPickler(namePool, output)
  private val modifierPickler = new ScalacModifierPickler(namePool, output)

  override protected def pickle(tree: Global#Tree): Unit = if (!tree.isEmpty) {
    val symbol = tree.symbol
    val owner = symbol.owner

    tree match {
      case g.PackageDef(id, statements) => picklePackage(id, statements)
      case g.ClassDef(mods, name, tparams, impl) => pickleTypeDef(name, impl, Seq(tree.symbol))
      case g.Template(parents, self, body) =>
        // TODO type parameters and parameters and self
        // need to pickle the super constructor call as parent_term
        pickleParentConstructor(body)
        pickleTemplate(Nil, Nil, parents, None, body)

      case tree@g.DefDef(mods, name, tparams, vparams, tpt, rhs) =>
        val returnType = if (tree.symbol.isConstructor) g.TypeTree(g.definitions.UnitTpe) else tpt
        val body = if (tree.symbol.isPrimaryConstructor) None
        else Some(tree.rhs)
        val name = if (symbol.isConstructor && owner.isTrait) g.nme.CONSTRUCTOR // FIXME: this is not enough, if trait is PureInterface, no $init$ is generated at all
        else symbol.name

        pickleDefDef(name, tparams, vparams, returnType, body, Seq(symbol))

      case _ => pickleTerm(tree)
    }
  }

  override protected def pickleTerm(term: Global#Tree): Unit = termPickler.pickleTerm(term)

  override protected def pickleModifier(modifier: Global#Symbol): Unit = modifierPickler.pickleModifier(modifier)

  private def pickleParentConstructor(body: List[Global#Tree]): Unit = {
    body.find(_.symbol.isPrimaryConstructor).foreach {
      case defdef: g.DefDef =>
        pickleTerm(defdef.rhs.asInstanceOf[Global#Block].stats.head)
    }
  }
}

