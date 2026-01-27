package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record IntRefinedLowerBound (Long value) implements Wirespec.Refined<Long> {
  @Override
  public String toString() { return value.toString(); }
  public static boolean validate(IntRefinedLowerBound record) {
    return -1 < record.value;
  }
  @Override
  public Long getValue() { return value; }
}
