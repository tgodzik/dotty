package dotty.tools.dotc.core.tasty.translator

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core._

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
