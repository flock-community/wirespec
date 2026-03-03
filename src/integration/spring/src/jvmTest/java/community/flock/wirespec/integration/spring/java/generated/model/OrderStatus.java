package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public enum OrderStatus implements Wirespec.Enum {
  placed("placed"),
  approved("approved"),
  delivered("delivered");
  public final String label;
  OrderStatus(String label) {
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
