package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record NumberRefinedLowerBound (Double value) implements Wirespec.Refined<Double> {
  @Override
  public String toString() { return value.toString(); }
  public boolean validate() {
    return -1.0 < value;
  }
  @Override
  public Double value() { return value; }
}
