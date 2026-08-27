dir="$(dirname -- "$0")"

./gradlew spotlessApply &&
  (cd "$dir"/../examples && ./gradlew format)
