package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record NumberRefinedUpperBound (Double value) implements Wirespec.Refined<Double> {
  @Override
  public String toString() { return value.toString(); }
  public static boolean validate(NumberRefinedUpperBound record) {
    return record.value < 2.0;
  }
  @Override
  public Double getValue() { return value; }
}
