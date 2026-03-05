package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record NumberRefinedUpperBound (Double value) implements Wirespec.Refined<Double> {
  @Override
  public String toString() { return value.toString(); }
  public boolean validate() {
    return value < 2.0;
  }
  @Override
  public Double value() { return value; }
}
