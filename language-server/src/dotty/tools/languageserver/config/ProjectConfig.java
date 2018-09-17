package dotty.tools.languageserver.config;

import java.io.File;

import com.fasterxml.jackson.annotation.*;

public class ProjectConfig {
  public final String id;
  public final boolean hasTests;
  public final String compilerVersion;
  public final String[] compilerArguments;
  public final File[] sourceDirectories;
  public final File[] dependencyClasspath;
  public final File classDirectory;

  @JsonCreator
  public ProjectConfig(
      @JsonProperty("id") String id,
      @JsonProperty("hasTests") boolean hasTests,
      @JsonProperty("compilerVersion") String compilerVersion,
      @JsonProperty("compilerArguments") String[] compilerArguments,
      @JsonProperty("sourceDirectories") File[] sourceDirectories,
      @JsonProperty("dependencyClasspath") File[] dependencyClasspath,
      @JsonProperty("classDirectory") File classDirectory) {
     this.id = id;
     this.hasTests = hasTests;
     this.compilerVersion = compilerVersion;
     this.compilerArguments = compilerArguments;
     this.sourceDirectories = sourceDirectories;
     this.dependencyClasspath = dependencyClasspath;
     this.classDirectory =classDirectory;
  }
}
