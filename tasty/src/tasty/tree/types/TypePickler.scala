package tasty.tree.types

import dotty.tools.dotc.core.tasty.TastyFormat._
import tasty.Writer
import tasty.binary.SectionWriter
import tasty.names.WriterNamePool
import tasty.tree.TreeSectionWriter

import scala.collection.mutable

abstract class TypeWriter[Type, Name](nameSection: WriterNamePool[Name],
                                      underlying: SectionWriter)
  extends TreeSectionWriter[Type, Name](nameSection, underlying) {
  type Constant

  final val cache = mutable.Map[Type, Int]()

  protected def constantWriter: Writer[Constant]

  final def write(value: Type): Unit =
    if (cache.contains(value)) tagged(SHAREDtype) {
      writeRef(cache(value))
    } else {
      cache += value -> currentOffset
      dispatch(value)
    }

  protected def dispatch(value: Type): Unit

  protected final def writeTypeRef(name: Name, pre: Type): Unit = tagged(TYPEREF) {
    writeName(name)
    write(pre)
  }

  protected final def writeThis(typeConstructor: Type): Unit = tagged(THIS)(write(typeConstructor))

  protected final def writePackageTermRef(name: Name): Unit = tagged(TERMREFpkg) {
    writeName(name)
  }

  protected final def writeAnd(left: Type, right: Type): Unit = tagged(ANDtype) {
    write(left)
    write(right)
  }

  protected final def writeOr(left: Type, right: Type): Unit = tagged(ORtype) {
    write(left)
    write(right)
  }
}
