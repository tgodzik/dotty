import * as vscode from 'vscode'
import * as fs from 'fs'
import * as path from 'path'
import { LanguageClient } from 'vscode-languageclient'

import { saveAllAndCompile } from './extension'
import { Commands } from './commands'
import { ListTestsRequest, RunTestsRequest, TestStatusNotification } from './extensions/protocol'
import { TestIdentifier, TestStatus, ListTestsParams, TestStatusKind } from './extensions/types'

// We use strings for keys instead of `TestIdentifier` itself because
// Javascript doesn't have a useful notion of value equality on objects.
type TestIdentifierHandle = string

// * Done
// - Expand "All tests" and test classes
// - Expanding test class should start test run
//   - Hopefully ok because children are cached
// - Actually use status:
//   - When running, show spinning wheel if possible and "Running..." in tooltip
//   - When finished, show green check mark or red cross mark, and actual error in tooltip
//     - Check what happens with exceptions
// - Implement compile-on-save:
//   - save all files first
//   - configurable with a setting
//   - for now: compile everything
// - Try harder to get updated children
// - Add refresh button
// - success/failure of test class using BSPListener#testEvent
// - Figure out if we can get the output tab to be shown when running tests
//     - vscode.commands.executeCommand("...") ? switchOutput probably
//     ==> actually not useful, because sbt doesn't log the exceptions :/
//   - ... and get the problem tab to be shown when saving
//     - vscode.commands.executeCommand("workbench.action.problems.focus")
// * sbt-plugin
// - hide cyclic warning
// - deal with offline

// * IDE
// - save sbt logs in lsp logs (embed in custom message, maybe telemety or traceEvent ?)
// - Figure out how to start sbt (probably just run sbt from DLS)
// - integrate worksheet mode, see gitter discussion
// - Test on Windows

// * Dotty
// - Scaladoc/Javadoc URL when asking for doc

// * Mooc
// - fork in Test, ...
// - Merge compile and test project
// - Documentation:
//   - Take JDK/sbt page from progfun, cleanup
//   - Write intro to IDE and sbt at the same time
//     - Find recording software
//   - Write intro to worksheet

// Later stuff:
// - Don't discard the DLC output
// - Figure out how to keep alive sbt (and the dls itself too)
// - color sbt messages with syntax extension

export class TestProvider implements vscode.TreeDataProvider<TestIdentifierHandle> {
  readonly testRunningIcon = {
    dark: this.context.asAbsolutePath(path.join('images', 'loading-dark.svg')),
    light: this.context.asAbsolutePath(path.join('images', 'loading.svg'))
  }
  readonly testSuccessIcon = this.context.asAbsolutePath(path.join('images', 'green-check.svg'))
  readonly testFailureIcon = this.context.asAbsolutePath(path.join('images', 'red-cross.svg'))

  private nodesMap: Map<TestIdentifierHandle, TestIdentifier> = new Map()

  private statusMap: Map<TestIdentifierHandle, TestStatus> = new Map()

  private childrenMap: Map<TestIdentifierHandle, Set<TestIdentifierHandle>> = new Map()

	private _onDidChangeTreeData: vscode.EventEmitter<TestIdentifierHandle | undefined> = new vscode.EventEmitter()
	readonly onDidChangeTreeData: vscode.Event<TestIdentifierHandle | undefined> = this._onDidChangeTreeData.event

  private active: boolean = false

	constructor(private client: LanguageClient, private context: vscode.ExtensionContext) {
    vscode.commands.registerCommand(Commands.RUN_ALL_TESTS, () => {
      saveAllAndCompile(client)
        .then(result => {
          if (!result.compilationSucceeded) {
            vscode.window.showErrorMessage("The tests cannot be run because compilation failed.")
            return
          }

          this.nodesMap.clear()
          this.statusMap.clear()
          this.childrenMap.clear()

          this.active = true

          client.sendRequest(RunTestsRequest.type, { tests: [] })
            .then(res => {
              this._onDidChangeTreeData.fire()
            })
        })
    });

    client.onNotification(TestStatusNotification.type, status => {
      if (!this.active)
        return

      console.log("status")
      console.log(status)
      const test = status.id
      const testHandle = this.registerHandle(test)
      this.statusMap.set(testHandle, status)

      const parent = TestIdentifier.parent(test)
      if (parent !== undefined) {
        const parentHandle = this.getHandle(parent)
        const siblings = this.childrenMap.get(parentHandle)
        if (siblings === undefined) {
          this.childrenMap.set(parentHandle, new Set( [ testHandle ] ))
        } else {
          siblings.add(testHandle)
        }
      }
    })
  }

