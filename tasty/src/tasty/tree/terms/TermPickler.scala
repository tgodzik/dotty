package tasty.tree.terms

import dotty.tools.dotc.core.tasty.TastyFormat._
import tasty.names.NameRef
import tasty.tree.TreeSectionPickler

import scala.collection.mutable

abstract class TermPickler extends TreeSectionPickler {
  type Term
  protected type Type

  private val cache = mutable.Map[Term, Int]()

  final def pickleTerm(term: Term): Unit = {
    if (cache.contains(term)) {
      output.writeByte(SHAREDterm)
      output.writeNat(cache(term))
    } else {
      val offset = output.size
      cache.put(term, offset)
      pickle(term)
    }
  }

  protected def pickle(tree: Term): Unit

  protected def pickleType(t: Type): Unit

  protected final def picklePackageRef(name: Name): Unit = tagged(TERMREFpkg)(pickleName(name))

  protected final def pickleConstructor(initName: Name, tpe: Type): Unit = tagged(SELECT) {
    pickleName(initName)
    tagged(NEW) {
      pickleType(tpe)
    }
  }

  protected final def onIdent(name: Name, t: Type): Unit = tagged(IDENT) {
    pickleName(name)
    pickleType(t)
  }

  protected final def onSelect(name: Name, term: Term): Unit = tagged(IDENT) {
    pickleName(name)
    pickleTerm(term)
  }

  protected final def onBlock(stats: List[Term], expression: Term): Unit = tagged(BLOCK) {
    pickleTerm(expression)
    stats.foreach(pickleTerm)
  }

  protected final def onApply(func: Term, args: List[Term]): Unit = tagged(APPLY) {
    pickleTerm(func)
    args.foreach(pickleTerm)
  }
}
