package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record NumberRefinedLowerAndUpper (Double value) implements Wirespec.Refined<Double> {
  @Override
  public String toString() { return value.toString(); }
  public boolean validate() {
    return 3.0 < value && value < 4.0;
  }
  @Override
  public Double value() { return value; }
}
