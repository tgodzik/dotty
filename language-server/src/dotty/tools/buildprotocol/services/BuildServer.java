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

public interface BuildServer extends LanguageServer {
  @JsonNotification("sbt/exec")
  void sbtExec(SbtExecParams params);

  @JsonRequest("dotty/test")
  CompletableFuture<TestResult> dottyTest(SbtExecParams params);
}
