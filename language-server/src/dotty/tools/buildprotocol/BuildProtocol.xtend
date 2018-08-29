package dotty.tools.buildprotocol

import org.eclipse.lsp4j.generator.JsonRpcData

@JsonRpcData
class SbtExecParams {
  String commandLine

  new() {
  }

  new (String commandLine) {
    this.commandLine = commandLine
  }
}
