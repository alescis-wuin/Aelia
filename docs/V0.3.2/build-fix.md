# Build fix V0.3.2

This patch makes the UI-heavy dashboard build classpath-based by removing `module-info.java` and using the JavaFX Maven plugin main class `fr.alescis.aelia.AeliaApplication`.

The change avoids IDE module-path configuration issues while preserving Java 21, JavaFX, AtlantaFX and the `fr.alescis.aelia` package.

## Validation

The source tree was checked with a local Java compiler using JavaFX-compatible compile stubs because the execution environment does not provide Maven or the JavaFX runtime artifacts.
