package dotty.tools.dotc.tasty

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Symbols._
import dotty.tools.dotc.core.Flags._


object FromSymbol {

  def definition(sym: Symbol)(implicit ctx: Context): tpd.Tree = {
    if (sym.is(Package)) packageDef(sym)
    else if (sym == defn.AnyClass) tpd.EmptyTree // FIXME
    else if (sym == defn.NothingClass) tpd.EmptyTree // FIXME
    else if (sym.isClass) classDef(sym.asClass)
    else if (sym.isType) typeDef(sym.asType)
    else if (sym.is(Method)) defDef(sym.asTerm)
    else valDef(sym.asTerm)
  }

  def packageDef(sym: Symbol)(implicit ctx: Context): tpd.Tree = {
    tpd.EmptyTree // FIXME
  }

  def classDef(sym: ClassSymbol)(implicit ctx: Context): tpd.Tree = {
    def toTree(sym: ClassSymbol): tpd.TypeDef = {
      val constr = tpd.DefDef(sym.unforcedDecls.find(_.isPrimaryConstructor).asTerm)
      val body = sym.unforcedDecls.filter(!_.isPrimaryConstructor).map(s => definition(s))
      val superArgs = Nil // TODO
      tpd.ClassDef(sym, constr, body, superArgs)
    }
    toTree(sym)
  }

  def typeDef(sym: TypeSymbol)(implicit ctx: Context): tpd.Tree = tpd.TypeDef(sym)

  def defDef(sym: TermSymbol)(implicit ctx: Context): tpd.Tree = tpd.DefDef(sym)

  def valDef(sym: TermSymbol)(implicit ctx: Context): tpd.Tree = tpd.ValDef(sym)

}
