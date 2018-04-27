package scala.tasty

import scala.runtime.tasty.Toolbox
import scala.tasty.trees.Definition

trait Context {
  def owner: Definition
  protected[tasty] def toolbox: Toolbox
}
