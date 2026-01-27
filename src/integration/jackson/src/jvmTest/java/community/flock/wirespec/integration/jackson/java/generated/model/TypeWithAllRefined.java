package community.flock.wirespec.integration.jackson.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record TypeWithAllRefined (
  StringRefinedRegex stringRefinedRegex,
  StringRefined stringRefined,
  IntRefinedNoBound intRefinedNoBound,
  IntRefinedLowerBound intRefinedLowerBound,
  IntRefinedUpperBound intRefinedUpperBound,
  IntRefinedLowerAndUpper intRefinedLowerAndUpper,
  NumberRefinedNoBound numberRefinedNoBound,
  NumberRefinedLowerBound numberRefinedLowerBound,
  NumberRefinedUpperBound numberRefinedUpperBound,
  NumberRefinedLowerAndUpper numberRefinedLowerAndUpper
) {
};
