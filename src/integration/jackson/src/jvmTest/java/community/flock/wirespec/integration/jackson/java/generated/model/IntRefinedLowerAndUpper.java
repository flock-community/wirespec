package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record IntRefinedLowerAndUpper (Long value) implements Wirespec.Refined<Long> {
  @Override
  public String toString() { return value.toString(); }
  public static boolean validate(IntRefinedLowerAndUpper record) {
    return 3 < record.value && record.value < 4;
  }
  @Override
  public Long getValue() { return value; }
}
