package scala.tasty

import scala.runtime.tasty.Toolbox
import scala.tasty.trees._

trait Context {
  def owner: Definition

  def toTasty[T](expr: quoted.Expr[T]): Term
  def toTasty[T](expr: quoted.Type[T]): TypeTree

  protected[tasty] def toolbox: Toolbox
}

object Context {
  // TODO move to some other place
  /** Compiler context available in a ~ at inline site */
  def compilationContext: Context = throw new Exception("Not in inline ~")

  /** Provides a Contexts that is valid during the execution of `code`.
   *  DO NOT use this context of tasty trees that where generated from a different context.
   */
  def provided[T](code: Context => T)(implicit ctxProvider: ContextProvider): T = ctxProvider.provide(code)
}
