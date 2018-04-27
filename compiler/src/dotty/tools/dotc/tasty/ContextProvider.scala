package dotty.tools.dotc.tasty

import scala.tasty.Context

object ContextProvider {

  implicit def provider: scala.tasty.ContextProvider = new scala.tasty.ContextProvider {
    def provide[T](code: (Context) => T): T = {
      ???
    }
  }
}
