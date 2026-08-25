# REQ-002 — Fermentation Skid

**Paper use case B.** A fermentation skid module described with MTP (Module Type Package)
AML.

## Input

- `req002-mtp-fermentation-skid-complete.xml` — full MTP AML for the fermentation skid.
- `req002-mtp-fermentation-skid.xml` — trimmed MTP AML variant.

## Expected result

- Matched service: **`SVC-FermentationSkid`**
- Protocol: PROFINET, SIL-2, HardRealTime.

## How to test

1. Mapping Hub: drag `req002-mtp-fermentation-skid-complete.xml` into the dropzone.
2. REST: `POST /api/v1/requirements/match` with `{"requirementId":"REQ-002"}`.

## Source

`src/test/resources/examples/req002-mtp-fermentation-skid-complete.xml`,
`src/test/resources/examples/req002-mtp-fermentation-skid.xml`
