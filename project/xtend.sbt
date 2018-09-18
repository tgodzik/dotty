libraryDependencies += "org.eclipse.xtend" % "org.eclipse.xtend.core" % "2.14.0"

dependencyOverrides ++= Seq(
  // Workaround https://github.com/eclipse/xtext/issues/1231
  "org.eclipse.platform" % "org.eclipse.equinox.common" % "3.10.0",
  "org.eclipse.platform" % "org.eclipse.equinox.app" % "1.3.500",
  "org.eclipse.emf" % "org.eclipse.emf.common" % "2.12.0",
  "org.eclipse.emf" % "org.eclipse.emf.ecore" % "2.12.0",
  "org.eclipse.emf" % "org.eclipse.emf.ecore.xmi" % "2.12.0",

  // ...and lock all the other transitive dependencies with version bounds to
  // stop sbt from spending a really long time trying to resolve them
  "com.google.guava" % "guava" % "21.0",
  "org.eclipse.jdt" % "org.eclipse.jdt.compiler.apt" % "1.3.110",
  "org.eclipse.jdt" % "org.eclipse.jdt.compiler.tool" % "1.2.101",
  "org.eclipse.jdt" % "org.eclipse.jdt.core" % "3.13.102",
  "org.eclipse.platform" % "org.eclipse.core.commands" % "3.9.100",
  "org.eclipse.platform" % "org.eclipse.core.contenttype" % "3.7.0",
  "org.eclipse.platform" % "org.eclipse.core.expressions" % "3.6.100",
  "org.eclipse.platform" % "org.eclipse.core.filesystem" % "1.7.100",
  "org.eclipse.platform" % "org.eclipse.core.jobs" % "3.10.0",
  "org.eclipse.platform" % "org.eclipse.core.resources" % "3.13.0",
  "org.eclipse.platform" % "org.eclipse.core.runtime" % "3.14.0",
  "org.eclipse.platform" % "org.eclipse.equinox.app" % "1.3.500",
  "org.eclipse.platform" % "org.eclipse.equinox.common" % "3.10.0",
  "org.eclipse.platform" % "org.eclipse.equinox.preferences" % "3.7.100",
  "org.eclipse.platform" % "org.eclipse.equinox.registry" % "3.8.0",
  "org.eclipse.platform" % "org.eclipse.osgi" % "3.13.0",
  "org.eclipse.platform" % "org.eclipse.text" % "3.6.300",
  // "org.eclipse.xtend" % "org.eclipse.xtend.core" % "2.14.0",
  // "org.eclipse.xtend" % "org.eclipse.xtend.lib" % "2.14.0",
  // "org.eclipse.xtend" % "org.eclipse.xtend.lib.macro" % "2.14.0",
  // "org.eclipse.xtext" % "org.eclipse.xtext" % "2.14.0",
  // "org.eclipse.xtext" % "org.eclipse.xtext.common.types" % "2.14.0",
  // "org.eclipse.xtext" % "org.eclipse.xtext.util" % "2.14.0",
  // "org.eclipse.xtext" % "org.eclipse.xtext.xbase" % "2.14.0",
  // "org.eclipse.xtext" % "org.eclipse.xtext.xbase.lib" % "2.14.0",
)
