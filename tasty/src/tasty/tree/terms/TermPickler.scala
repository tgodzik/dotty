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

  protected final def picklePackageRef(nameRef: NameRef): Unit = tagged(TERMREFpkg)(output.writeNat(nameRef.index))
  
  protected final def onIdent(name: Name, t: Type): Unit = tagged(IDENT) {
    pickleName(name)
    pickleType(t)
  }

  protected final def onSelect(name: Name, term: Term): Unit = tagged(IDENT) {
    pickleName(name)
    pickleTerm(term)
  }
}
