package tasty

trait Writer[-A] {
  def write(value: A): Unit

  def writeSequence(values: Seq[A]): Unit

  def writeTerminalSequence(values: Seq[A]): Unit
}
