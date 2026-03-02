package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record StringRefined (String value) implements Wirespec.Refined<String> {
  @Override
  public String toString() { return value.toString(); }
  public boolean validate() {
    return true;
  }
  @Override
  public String value() { return value; }
}
