# Setup V0.1

## Local execution

```bash
mvn javafx:run
```

## Tests

```bash
mvn test
```

## Verification

```bash
mvn verify
```

## Environment check

```bash
./scripts/check-environment.sh
```

## Fonts

The stylesheet requests Luciole for general text and Hack for data-oriented text. The project remains functional when these fonts are not installed because the CSS defines system fallbacks.

Recommended local font installation paths depend on the operating system and desktop environment. The repository does not bundle font binaries.
