# Phase 1 Feature Spec Review Report

## Summary

The required `requirements.md`, `plan.md`, and `validation.md` artifacts are present in the correct feature folder. The specification is aligned with the Phase 1 roadmap and clearly excludes ontology, SPARQL, semantic matching, Kubernetes generation, and external infrastructure. The smoke endpoint and expanded configuration work are scope additions explicitly approved by the human engineer.

No high-severity architecture or roadmap conflict was found. Three medium-severity gaps should be resolved before Builder implementation and spec validation so dependency resolution, package deliverables, and container-profile behavior are deterministic.

## Findings

### MEDIUM: Dependency versions are not selected reproducibly

References:

- `requirements.md:13-15`
- `plan.md:57-61`
- `validation.md:42-45`

The spec requires Spring Boot 4.0.x, Apache Jena, and SnakeYAML but does not select exact versions or define an approved version-selection rule. A Spring Boot parent version must be concrete, and Apache Jena's aggregate dependency normally requires an explicit version unless separately managed. Allowing the Builder to choose any available 4.0.x/Jena version makes the implementation and later validation environment-dependent and can produce materially different dependency trees.

Required correction:

- Pin the Spring Boot patch version for Phase 1.
- Pin the Apache Jena version or define the exact dependency-management import that supplies it.
- State whether SnakeYAML is supplied through Spring Boot dependency management or explicitly versioned.
- Add validation that records the effective versions, for example through `mvn help:evaluate` or `mvn dependency:tree`.

### MEDIUM: Package skeleton is required but has no realizable artifact definition

References:

- `requirements.md:16`
- `requirements.md:51`
- `plan.md:13`
- `plan.md:69`

The requirements call for package boundaries for `api`, `ontology`, `reasoning`, `deployment`, and `error`, while the plan correctly rejects empty marker classes and only names concrete files in `api`. Git does not preserve empty directories, so the ontology, reasoning, deployment, and error packages cannot be delivered as currently described. The Builder and Validator therefore cannot agree whether the package-structure requirement is complete.

Required correction:

- Either limit Phase 1's implemented packages to `com.sda.mapper` and `com.sda.mapper.api` and document the other packages as future structure, or approve concrete `package-info.java` artifacts for each package.
- Update requirements, plan, and validation to use the same measurable package expectation.

### MEDIUM: The container profile is mandatory but its behavior is undefined

References:

- `requirements.md:27-30`
- `requirements.md:61-63`
- `plan.md:71-75`
- `validation.md:75-78`
- `validation.md:101-104`

The spec requires `application-container.yml` and requires validation that the `container` profile is active, but it does not identify any container-specific property or observable behavior supplied by that profile. This permits an empty or redundant profile file and makes the validation assertion largely equivalent to checking that an environment variable exists, rather than proving profile configuration was applied.

Required correction:

- Define at least one justified container-specific setting and its expected value, or remove the separate profile requirement if no behavior differs from local execution.
- Add an observable validation method that proves the profile setting was applied without exposing sensitive environment data.
- Keep shared settings in `application.yml` and avoid duplicating values solely to make the profile non-empty.

## Spec Gaps Blocking Builder/Validator

The following gaps block an unambiguous Builder and Validator handoff:

1. Exact dependency versions or an approved deterministic version-selection rule.
2. A Git-realizable definition of the required package structure.
3. A concrete purpose and observable effect for `application-container.yml`.

Once these are corrected consistently across all three spec files, the feature is ready for spec validation.

## Open Questions

1. Which exact Spring Boot 4.0.x and Apache Jena versions does the human engineer approve for Phase 1?
2. Should future ownership packages be represented now using `package-info.java`, or created only when their first implementation classes arrive?
3. What container-only behavior should the `container` profile control? A specific operational setting should be selected rather than adding a profile file with no functional difference.

## Residual Risks

- Java 25-compatible builder and runtime image availability must be confirmed during spec validation or implementation validation.
- The Docker health-check command must be compatible with the selected runtime image; the existing requirement identifies this risk but leaves the implementation mechanism appropriately open after the runtime image is selected.
- The smoke endpoint increases Phase 1 scope, but its narrow deterministic contract and explicit exclusion from the Phase 6 API keep the expansion contained.
