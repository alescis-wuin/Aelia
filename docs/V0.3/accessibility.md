# Accessibility

## Implemented measures

- JavaFX accessible roles and accessible text on custom cards.
- Accessible help text for non-standard visual controls.
- Keyboard focus enabled on selectable custom nodes.
- Explicit yellow focus border for keyboard navigation.
- Tooltips for complex and non-standard components.
- Text is not conveyed through images.
- Status values are conveyed with labels, not only with color.

## JavaFX note

JavaFX desktop applications do not use web ARIA attributes. The equivalent mechanism is provided through JavaFX node accessibility properties such as accessible role, accessible text and accessible help.

## Contrast note

The UI is designed around high contrast colors on a dark background. Red is not used as a small text-only indicator; warning states use orange plus labels and icons.
