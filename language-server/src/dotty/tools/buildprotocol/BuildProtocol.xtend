package dotty.tools.buildprotocol

import java.util.List
import org.eclipse.lsp4j.generator.JsonRpcData
import org.eclipse.lsp4j.jsonrpc.validation.NonNull

@JsonRpcData
class SbtExecParams {
  String commandLine

  new() {
  }

  new (String commandLine) {
    this.commandLine = commandLine
  }
}

@JsonRpcData
class BuildTargetIdentifier {
	@NonNull String name

  new() {
  }

  new(@NonNull String name) {
    this.name = name
  }
}

@JsonRpcData
class TestIdentifier {
  @NonNull BuildTargetIdentifier target
	@NonNull String name

  new() {
  }

  new(@NonNull BuildTargetIdentifier target, @NonNull String name) {
    this.target = target
    this.name = name
  }
}

@JsonRpcData
class ListTestsParams {
  @NonNull List<BuildTargetIdentifier> targets

  new() {
  }

  new(@NonNull List<BuildTargetIdentifier> targets) {
    this.targets = targets
  }
}

@JsonRpcData
class ListTestsItem {
  @NonNull TestIdentifier id
  @NonNull List<ListTestsItem> subTests

  new() {
  }

  new(@NonNull TestIdentifier id, @NonNull List<ListTestsItem> subTests) {
    this.id = id
    this.subTests = subTests
  }
}

@JsonRpcData
class ListTestsResults {
  @NonNull List<ListTestsItem> items

  new() {
  }

  new(@NonNull List<ListTestsItem> items) {
    this.items = items
  }
}
@JsonRpcData
class RunTestsParams {
  @NonNull List<TestIdentifier> tests

  new() {
  }

  new(@NonNull List<TestIdentifier> tests) {
    this.tests = tests
  }
}

@JsonRpcData
class RunTestsResults {
  new() {
  }
}