  handleName(test: TestIdentifier): TestIdentifierHandle {
    return TestIdentifier.isRoot(test) ?
      "." :
      TestIdentifier.fullName(test)
  }

  public getHandle(test: TestIdentifier): TestIdentifierHandle {
    const handle = this.handleName(test)

    if (this.nodesMap.get(handle) === undefined) {
      this.nodesMap.set(handle, test)
    }
    return handle
  }

  public getTest(handle: TestIdentifierHandle): TestIdentifier {
    const elem = this.nodesMap.get(handle)
    if (elem === undefined) {
      throw new TypeError()
    } else {
      return elem
    }
  }

  registerHandle(test: TestIdentifier): TestIdentifierHandle {
    const handle = this.handleName(test)
    this.nodesMap.set(handle, test)
    return handle
  }

	getTreeItem(element: TestIdentifierHandle): vscode.TreeItem {
    console.log(`getTreeItem`)
    console.log(element)
    const test = this.getTest(element)

    const state = test.hasChildrenTests ?
      vscode.TreeItemCollapsibleState.Expanded :
      vscode.TreeItemCollapsibleState.None

    if (TestIdentifier.isRoot(test)) {
      return new TestNode(
        "All tests",
        element,
        state
      )
    } else {
      const name = TestIdentifier.name(test)
      const status = this.statusMap.get(element)
      console.log(`status`)
      console.log(status)
      let iconPath: string | { dark: string, light: string } | undefined = undefined
      let tooltip: string = ""
      if (status !== undefined) {
        switch (status.kind) {
          case TestStatusKind.Running:
            iconPath = this.testRunningIcon
            tooltip = `Running ${name}...`
            break
          case TestStatusKind.Success:
            iconPath = this.testSuccessIcon
            tooltip = `Test suceeded: ${name}`
            break
          case TestStatusKind.Failure:
            iconPath = this.testFailureIcon
            tooltip = `Test failed: ${name}`
            break
          case TestStatusKind.Ignored:
            tooltip = `Test ignored: ${name}`
            break
          default:
        }

        if (status.shortDescription !== "") {
          tooltip = `${tooltip}\n\n${status.shortDescription}`
        }
        // TODO: figure out what to do about longDescription, it's too long to
        // fit in a tooltip.
      }

      const x = new TestNode(
        TestIdentifier.name(test),
        element,
        state,
        iconPath,
        tooltip
      )
      console.log("YYY")
      console.log(x)
      console.log("ZZZ")
      return x
    }
  }

  getChildren(element?: TestIdentifierHandle): Thenable<TestIdentifierHandle[]> | TestIdentifierHandle[] {
    console.log(`getChildren`)
    console.log(element)

    if (element === undefined) {
      if (!this.active) {
        return []
      }

      const handle = this.registerHandle(TestIdentifier.root)
      return [ handle ]
    } else {
      const test = this.getTest(element)

      if (!test.hasChildrenTests) {
        return []
      }

      if (TestIdentifier.isRoot(test)) {
        let result: TestIdentifierHandle[] = []
        this.nodesMap.forEach((value, key) => {
          if (key !== element && TestIdentifier.isBuild(value))
            result.push(key)
        })
        return result
      }

      console.log("childrenMap")
      console.log(this.childrenMap)

      const children = this.childrenMap.get(element)
      if (children === undefined) {
        return []
      } else {
        return Array.from(children)
      }
    }
  }

  public getParent(element: TestIdentifierHandle): TestIdentifierHandle | undefined {
    const parent = TestIdentifier.parent(this.getTest(element))
    return parent === undefined ?
      undefined :
      this.getHandle(parent)
  }
}

class TestNode extends vscode.TreeItem {
  public constructor(
    public readonly label: string,
    public readonly id: string,
    public collapsibleState: vscode.TreeItemCollapsibleState,
    public readonly iconPath?: string | { dark: string, light: string },
    public readonly tooltip?: string
  ) {
    super(label, collapsibleState)
  }
}
