package tasty.tree.types

import dotty.tools.dotc.core.tasty.TastyFormat._
import tasty.tree.TreeSectionPickler

abstract class ConstantPickler extends TreeSectionPickler {
  protected type Constant

  import output._

  def pickleConstant(constant: Constant): Unit

  protected final def pickleUnit(): Unit = tagged(UNITconst) {}

  protected final def pickleNull(): Unit = tagged(NULLconst) {}

  protected final def pickleBoolean(value: Boolean): Unit = tagged(if (value) TRUEconst else FALSEconst) {}

  protected final def pickleByte(value: Byte): Unit = tagged(BYTEconst) {
    writeInt(value)
  }

  protected final def pickleShort(value: Short): Unit = tagged(SHORTconst) {
    writeInt(value)
  }

  protected final def pickleInt(value: Int): Unit = tagged(INTconst) {
    writeInt(value)
  }

  protected final def pickleLong(value: Long): Unit = tagged(LONGconst) {
    writeLongInt(value)
  }

  protected final def pickleFloat(value: Float): Unit = tagged(FLOATconst) {
    writeInt(java.lang.Float.floatToRawIntBits(value))
  }

  protected final def pickleDouble(value: Double): Unit = tagged(DOUBLEconst) {
    writeLongInt(java.lang.Double.doubleToRawLongBits(value))
  }

  protected final def pickleChar(value: Char): Unit = tagged(CHARconst) {
    writeNat(value)
  }

  protected final def pickleString(value: Name): Unit = tagged(STRINGconst) {
    pickleName(value)
  }

  protected final def pickleClass(): Unit = ???

  protected final def pickleEnum(): Unit = ???

  protected final def pickleSymbol(): Unit = ???


}
