package dotty.tools.buildprotocol

import java.util.List
import java.util.ArrayList
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
class BuildIdentifier {
  @NonNull public static BuildIdentifier root = new BuildIdentifier("", true)

	@NonNull String name
	@NonNull Boolean hasTests

  new() {
  }

  new(@NonNull String name, @NonNull Boolean hasTests) {
    this.name = name
    this.hasTests = hasTests
  }
}

@JsonRpcData
class CompileBuildsParams {
  @NonNull List<BuildIdentifier> builds

  new() {
  }

  new(@NonNull List<BuildIdentifier> builds) {
    this.builds = builds
  }
}

@JsonRpcData
class CompileBuildsResult {
  @NonNull Boolean compilationSucceeded

  new() {
  }

  new(@NonNull Boolean compilationSucceeded) {
    this.compilationSucceeded = compilationSucceeded
  }
}

@JsonRpcData
class TestIdentifier {
  @NonNull public static TestIdentifier root =
    new TestIdentifier(BuildIdentifier.root, new ArrayList(), true)

  @NonNull BuildIdentifier build
	@NonNull List<String> path
	@NonNull Boolean hasChildrenTests

  new() {
  }

  new(@NonNull BuildIdentifier build, @NonNull List<String> path,
      @NonNull Boolean hasChildrenTests) {
    this.build = build
    this.path = path
    this.hasChildrenTests = hasChildrenTests
  }
}

@JsonRpcData
class ListTestsParams {
  @NonNull List<TestIdentifier> parents

  new() {
  }

  new(@NonNull List<TestIdentifier> parents) {
    this.parents = parents
  }
}

@JsonRpcData
class ListTestsResult {
  @NonNull List<TestIdentifier> tests

  new() {
  }

  new(@NonNull List<TestIdentifier> tests) {
    this.tests = tests
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
class RunTestsResult {
  new() {
  }
}

@JsonRpcData
class TestStatus {
  @NonNull TestIdentifier id
  @NonNull TestStatusKind kind
  @NonNull String details

  new() {
  }

  new(@NonNull TestIdentifier id, @NonNull TestStatusKind kind, @NonNull String details) {
    this.id = id
    this.kind = kind
    this.details = details
  }
}

