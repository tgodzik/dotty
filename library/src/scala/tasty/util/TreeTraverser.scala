package scala.tasty.util

import scala.tasty.Tasty

abstract class TreeTraverser(implicit tasty0: Tasty) extends TreeAccumulator[Unit] {
  import tasty._

  def traverseTree(tree: TopLevelStatement)(implicit ctx: Context): Unit
  def traverseTypeTree(tree: MaybeTypeTree)(implicit ctx: Context): Unit
  def traverseCaseDef(tree: CaseDef)(implicit ctx: Context): Unit
  def traversePattern(tree: Pattern)(implicit ctx: Context): Unit
  def traverseParent(tree: Parent)(implicit ctx: Context): Unit

  def foldTree(x: Unit, tree: TopLevelStatement)(implicit ctx: Context): Unit = traverseTree(tree)
  def foldTypeTree(x: Unit, tree: MaybeTypeTree)(implicit ctx: Context) = traverseTypeTree(tree)
  def foldCaseDef(x: Unit, tree: CaseDef)(implicit ctx: Context) = traverseCaseDef(tree)
  def foldPattern(x: Unit, tree: Pattern)(implicit ctx: Context) = traversePattern(tree)
  def foldParent(x: Unit, tree: Parent)(implicit ctx: Context) = traverseParent(tree)

  protected def traverseTreeChildren(tree: TopLevelStatement)(implicit ctx: Context): Unit = foldOverTree((), tree)
  protected def traverseTypeTreeChildren(tree: MaybeTypeTree)(implicit ctx: Context): Unit = foldOverTypeTree((), tree)
  protected def traverseCaseDefChildren(tree: CaseDef)(implicit ctx: Context): Unit = foldOverCaseDef((), tree)
  protected def traversePatternChildren(tree: Pattern)(implicit ctx: Context): Unit = foldOverPattern((), tree)
  protected def traverseParentChildren(tree: Parent)(implicit ctx: Context): Unit = foldOverParent((), tree)

}
