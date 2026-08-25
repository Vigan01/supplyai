# Contributing

Thanks for improving SupplyAI. Keep changes small, tested, and aligned with the package-by-feature structure described in the README.

## Development workflow

1. Create a branch from `main`.
2. Make the smallest change that solves the problem.
3. Run the test suite:

```bash
./mvnw test
```

4. Open a pull request and fill out the checklist.

## Code standards

- Use Java 21 and Spring Boot conventions.
- Keep user-facing application text in German.
- Keep source code, package names, commits, and technical documentation in English.
- Place new business capabilities in their own package under `com.supplyai`.
- Do not commit secrets, generated build output, IDE state, or local database files.

## Commit style

Use clear imperative commit messages, for example:

```text
Add inventory threshold validation
```
