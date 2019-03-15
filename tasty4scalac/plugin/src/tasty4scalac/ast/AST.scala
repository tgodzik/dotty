package tasty4scalac.ast

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core._

import scala.tools.nsc.Global

trait AST {

  type Name
  type TermName <: Name
  type TypeName <: Name

  type Type
  type TypeRef <: Type
  type TermRef <: Type
  type ParamRef <: Type

  type Constant
  type Tree
  type Modifier
  type Context
  type Symbol

  type Annotation

}

object DottyAST extends AST {
  override type Name = Names.Name
  override type TermName = Names.TermName
  override type TypeName = Names.TypeName

  override type Type = Types.Type
  override type TermRef = Types.TermRef
  override type TypeRef = Types.TypeRef
  override type ParamRef = Types.ParamRef

  override type Constant = Constants.Constant
  override type Tree = tpd.Tree
  override type Modifier = Flags.FlagSet
  override type Context = Contexts.Context
  override type Symbol = Symbols.Symbol

  override type Annotation = Annotations.Annotation
}


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
