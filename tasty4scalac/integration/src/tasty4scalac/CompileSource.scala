package tasty4scalac

import java.nio.file.Path

final class CompileSource(val name: String, val source: Path) {
  override def toString: String = name
}
