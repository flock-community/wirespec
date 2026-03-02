package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record IntRefinedLowerAndUpper (Long value) implements Wirespec.Refined<Long> {
  @Override
  public String toString() { return value.toString(); }
  public boolean validate() {
    return 3 < value && value < 4;
  }
  @Override
  public Long value() { return value; }
}
