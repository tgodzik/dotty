package dotty.tools.dotc.tasty
package internal

import dotty.tools.dotc.core.Contexts.Context

import scala.tasty.trees

class TastyContext(val ctx: Context) extends scala.tasty.Context {
  override def owner: trees.Definition = Definition(ctx.owner)(ctx)
  override def toolbox: scala.runtime.tasty.Toolbox = Toolbox
}
