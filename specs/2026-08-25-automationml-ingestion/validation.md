# Validation — Phase 19

## Automated
- `mvn test` passes (full suite stays green).
- Uploading `.aml`/`.xml` CAEX AutomationML is detected as `AUTOMATION_ML`, parsed, and entities
  appear in the Mapping Hub canonical model.
- Generic AutomationML NOT misclassified as MTP AML.
- Invalid input fails with clear error.

## Manual
- In `MappingHubView`, drop an AutomationML (CAEX) file; entities appear in the workbench.

## Definition of done
- AutomationML (CAEX) upload supported end-to-end with tests.
