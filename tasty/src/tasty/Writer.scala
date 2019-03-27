package tasty

trait Pickler[-A] {
  def pickle(value: A): Unit

  def pickleSequence(values: Seq[A]): Unit

  def pickleTerminalSequence(values: Seq[A]): Unit
}
