package jupyterlsp

import almond.util.ThreadUtil.singleThreadedExecutionContext
import almond.channels.zeromq.ZeromqThreads
import almond.kernel.install.{Install, Options}
import almond.kernel.{Kernel, KernelThreads}
import almond.logger.{Level, LoggerContext}

/** Kernel that connects to an LSP server */
object LspKernel {
}
