package tasty.tree

import dotty.tools.dotc.core.tasty.TastyFormat._

import scala.collection.mutable

abstract class TreePickler extends TreeSectionPickler {
  type Tree
  protected type Term
  protected type Type
  protected type Modifier

  // TODO share cache with terms
  private val cache = mutable.Map[Tree, Int]()

  final def pickleTree(tree: Tree): Unit = {
    if (cache.contains(tree)) {
      output.writeByte(SHAREDterm)
      output.writeNat(cache(tree))
    } else {
      val offset = output.size
      cache.put(tree, offset)
      pickle(tree)
    }
  }

  protected def pickle(tree: Tree): Unit

  protected def pickleTerm(term: Term): Unit

  protected def pickleModifier(modifier: Modifier): Unit

  protected final def picklePackage(term: Term, statements: Seq[Tree]): Unit = tagged(PACKAGE) {
    pickleTerm(term)
    statements.foreach(pickleTree)
  }

  protected final def pickleTypeDef(name: Name, template: Tree, modifiers: Seq[Modifier]): Unit = tagged(TYPEDEF) {
    pickleName(name)
    pickleTree(template)
    modifiers.foreach(pickleModifier)
  }

  protected final def pickleTemplate(typeParameters: Seq[Any], parameters: Seq[Any], parents: Seq[Term],
                                     self: Option[(Name, Term)], statements: Seq[Tree]): Unit = tagged(TEMPLATE) {
    // TODO {type,}parameters
    // parents.foreach(pickleTerm)
    self.foreach {
      case (name, tp) =>
        pickleName(name)
        pickleTerm(tp)
    }
    statements.foreach(pickleTree)
  }

  protected def pickleDefDef(name: Name, typeParameters: Seq[Any], parameters: Seq[Any],
                             returnType: Term, body: Option[Term], modifiers: Seq[Modifier]): Unit = tagged(DEFDEF) {
    pickleName(name)
    // TODO {type,}parameters
    pickleTerm(returnType)
    body.foreach(pickleTerm)
    modifiers.foreach(pickleModifier)
  }
}
