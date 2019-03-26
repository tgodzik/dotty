package tasty

import dotty.tools.dotc.core.tasty.TastyFormat._
import tasty.binary.BinaryWriter.hashOf
import tasty.binary.{BinaryWriter, SectionWriter}
import tasty.names.ScalacWriterNamePool
import tasty.tree.terms.ScalacTreeWriter

import scala.tools.nsc.Global

final class ScalacTastyWriter(implicit val g: Global) extends TastyWriter {
  override type Name = Global#Name
  private val namesSection = new SectionWriter
  private val namePool = new ScalacWriterNamePool(namesSection)

  private val treeSection = new SectionWriter
  private val treeSectionNameRef = namePool.writeName(g.newTermName("ASTs"))
  private val treePickler = new ScalacTreeWriter(namePool, treeSection)

  def write(tree: Global#Tree): Unit = treePickler.write(tree)

  def output(): Array[Byte] = {
    val totalSize = TastyWriter.headerBytes + namesSection.size + treeSection.size
    val output = new BinaryWriter(totalSize) // TODO can be streamed instead of copying arrays in memory

    writeHeader(output)
    output.write(namesSection)
    output.writeNat(treeSectionNameRef.index)
    output.write(treeSection)

    output.bytes
  }

  private def writeHeader(output: BinaryWriter): Unit = {
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