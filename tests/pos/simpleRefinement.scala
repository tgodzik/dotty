
class Foo {
  val bar = new Bar {
    type T = Int
    val x: Long = 2L
    def y: Boolean = true
    def z(): Char = 'f'
    def z2()(): Char = 'g'
    def w[T]: String = "a"
    def w2[T](a: Null)(b: Null): Null = null
  }
}

trait Bar {
  type T
  val x: Any
  def y: Any
  def z(): Any
  def z2()(): Any
  def w[T]: Any
  def w2[T](a: Null)(b: Null): Any
}
