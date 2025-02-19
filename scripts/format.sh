dir="$(dirname -- "$0")"

./gradlew spotlessApply &&
  (cd "$dir"/../examples && make format)
