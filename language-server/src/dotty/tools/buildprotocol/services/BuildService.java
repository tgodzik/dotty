package dotty.tools.buildprotocol.services;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonDelegate;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;

import dotty.tools.buildprotocol.*;

public interface BuildService {
  @JsonNotification("sbt/exec")
  void sbtExec(SbtExecParams params);

  @JsonRequest("dotty/compileBuilds")
  CompletableFuture<CompileBuildsResult> compileBuilds(CompileBuildsParams params);

  @JsonRequest("dotty/listTests")
  CompletableFuture<ListTestsResult> listTests(ListTestsParams params);

  @JsonRequest("dotty/runTests")
  CompletableFuture<RunTestsResult> runTests(RunTestsParams params);
}
