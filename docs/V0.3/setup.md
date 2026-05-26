# Setup

## Run

```bash
mvn javafx:run
```

## Test

```bash
mvn test
```

## Verify

```bash
mvn verify
```

## Import local fonts

```bash
make import-fonts \
  LUCIOLE_ZIP=/path/to/Luciole_webfonts.zip \
  HACK_ZIP=/path/to/Hack-v3.003-ttf.zip
```

Font binaries are intentionally not committed. They are loaded only when present locally.
