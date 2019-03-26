package tasty.tree.types

import dotty.tools.dotc.core.tasty.TastyFormat.{TERMREFpkg, TYPEREFpkg}
import tasty.Writer
import tasty.binary.SectionWriter
import tasty.names.ScalacWriterNamePool

import scala.tools.nsc.Global

final class ScalacTypeWriter(nameSection: ScalacWriterNamePool,
                             underlying: SectionWriter)
                            (implicit g: Global)
  extends TypeWriter[Global#Type, Global#Name](nameSection, underlying) {

  override type Constant = Global#Constant
  override protected val constantWriter: Writer[Constant] = new ScalacConstantWriter(nameSection, underlying)

  override protected def dispatch(t: Global#Type): Unit = t match {
    case g.ConstantType(constant) => constantWriter.write(constant)
    case g.SingleType(pre, sym) =>
      if (sym.hasPackageFlag) tagged(if (sym.isType) TYPEREFpkg else TERMREFpkg) {
        writeName(sym.fullNameAsName('.'))
      } else {
        ???
      }
    case g.TypeRef(pre, sym, args) =>
      if (args.isEmpty) {
        val name = sym.name
        writeTypeRef(name, pre)
      } else ??? // TODO APPLIEDTYPE

    case tpe@g.ThisType(sym) =>
      if (sym.isRoot || !sym.hasPackageFlag) {
        val typeConstructor = tpe.underlying.typeConstructor
        writeThis(typeConstructor)
      } else {
        writePackageTermRef(sym.fullNameAsName('.'))
      }

    case _ => throw new UnsupportedOperationException(s"Cannot pickle type [${t.getClass} $t]")
  }
}
