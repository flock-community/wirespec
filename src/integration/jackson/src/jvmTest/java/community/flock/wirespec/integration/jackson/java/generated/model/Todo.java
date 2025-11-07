package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record Todo (
  TodoId id,
  String name,
  Boolean _final,
  TodoCategory category,
  String eMail
) {
};
