package dotty.tools.languageserver.config;

import java.net.URI;

import com.fasterxml.jackson.annotation.*;

public class SbtPortFile {
  public final URI uri;

  @JsonCreator
  public SbtPortFile(
      @JsonProperty("uri") URI uri) {
     this.uri = uri;
  }
}
