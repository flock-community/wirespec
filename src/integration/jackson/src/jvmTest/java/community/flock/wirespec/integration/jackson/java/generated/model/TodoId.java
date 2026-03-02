package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record TodoId (String value) implements Wirespec.Refined<String> {
  @Override
  public String toString() { return value.toString(); }
  public boolean validate() {
    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$").matcher(value).find();
  }
  @Override
  public String value() { return value; }
}
