package community.flock.wirespec.generated.model;

import community.flock.wirespec.java.Wirespec;

public enum PetStatus implements Wirespec.Enum {
  available("available"),
  pending("pending"),
  sold("sold");
  public final String label;
  PetStatus(String label) {
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
