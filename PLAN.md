# Spring Boot 3/4 & Jackson 2/3 Support Strategy

## Context

Spring Boot 4 has been released with breaking changes, including the upgrade from Jackson 2 to Jackson 3. We need to support both versions for some time without overcomplicating the repository or release strategy.

### Requirements
- Support Spring Boot 3 (Jackson 2)
- Support Spring Boot 4 (Jackson 3)
- Avoid complex release strategies
- Keep the repository maintainable
- Provide clear migration path for users

## Recommended Approach: Separate Integration Artifacts

Create two separate integration modules that users can choose between based on their Spring Boot version.

### Structure

```
src/integration/
├── spring-boot-3/          # Jackson 2, Spring Boot 3
│   └── src/jvmMain/kotlin/...
├── spring-boot-4/          # Jackson 3, Spring Boot 4
│   └── src/jvmMain/kotlin/...
└── spring-common/          # Shared code (if any)
    └── src/jvmMain/kotlin/...
```

### Artifact IDs
- `community.flock.wirespec.integration:spring-boot-3-jvm`
- `community.flock.wirespec.integration:spring-boot-4-jvm`

### Benefits
1. **Clean separation** - No complex build logic or profiles
2. **Explicit versioning** - Users explicitly choose their version
3. **Independent releases** - Both can release with the main project
4. **Easy deprecation** - When ready, just mark spring-boot-3-jvm as deprecated
5. **Simple for users** - Clear dependency choice based on their Spring Boot version

### Trade-offs
- Some code duplication (minimal for integration adapters)
- Two modules to maintain (but simpler than profile-based approaches)

## Implementation Steps

### 1. Rename Current Spring Integration
```bash
# Rename existing spring module to spring-boot-3
mv src/integration/spring src/integration/spring-boot-3
```

Update artifact ID in `build.gradle.kts` or relevant build file:
```kotlin
// In spring-boot-3/build.gradle.kts
artifactId = "spring-boot-3-jvm"
```

### 2. Create Spring Boot 4 Integration
```bash
# Copy spring-boot-3 to spring-boot-4
cp -r src/integration/spring-boot-3 src/integration/spring-boot-4
```

Update artifact ID and dependencies:
```kotlin
// In spring-boot-4/build.gradle.kts
artifactId = "spring-boot-4-jvm"

dependencies {
    // Update to Spring Boot 4 dependencies
    implementation("org.springframework.boot:spring-boot-starter-webmvc:4.0.1")
    // ... other Spring Boot 4 dependencies
}
```

### 3. Extract Shared Code (Optional)
If there's significant shared code, create `spring-common`:
```bash
mkdir -p src/integration/spring-common/src/jvmMain/kotlin
```

Move common utilities, interfaces, and non-Jackson-specific code here.

### 4. Update Code for Jackson 3
In `spring-boot-4` module, update code for Jackson 3 breaking changes:
- Package name changes (e.g., `com.fasterxml.jackson` remains the same, but some classes moved)
- API changes in Jackson 3
- Test with Spring Boot 4.0.1+

### 5. Update BOM
Update the BOM (`src/bom/build.gradle.kts` or similar) to include both artifacts:
```kotlin
api(project(":integration:spring-boot-3-jvm"))
api(project(":integration:spring-boot-4-jvm"))
```

### 6. Update Examples
Update example modules to reference the correct artifact:

**Spring Boot 3 examples** (existing):
```xml
<dependency>
    <groupId>community.flock.wirespec.integration</groupId>
    <artifactId>spring-boot-3-jvm</artifactId>
    <version>${wirespec.version}</version>
</dependency>
```

**Spring Boot 4 examples** (new):
```xml
<dependency>
    <groupId>community.flock.wirespec.integration</groupId>
    <artifactId>spring-boot-4-jvm</artifactId>
    <version>${wirespec.version}</version>
</dependency>
```

### 7. Update Documentation
- Update README to document both integration modules
- Provide migration guide for users upgrading from Spring Boot 3 to 4
- Clarify version compatibility matrix

## Alternative Approaches Considered

### Option 2: Maven Classifiers
Use classifiers to publish multiple variants of the same module:
- `spring-jvm-0.0.0-SNAPSHOT.jar` (Spring Boot 3 - default)
- `spring-jvm-0.0.0-SNAPSHOT-sb4.jar` (Spring Boot 4 - with classifier)

**Rejected because:**
- More complex build setup with profiles
- Harder to test both variants
- Less explicit for users

### Option 3: Version-Agnostic Code
Write code that works with both Jackson 2 and 3, mark dependencies as `provided`.

**Rejected because:**
- Jackson 3 has breaking changes that make this impractical
- Would require extensive compatibility shims
- Maintenance burden would be high

## Migration & Deprecation Strategy

### Phase 1: Support Both (Current)
- Maintain both `spring-boot-3-jvm` and `spring-boot-4-jvm`
- All features available in both

### Phase 2: Encourage Migration (6-12 months)
- Mark `spring-boot-3-jvm` as "maintenance mode" in documentation
- New features only in `spring-boot-4-jvm`
- Continue bug fixes for both

### Phase 3: Deprecate Spring Boot 3 (12-18 months)
- Officially deprecate `spring-boot-3-jvm`
- Add deprecation warnings in documentation
- Set end-of-support date

### Phase 4: Drop Support (18-24 months)
- Remove `spring-boot-3-jvm` module
- Update examples and documentation

## Open Questions

1. **Shared code extraction**: How much code is actually shared between Spring Boot 3 and 4 integrations?
2. **Testing strategy**: Do we need separate CI jobs for each version?
3. **Version numbering**: Should both modules share the same version number, or be independent?
4. **Emitter updates**: Do emitters need to be version-aware, or are they Jackson-agnostic?

## Next Steps

1. Assess the current `spring-jvm` integration code for Jackson 2/3 compatibility issues
2. Identify truly shared code that can be extracted to `spring-common`
3. Implement the module split
4. Update build configuration
5. Update examples and tests
6. Document the changes and migration path
