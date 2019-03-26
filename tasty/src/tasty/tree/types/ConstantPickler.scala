package tasty.tree.types

import dotty.tools.dotc.core.tasty.TastyFormat._
import tasty.binary.SectionWriter
import tasty.names.WriterNamePool
import tasty.tree.TreeSectionWriter

abstract class ConstantWriter[Constant, Name](nameSection: WriterNamePool[Name],
                                              underlying: SectionWriter)
  extends TreeSectionWriter[Constant, Name](nameSection, underlying) {

  def write(constant: Constant): Unit

  protected final def writeUnitConst(): Unit = tagged(UNITconst) {}

  protected final def writeNullConst(): Unit = tagged(NULLconst) {}

  protected final def writeBooleanConst(value: Boolean): Unit = tagged(if (value) TRUEconst else FALSEconst) {}

  protected final def writeByteConst(value: Byte): Unit = tagged(BYTEconst) {
    writeInteger(value)
  }

  protected final def writeShortConst(value: Short): Unit = tagged(SHORTconst) {
    writeInteger(value)
  }

  protected final def writeIntConst(value: Int): Unit = tagged(INTconst) {
    writeInteger(value)
  }

  protected final def writeLongConst(value: Long): Unit = tagged(LONGconst) {
    writeLong(value)
  }

  protected final def writeFloatConst(value: Float): Unit = tagged(FLOATconst) {
    writeInteger(java.lang.Float.floatToRawIntBits(value))
  }

  protected final def writeDoubleConst(value: Double): Unit = tagged(DOUBLEconst) {
    writeLong(java.lang.Double.doubleToRawLongBits(value))
  }

  protected final def writeCharConst(value: Char): Unit = tagged(CHARconst) {
    writeNat(value)
  }

  protected final def writeStringConst(value: Name): Unit = tagged(STRINGconst) {
    writeName(value)
  }

  protected final def writeClassConst(): Unit = ???

  protected final def writeEnumConst(): Unit = ???

  protected final def writeSymbolConst(): Unit = ???
}
