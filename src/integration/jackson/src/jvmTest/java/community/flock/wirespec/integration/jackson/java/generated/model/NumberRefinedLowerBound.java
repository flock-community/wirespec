package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record NumberRefinedLowerBound (Double value) implements Wirespec.Refined<Double> {
  @Override
  public String toString() { return value.toString(); }
  public static boolean validate(NumberRefinedLowerBound record) {
    return -1.0 < record.value;
  }
  @Override
  public Double getValue() { return value; }
}
