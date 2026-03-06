package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public enum TodoCategory implements Wirespec.Enum {
  WORK("WORK"),
  LIFE("LIFE");
  public final String label;
  TodoCategory(String label) {
    this.label = label;
  }
  @Override
  public String toString() {
    return label;
  }
  @Override
  public String label() {
    return label;
  }
}
