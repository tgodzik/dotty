package tasty.tree.types

import tasty.binary.SectionWriter
import tasty.names.ScalacWriterNamePool

import scala.tools.nsc.Global

final class ScalacConstantWriter(val nameSection: ScalacWriterNamePool,
                                 output: SectionWriter)
                                (implicit g: Global)
  extends ConstantWriter[Global#Constant, Global#Name](nameSection, output) {

  override def write(constant: Global#Constant): Unit = constant.tag match {
    // TODO   case g.NoTag => ???
    case g.UnitTag => writeUnitConst()
    case g.BooleanTag => writeBooleanConst(constant.booleanValue)
    case g.ByteTag => writeByteConst(constant.byteValue)
    case g.ShortTag => writeShortConst(constant.shortValue)
    case g.IntTag => writeIntConst(constant.intValue)
    case g.LongTag => writeLongConst(constant.longValue)
    case g.FloatTag => writeFloatConst(constant.floatValue)
    case g.DoubleTag => writeDoubleConst(constant.doubleValue)
    case g.CharTag => writeCharConst(constant.charValue)
    case g.StringTag => writeStringConst(g.newTermName(constant.stringValue))
    case g.NullTag => writeNullConst()
    case g.ClazzTag => writeClassConst()
    case g.EnumTag => writeEnumConst()
    case _ => throw new UnsupportedOperationException(s"Cannot write constant [$constant]")
  }
}
