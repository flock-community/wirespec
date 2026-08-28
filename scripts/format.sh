dir="$(dirname -- "$0")"

./gradlew spotlessApply &&
  ./gradlew formatExamples
