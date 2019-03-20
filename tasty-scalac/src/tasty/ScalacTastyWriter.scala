package tasty

import dotty.tools.dotc.core.tasty.TastyFormat._
import tasty.binary.BinaryOutput
import tasty.binary.BinaryOutput.hashOf
import tasty.names.ScalacNamePool
import tasty.tree.ScalacTreePickler

import scala.tools.nsc.Global

final class ScalacTastyWriter(implicit val g: Global) extends TastyWriter {
  override type Name = Global#Name
  private val namesSection = new BinaryOutput()
  private val namePool = new ScalacNamePool(namesSection)

  private val treeSection = new BinaryOutput
  private val treeSectionName = g.newTermName("ASTs")
  private val treePickler = new ScalacTreePickler(namePool, treeSection)

  def write(tree: Global#Tree): Unit = treePickler.pickleTree(tree)

  def output(): Array[Byte] = {
    val totalSize = TastyWriter.headerBytes + namesSection.size // + sections.values.map(_.size).sum
    val output = new BinaryOutput(totalSize) // TODO can be streamed instead of copying arrays in memory

    writeHeader(output)
    output.write(namesSection)

    output.bytes
  }

  private def writeHeader(output: BinaryOutput): Unit = {
    val namesHash = hashOf(namesSection)
    val treeHash = hashOf(treeSection)
    val uuidLow: Long = namesHash ^ treeHash
    val uuidHi = (Map() - treeSectionName).values.foldLeft(0L)(_ ^ hashOf(_))

    for (ch <- header) output.writeByte(ch.toByte)
    output.writeNat(MajorVersion)
    output.writeNat(MinorVersion)
    output.writeUncompressedLong(uuidLow)
    output.writeUncompressedLong(uuidHi)
  }

}