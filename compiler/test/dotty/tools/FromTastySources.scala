package dotty.tools

import java.io.File
import scala.io.Source
import org.junit.Test
import org.junit.Assert._

import dotty.tools.ListOfSources.loadList

object FromTastySources {

  // pos tests blacklists

  def posFromTastyBlacklistFile: String = "compiler/test/dotc/pos-from-tasty.blacklist"
  def posDecompilationBlacklistFile: String = "compiler/test/dotc/pos-decompilation.blacklist"
  def posRecompilationBlacklistFile: String = "compiler/test/dotc/pos-recompilation.blacklist"

  def posFromTastyBlacklisted: List[String] = loadList(posFromTastyBlacklistFile)
  def posDecompilationBlacklisted: List[String] = loadList(posDecompilationBlacklistFile)
  def posRecompilationBlacklisted: List[String] = loadList(posRecompilationBlacklistFile)

  // run tests blacklists

  def runFromTastyBlacklistFile: String = "compiler/test/dotc/run-from-tasty.blacklist"
  def runDecompilationBlacklistFile: String = "compiler/test/dotc/run-decompilation.blacklist"
  def runRecompilationBlacklistFile: String = "compiler/test/dotc/run-recompilation.blacklist"

  def runFromTastyBlacklisted: List[String] = loadList(runFromTastyBlacklistFile)
  def runDecompilationBlacklisted: List[String] = loadList(runDecompilationBlacklistFile)
  def runRecompilationBlacklisted: List[String] = loadList(runRecompilationBlacklistFile)
}
