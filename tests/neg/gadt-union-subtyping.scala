object `gadt-union-subtyping` {
  enum SUB[A, +B] { case Refl[T]() extends SUB[T, T] }

  def foo[T](t: T, sub1: 5 SUB T, sub2: 6 SUB T): T = {
    (sub1, sub2) match {
      case (SUB.Refl(), SUB.Refl()) =>
        val a: 5 = t // error
        val _t: T = (5 : 5)
        ???
    }
  }

  def bar[T](t: T, sub1: 5 SUB T, sub2: 6 SUB T, sub3: String SUB T): T = {
    (sub1, sub2, sub3) match {
      case (SUB.Refl(), SUB.Refl(), SUB.Refl()) =>
        val a: 5 = t // error
        val b: 6 = t // error
        val c: String = t // error
        val _t: T = (5 : 5)
        ???
    }
  }

  def baz[T](t: T, sub1: String SUB T, sub2: 5 SUB T, sub3: 6 SUB T): T = {
    (sub1, sub2, sub3) match {
      case (SUB.Refl(), SUB.Refl(), SUB.Refl()) =>
        val a: 5 = t // error
        val b: 6 = t // error
        val c: String = t // error
        val _t: T = (5 : 5)
        ???
    }
  }
}
