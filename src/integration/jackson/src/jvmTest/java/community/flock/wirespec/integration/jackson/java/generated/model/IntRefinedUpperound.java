package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record IntRefinedUpperound (Long value) implements Wirespec.Refined<Long> {
  @Override
  public String toString() { return value.toString(); }
  public static boolean validate(IntRefinedUpperound record) {
    return record.value < 2;
  }
  @Override
  public Long getValue() { return value; }
}
