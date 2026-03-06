package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record StringRefinedRegex (String value) implements Wirespec.Refined<String> {
  @Override
  public String toString() { return value.toString(); }
  public static boolean validate(StringRefinedRegex record) {
    return java.util.regex.Pattern.compile("^[0-9a-f]{8}\\b-[0-9a-f]{4}\\b-[0-9a-f]{4}\\b-[0-9a-f]{4}\\b-[0-9a-f]{12}$").matcher(record.value).find();
  }
  @Override
  public String value() { return value; }
}
