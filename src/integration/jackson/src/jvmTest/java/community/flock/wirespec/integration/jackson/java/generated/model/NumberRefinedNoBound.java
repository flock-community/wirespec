package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record NumberRefinedNoBound (Double value) implements Wirespec.Refined<Double> {
  @Override
  public String toString() { return value.toString(); }
  public static boolean validate(NumberRefinedNoBound record) {
    return true;
  }
  @Override
  public Double value() { return value; }
}
