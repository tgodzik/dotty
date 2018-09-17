export interface BuildIdentifier {
	name: string
  hasTests: boolean
}

export namespace BuildIdentifier {
  export function isRoot(build: BuildIdentifier): boolean {
    return build.name === ""
  }
  export const root: BuildIdentifier = {
    name: "",
    hasTests: true
  }
}

export interface ListBuildsParams {
}

export interface ListBuildsResult {
  builds: BuildIdentifier[]
}

export interface TestIdentifier {
  build: BuildIdentifier
	path: string[]
  hasChildrenTests: boolean
}

export namespace TestIdentifier {
  export function ofBuild(build: BuildIdentifier): TestIdentifier {
    return {
      build: build,
      path: [],
      hasChildrenTests: build.hasTests
    }
  }

  export function isBuild(test: TestIdentifier): boolean {
    return test.path.length === 0
  }

  export function isRoot(test: TestIdentifier): boolean {
    return isBuild(test) && BuildIdentifier.isRoot(test.build)
  }

  export const root: TestIdentifier = ofBuild(BuildIdentifier.root)

  export function name(test: TestIdentifier): string {
    return isBuild(test) ?
      test.build.name :
      test.path[test.path.length - 1]
  }

  export function fullName(test: TestIdentifier): string {
    return [ test.build.name ].concat(test.path).join(".")
  }

  export function parent(test: TestIdentifier): TestIdentifier | undefined {
    return isBuild(test) ?
      (isRoot(test) ?
       undefined :
       TestIdentifier.root
      ) :
      {
        build: test.build,
        path: test.path.slice(0, -1),
        hasChildrenTests: true
      }
  }
}

export interface ListTestsParams {
  parents: TestIdentifier[]
}

export interface ListTestsResult {
  tests: TestIdentifier[]
}

export interface RunTestsParams {
  tests: TestIdentifier[]
}

export interface RunTestsResult {
}

export interface TestStatus {
  id: TestIdentifier
  kind: TestStatusKind
  details: String
}

export enum TestStatusKind {
  Ignored = 1,
  Running,
  Success,
  Failure,
	// export const Ignored: 1 = 1
	// export const Running: 2 = 2
	// export const Success: 3 = 3
	// export const Failure: 3 = 3
}

// export type TestStatusKind = 1 | 2 | 3
