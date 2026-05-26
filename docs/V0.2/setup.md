# Setup

Run the project locally:

```bash
mvn javafx:run
```

Run tests:

```bash
mvn test
mvn verify
```

Check required tools:

```bash
./scripts/check-environment.sh
```

Import local font archives:

```bash
./scripts/import-local-fonts.sh ../Luciole_webfonts.zip ../Hack-v3.003-ttf.zip
```

Font binaries are not committed by default. The import script copies local archives into JavaFX resources and generates `styles/fonts.css`.
