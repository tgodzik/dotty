'use strict';

import * as fs from 'fs';
import * as path from 'path';

import * as cpp from 'child-process-promise';

import { commands, ExtensionContext } from 'vscode';
import * as vscode from 'vscode';
import { ExecuteCommandParams, ExecuteCommandRequest, LanguageClient, LanguageClientOptions, ServerOptions } from 'vscode-languageclient';

import { Commands } from './commands'

import { TestProvider } from './testTreeView'

import { ListTestsRequest, RunTestsRequest } from './extensions/protocol'
import { TestIdentifier } from './extensions/types'

let extensionContext: ExtensionContext
let outputChannel: vscode.OutputChannel

function executeDottyLanguageServerCommand(command: string, ...params: any[]) {
  return vscode.commands.executeCommand(Commands.EXECUTE_WORKSPACE_COMMAND, command, ...params)
}

export function activate(context: ExtensionContext) {
  extensionContext = context
  outputChannel = vscode.window.createOutputChannel('Dotty Language Client');

  const sbtArtifact = "org.scala-sbt:sbt-launch:1.2.3"
  const buildSbtFile = `${vscode.workspace.rootPath}/build.sbt`
  const dottyPluginSbtFile = path.join(extensionContext.extensionPath, './out/dotty-plugin.sbt')
  const disableDottyIDEFile = `${vscode.workspace.rootPath}/.dotty-ide-disabled`
  const languageServerArtifactFile = `${vscode.workspace.rootPath}/.dotty-ide-artifact`
  const languageServerDefaultConfigFile = path.join(extensionContext.extensionPath, './out/default-dotty-ide-config')
  const coursierPath = path.join(extensionContext.extensionPath, './out/coursier');

  if (process.env['DLS_DEV_MODE']) {
    const portFile = `${vscode.workspace.rootPath}/.dotty-ide-dev-port`
    fs.readFile(portFile, (err, port) => {
      if (err) {
        outputChannel.append(`Unable to parse ${portFile}`)
        throw err
      }

      run({
        module: context.asAbsolutePath('out/src/passthrough-server.js'),
        args: [ port.toString() ]
      })
    })

  } else {
    // Check whether `.dotty-ide-artifact` exists. If it does, start the language server,
    // otherwise, try propose to start it if there's no build.sbt
    if (fs.existsSync(languageServerArtifactFile)) {
      runLanguageServer(coursierPath, languageServerArtifactFile)
    } else if (!fs.existsSync(disableDottyIDEFile) && !fs.existsSync(buildSbtFile)) {
      vscode.window.showInformationMessage(
          "This looks like an unconfigured Scala project. Would you like to start the Dotty IDE?",
          "Yes", "No"
      ).then(choice => {
        if (choice == "Yes") {
          fs.readFile(languageServerDefaultConfigFile, (err, data) => {
            if (err) throw err
            else {
              const languageServerScalaVersion = data.toString().trim()
              fetchAndConfigure(coursierPath, sbtArtifact, languageServerScalaVersion, dottyPluginSbtFile).then(() => {
                runLanguageServer(coursierPath, languageServerArtifactFile)
              })
            }
          })
        } else {
          fs.appendFile(disableDottyIDEFile, "", _ => {})
        }
      })
    }
  }
}

function runLanguageServer(coursierPath: string, languageServerArtifactFile: string) {
  fs.readFile(languageServerArtifactFile, (err, data) => {
    if (err) throw err
    else {
      const languageServerArtifact = data.toString().trim()
      fetchWithCoursier(coursierPath, languageServerArtifact).then((languageServerClasspath) => {
        run({
          command: "java",
          args: ["-classpath", languageServerClasspath, "dotty.tools.languageserver.Main", "-stdio"]
        })
      })
    }
  })
}

function fetchAndConfigure(coursierPath: string, sbtArtifact: string, languageServerScalaVersion: string, dottyPluginSbtFile: string) {
    return fetchWithCoursier(coursierPath, sbtArtifact).then((sbtClasspath) => {
        return configureIDE(sbtClasspath, languageServerScalaVersion, dottyPluginSbtFile)
    })
}

function fetchWithCoursier(coursierPath: string, artifact: string, extra: string[] = []) {
  return vscode.window.withProgress({
      location: vscode.ProgressLocation.Window,
      title: `Fetching ${ artifact }`
    }, (progress) => {
      const args = [
        "-jar", coursierPath,
        "fetch",
        "-p",
        artifact
      ].concat(extra)
      const coursierPromise = cpp.spawn("java", args)
      const coursierProc = coursierPromise.childProcess

      let classPath = ""

      coursierProc.stdout.on('data', (data: Buffer) => {
        classPath += data.toString().trim()
      })

      coursierProc.on('close', (code: number) => {
        if (code != 0) {
          let msg = `Couldn't fetch '${ artifact }' (exit code ${ code }).`
          outputChannel.append(msg)
          throw new Error(msg)
        }
      })
      return coursierPromise.then(() => { return classPath })
    })
}

function configureIDE(sbtClasspath: string, languageServerScalaVersion: string, dottyPluginSbtFile: string) {
  return vscode.window.withProgress({
    location: vscode.ProgressLocation.Window,
    title: 'Configuring the IDE for Dotty...'
  }, (progress) => {

    // Run sbt to configure the IDE. If the `DottyPlugin` is not present, dynamically load it and
    // eventually run `configureIDE`.
    const sbtPromise =
      cpp.spawn("java", [
        "-classpath", sbtClasspath,
        "xsbt.boot.Boot",
        `--addPluginSbtFile=${dottyPluginSbtFile}`,
        `set every scalaVersion := "${languageServerScalaVersion}"`,
        "configureIDE"
      ])

    const sbtProc = sbtPromise.childProcess
    sbtProc.on('close', (code: number) => {
      if (code != 0) {
        const msg = "Configuring the IDE failed."
        outputChannel.appendLine(msg)
        throw new Error(msg)
      }
    })

      return sbtPromise
  })
}

function run(serverOptions: ServerOptions) {
  const clientOptions: LanguageClientOptions = {
    documentSelector: [
      { language: 'scala', scheme: 'file', pattern: '**/*.scala' },
      { language: 'scala', scheme: 'untitled', pattern: '**/*.scala' }
    ],
    synchronize: {
      configurationSection: 'dotty'
    }
  }

  // outputChannel.dispose()

  const client = new LanguageClient('dotty', 'Dotty Language Server', serverOptions, clientOptions);

  commands.registerCommand(Commands.BSP_RUN_TESTS, (...params) => {
    const id: TestIdentifier = {
      target: { name: params[0] as string },
      name: params[1] as string
    }

    const x = client.sendRequest(RunTestsRequest.type, { tests: [id] })
      // .then(res => {
      //   console.log(res)
      //   res
      // })
    // outputChannel.appendLine(`x: ${x}`)
    return x
  });

  const rootPath = vscode.workspace.rootPath as string;
	const testProvider = new TestProvider(client, rootPath, outputChannel);
  vscode.window.registerTreeDataProvider('dottyTests', testProvider);

  // Push the disposable to the context's subscriptions so that the
  // client can be deactivated on extension deactivation
  extensionContext.subscriptions.push(client.start());
}
