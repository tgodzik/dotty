import scala.quoted._

object Macros {
  inline def unrolledForeach(seq: IndexedSeq[Int], f: Int => Unit, inline unrollSize: Int): Unit = ~unrolledForeachImpl('(seq), '(f), unrollSize)

  def unrolledForeachImpl(seq: Expr[IndexedSeq[Int]], f: Expr[Int => Unit], unrollSize: Int): Expr[Unit] = '{
    val size = (~seq).length
    assert(size % (~unrollSize.toExpr) == 0) // for simplicity of the implementation
    var i = 0
    while (i < size) {
      ~{
        block(List.range(0, unrollSize).map(j => '{
          val idx = ~i + ~j.toExpr
          val el = (~seq)(idx)
          ~f.apply('(el))
        }))
      }
      i += ~unrollSize.toExpr
    }

  }

  def block(stats: List[Expr[Unit]]): Expr[Unit] = stats match {
    case x :: xs => '{ ~x; ~block(xs) }
    case Nil => '()
  }
}
