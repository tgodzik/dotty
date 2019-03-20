package tasty.tree.types

import dotty.tools.dotc.core.tasty.TastyFormat._
import tasty.tree.TreeSectionPickler

import scala.collection.mutable

abstract class TypePickler extends TreeSectionPickler {
  type Type

  private val cache = mutable.Map[Type, Int]()

  final def pickleType(t: Type): Unit = {
    if (cache.contains(t)) {
      output.writeByte(SHAREDtype)
      output.writeNat(cache(t))
    } else {
      val offset = output.size
      cache.put(t, offset)
      pickle(t)
    }
  }

  protected def pickle(t: Type): Unit

  protected final def pickleTypeRef(name: Name, t: Type): Unit = tagged(TYPEREF) {
    pickleName(name)
    pickleType(t)
  }

  protected final def pickleThis(typeConstructor: Type): Unit = tagged(THIS)(pickleType(typeConstructor))

  protected final def pickleTermRef(name: Name): Unit = tagged(TERMREFpkg)(pickleName(name))
}
