package tasty.tree

import dotty.tools.dotc.core.tasty.TastyFormat._
import tasty.binary.SectionWriter
import tasty.names.WriterNamePool
import tasty.tree.types.{ConstantWriter, TypeWriter}

import scala.collection.mutable

abstract class TreeWriter[Tree, Name](nameSection: WriterNamePool[Name],
                                      underlying: SectionWriter)
  extends TreeSectionWriter[Tree, Name](nameSection, underlying) {

  protected type Type
  protected type Modifier
  protected type Constant

  final val cache = mutable.Map[Tree, Int]()

  protected def typeWriter: TypeWriter[Type, Name]

  protected def constantWriter: ConstantWriter[Constant, Name]

  protected def modifierWriter: ModifierWriter[Modifier, Name]

  final def write(value: Tree): Unit =
    if (cache.contains(value)) tagged(SHAREDtype) {
      writeRef(cache(value))
    } else {
      cache += value -> currentOffset
      dispatch(value)
    }

  protected def dispatch(tree: Tree): Unit

  // Top Level Statements
  protected final def writePackageDef(id: Tree, statements: Seq[Tree]): Unit = tagged(PACKAGE) {
    write(id)
    writeTerminalSequence(statements)
  }

  protected final def writeTypeDef(name: Name, template: Tree, modifiers: Seq[Modifier]): Unit = tagged(TYPEDEF) {
    writeName(name)
    write(template)
    modifierWriter.writeTerminalSequence(modifiers)
  }

  protected final def writeTemplate(typeParameters: Seq[Any], parameters: Seq[Any], parents: Seq[Tree],
                                    self: Option[(Name, Tree)], statements: Seq[Tree]): Unit = tagged(TEMPLATE) {
    // TODO {type,}parameters

    // TODO should be "writeSequence" but dotty has "very special" reader, which handles this particular case...
    writeTerminalSequence(parents)
    self.foreach {
      case (name, tp) =>
        writeName(name)
        write(tp)
    }
    writeTerminalSequence(statements)
  }

  protected def writeDefDef(name: Name, typeParameters: Seq[Any], curriedParams: Seq[Seq[Tree]],
                            returnType: Tree, body: Option[Tree], modifiers: Seq[Modifier]): Unit = tagged(DEFDEF) {
    writeName(name)
    // TODO type parameters
    curriedParams.foreach { parameters =>
      tagged(PARAMS) {
        writeTerminalSequence(parameters)
      }
    }
    write(returnType)
    body.foreach(write)
    modifierWriter.writeTerminalSequence(modifiers)
  }

  // Terms
  protected final def writeIdent(name: Name, typ: Type): Unit = tagged(IDENT) {
    writeName(name)
    typeWriter.write(typ)
  }

  protected final def writeSelect(name: Name, term: Tree): Unit = tagged(IDENT) {
    writeName(name)
    write(term)
  }

  protected final def writePackageRef(name: Name): Unit = tagged(TERMREFpkg)(writeName(name))

  protected final def writeBlock(expression: Tree, statements: Seq[Tree]): Unit = tagged(BLOCK) {
    write(expression)
    writeTerminalSequence(statements)
  }

  protected final def writeNew(typ: Type): Unit = tagged(NEW) {
    typeWriter.write(typ)
  }

  protected final def writeApply(function: Tree, args: Seq[Tree]): Unit = tagged(APPLY) {
    write(function)
    writeTerminalSequence(args)
  }

  protected final def writeTypeApply(function: Tree, args: Seq[Tree]): Unit = tagged(TYPEAPPLY) {
    write(function)
    writeTerminalSequence(args)
  }

  protected final def writeSuper(term: Tree, mixin: Option[Tree]): Unit = tagged(SUPER) {
    write(term)
    mixin.foreach(write)
  }
}
