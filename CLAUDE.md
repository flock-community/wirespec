# Wirespec - Claude Code Instructions

## Testing

- When emitters change, always run the test updater to regenerate expected test output:
  ```
  ./gradlew :src:compiler:test-updater:run
  ```
