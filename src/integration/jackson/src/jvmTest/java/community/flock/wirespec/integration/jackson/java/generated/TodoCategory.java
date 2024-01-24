package community.flock.wirespec.integration.jackson.java.generated;

import community.flock.wirespec.Wirespec;

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
}
