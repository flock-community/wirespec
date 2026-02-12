package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

import java.util.regex.Pattern;

public record TodoId (String value) implements Wirespec.Refined<String> {
  @Override
  public String toString() { return value.toString(); }
  public static boolean validate(TodoId record) {
    return Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$").matcher(record.value).find();
  }
  @Override
  public String getValue() { return value; }
}
