
object Foo {
  transparent def g(inline p: Int => Boolean): Boolean = ~{ // error
    if(p(5)) '(true)
    else '(false)
  }
}
