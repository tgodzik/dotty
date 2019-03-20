package tasty.tree.types

import tasty.binary.BinaryOutput
import tasty.names.ScalacNamePickler

import scala.tools.nsc.Global

final class ScalacConstantPickler(val namePool: ScalacNamePickler, val output: BinaryOutput)(implicit g: Global)
  extends ConstantPickler {
  override type Name = Global#Name
  override protected type Constant = Global#Constant

  override def pickleConstant(constant: Constant): Unit = constant.tag match {
    // TODO   case g.NoTag => ???
    case g.UnitTag => pickleUnit()
    case g.BooleanTag => pickleBoolean(constant.booleanValue)
    case g.ByteTag => pickleByte(constant.byteValue)
    case g.ShortTag => pickleShort(constant.shortValue)
    case g.IntTag => pickleInt(constant.intValue)
    case g.LongTag => pickleLong(constant.longValue)
    case g.FloatTag => pickleFloat(constant.floatValue)
    case g.DoubleTag => pickleDouble(constant.doubleValue)
    case g.CharTag => pickleChar(constant.charValue)
    case g.StringTag => pickleString(g.newTermName(constant.stringValue))
    case g.NullTag => pickleNull()
    case g.ClazzTag => pickleClass()
    case g.EnumTag => pickleEnum()
    case _ => throw new UnsupportedOperationException(s"Cannot pickle constant [$constant]")
  }
}
