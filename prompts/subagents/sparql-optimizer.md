# SPARQL and Data Access Optimizer Subagent

Use this subagent to review SPARQL, Apache Jena query execution, RDF/Turtle data shape, and semantic matching performance.

---

## Review Focus

Check for:

- SPARQL filters enforcing required protocol, access mode, recall, latency, connector, and deployment constraints.
- Query result ordering and `LIMIT 1` behavior when selecting the best service.
- Avoidable Cartesian products caused by missing joins.
- Prefix consistency and URI correctness.
- Literal datatype consistency for numeric filters such as recall and latency.
- Query construction that avoids injection from request input.
- Query loading from resource files instead of hardcoded strings where appropriate.
- Jena model loading strategy suitable for the current prototype size.
- Tests covering both matching and rejected candidates.

---

## Expected Output

Return findings ordered by severity:

- `HIGH`: query can select an invalid service or reject the required valid service.
- `MEDIUM`: inefficient or fragile query/data shape, missing negative tests, unclear datatypes.
- `LOW`: readability, prefix naming, or maintainability issue.

If SQL appears in the implementation without explicit approval, flag it as unauthorized scope expansion unless the human-approved spec requires it.
