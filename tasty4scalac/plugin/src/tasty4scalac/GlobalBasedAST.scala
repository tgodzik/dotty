package tasty4scalac

import dotty.tools.dotc.core.tasty.translator.AST

import scala.tools.nsc.Global


final class GlobalBasedAST(val global: Global) extends AST {
  override type Name = global.Name
  override type TypeName = global.TypeName
  override type TermName = global.TermName

  override type Type = global.Type
  override type TypeRef = global.TypeRef
  // TODO find proper alternative
  override type TermRef = global.Type
  override type ParamRef = global.TypeRef

  override type Constant = global.Constant
  override type Tree = global.Tree
  override type Modifier = global.FlagSet
  override type Context = Nothing
  override type Symbol = global.Symbol

  override type Annotation = global.Annotation
}

