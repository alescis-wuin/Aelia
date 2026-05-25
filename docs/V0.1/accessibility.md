# Accessibility V0.1

## Implemented choices

- Dark interface with high contrast colors.
- Large but compact typography.
- Direct labels and short helper text.
- JavaFX tooltips on actionable controls.
- JavaFX accessibility text and help metadata on primary controls.
- Keyboard focus is preserved through native JavaFX controls.

## Notes

JavaFX is not an HTML runtime and does not expose ARIA attributes directly. The implementation uses JavaFX accessibility roles, accessible text and accessible help properties instead.
