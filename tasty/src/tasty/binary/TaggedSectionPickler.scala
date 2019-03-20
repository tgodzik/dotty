package tasty.binary

trait TaggedSectionPickler {
  protected val output: BinaryOutput

  protected def startsSubsection(tag: Int): Boolean

  final protected def tagged(tag: Int)(op: => Unit): Unit = {
    output.writeByte(tag)
    if (startsSubsection(tag)) output.writeSubsection(op)
    else op
  }
}
