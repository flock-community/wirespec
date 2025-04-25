package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public enum FindPetsByStatusParameterStatus implements Wirespec.Enum {
  available("available"),
  pending("pending"),
  sold("sold");
  public final String label;
  FindPetsByStatusParameterStatus(String label) {
    this.label = label;
  }
  @Override
  public String toString() {
    return label;
  }
  @Override
  public String getLabel() {
    return label;
  }
}
