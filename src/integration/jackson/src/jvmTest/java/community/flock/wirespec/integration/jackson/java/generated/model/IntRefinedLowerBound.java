package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record IntRefinedLowerBound (Long value) implements Wirespec.Refined<Long> {
  @Override
  public String toString() { return value.toString(); }
  public boolean validate() {
    return -1 < value;
  }
  @Override
  public Long value() { return value; }
}
