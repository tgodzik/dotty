package tasty

import dotty.tools.dotc.core.tasty.TastyFormat._
import tasty.binary.BinaryOutput
import tasty.binary.BinaryOutput.hashOf
import tasty.names.ScalacNamePickler
import tasty.tree.ScalacTreePickler

import scala.tools.nsc.Global

final class ScalacTastyWriter(implicit val g: Global) extends TastyWriter {
  override type Name = Global#Name
  private val namesSection = new BinaryOutput()
  private val namePool = new ScalacNamePickler(namesSection)

  private val treeSection = new BinaryOutput
  private val treeSectionNameRef = namePool.pickleName(g.newTermName("ASTs"))
  private val treePickler = new ScalacTreePickler(namePool, treeSection)

  def write(tree: Global#Tree): Unit = treePickler.pickleTree(tree)

  def output(): Array[Byte] = {
    val totalSize = TastyWriter.headerBytes + namesSection.size + treeSection.size
    val output = new BinaryOutput(totalSize) // TODO can be streamed instead of copying arrays in memory

    writeHeader(output)
    output.write(namesSection)
    output.writeNat(treeSectionNameRef.index)
    output.write(treeSection)

    output.bytes
  }

  private def writeHeader(output: BinaryOutput): Unit = {
    val namesHash = hashOf(namesSection)
    val treeHash = hashOf(treeSection)
    val uuidLow: Long = namesHash ^ treeHash
    val uuidHi = (Map() - treeSectionNameRef).values.foldLeft(0L)(_ ^ hashOf(_))

    for (ch <- header) output.writeByte(ch.toByte)
    output.writeNat(MajorVersion)
    output.writeNat(MinorVersion)
    output.writeUncompressedLong(uuidLow)
    output.writeUncompressedLong(uuidHi)
  }

}