# Implementation plan

Version 0.3 converts the previous technical dashboard into a visual weather dashboard close to the supplied desktop mockup.

## Completed steps

1. Keep the existing Java 21, JavaFX, Maven and AtlantaFX baseline.
2. Preserve the provider port so local simulation and future remote APIs can share the same contract.
3. Add a deterministic dashboard snapshot matching the Paris mockup values.
4. Replace the dense technical view with a full fixed-ratio 1374 x 854 dashboard.
5. Create reusable card components for the sidebar, hero card, hourly strip, chart, forecast table and environmental cards.
6. Replace gradients by flat colors and segmented scales.
7. Add focusable custom components, tooltips and JavaFX accessibility metadata.
8. Add optional font import support for Luciole and Hack without bundling font binaries.
9. Document the UI composition and setup in versioned documentation.

## Next steps

1. Connect the static snapshot to live simulated variations.
2. Add a city search backing model.
3. Add a detail drawer for UV, air quality and pollen explanations.
4. Add a real remote provider adapter after the UI is validated.
