package tasty.tree.types

import dotty.tools.dotc.core.tasty.TastyFormat._
import tasty.binary.BinaryOutput
import tasty.names.ScalacNamePool

import scala.tools.nsc.Global

final class ScalacTypePickler(val namePool: ScalacNamePool,val output: BinaryOutput)(implicit g: Global) extends TypePickler {
  override type Name = Global#Name
  override type Type = Global#Type

  private val constantPickler = new ScalacConstantPickler(namePool, output)

    override protected def pickle(t: Global#Type): Unit = t match {
    case g.ConstantType(value) => constantPickler.pickleConstant(value)
    case g.SingleType(pre, sym) =>
      if (sym.hasPackageFlag) tagged(if (sym.isType) TYPEREFpkg else TERMREFpkg) {
        pickleName(sym.fullNameAsName('.'))
      } else {
        ???
      }
    case g.TypeRef(pre, sym, args) =>
      if (args.isEmpty) {
        val name = sym.name
        pickleTypeRef(name, pre)
      } else ??? // TODO APPLIEDTYPE

    case tpe@g.ThisType(sym) =>
      if (sym.isRoot || !sym.hasPackageFlag) {
        val typeConstructor = tpe.underlying.typeConstructor
        pickleThis(typeConstructor)
      } else {
        pickleTermRef(sym.fullNameAsName('.'))
      }

    case _ => throw new UnsupportedOperationException(s"Cannot pickle type [${t.getClass} $t]")
  }

  private def pickleAnnotations(annotations: List[Global#AnnotationInfo]) = {
    ???
  }

}
