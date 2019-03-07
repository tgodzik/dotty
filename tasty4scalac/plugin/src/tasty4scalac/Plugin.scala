package tasty4scalac

import dotty.tools.dotc.core.tasty.TastyPrinter

import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.backend.jvm.PostProcessorFrontendAccess
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}

class Plugin(val global: Global) extends NscPlugin {
  self =>
  val name = "tasty"
  val description = "Pickles Scala trees (tasty format)."
  val components = List[NscPluginComponent](TastyComponent)

  def changePhasesOrder(runsAfterPhase: String, phaseClass: Class[_], fieldToModify: Object) = {
    val newRunsAfter = List(runsAfterPhase)
    val runsAfterField = phaseClass.getDeclaredField("runsAfter")
    runsAfterField.setAccessible(true)
    runsAfterField.set(fieldToModify, newRunsAfter)
  }

  //update the order of phases
  //typer, superaccessors
  changePhasesOrder("typer", classOf[scala.tools.nsc.Global$superAccessors$], global.superAccessors)

  //superaccessors, patmat
  changePhasesOrder("superaccessors", classOf[scala.tools.nsc.Global$patmat$], global.patmat)

  //patmat, extensionMethods
  changePhasesOrder("patmat", classOf[scala.tools.nsc.Global$extensionMethods$], global.extensionMethods)


  object TastyComponent extends {
    val global: self.global.type = self.global
  } with NscPluginComponent {
    override val runsAfter = List("superaccessors")
    override val runsRightAfter = Some("superaccessors")
    override val phaseName = "tasty"
    override def description = "pickle tasty trees"

    private val frontendAccess = new PostProcessorFrontendAccess.PostProcessorFrontendAccessImpl(global)

    import global._

    override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit): Unit = {
        val tree = unit.body
        assert(!unit.isJava)

        val pickler = new ScalacTastyPickler(global)
        val treePkl = pickler.treePkl
        treePkl.pickle(List(tree.asInstanceOf[treePkl.g.Tree]))

        treePkl.compactify()
        writeTasty(unit, pickler)
      }
    }

    private def writeTasty(unit: CompilationUnit, pickler: ScalacTastyPickler): Unit = {
      val pickled = pickler.assembleParts()
      val outputDirectory = frontendAccess.compilerSettings.outputDirectory(unit.source.file)
      val tastyFile = outputDirectory.fileNamed(unit.source.file.name + ".tasty")
      val out = tastyFile.output
      out.write(pickled)
      out.close()
    }
  }
}
