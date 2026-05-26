# UI and UX iteration

The first screen is organized around one dominant value, one condition badge and four secondary indicators. Detailed supported-data and provider-limit tables are moved to a separate `Reference` tab to keep the default `Live` tab calm and readable.

Design constraints:

- dark flat palette;
- high-contrast text and controls;
- no gradients, no shadows and no blur;
- rounded borders instead of sharp containers;
- native JavaFX controls for predictable keyboard navigation;
- tooltips for all action controls;
- JavaFX accessibility metadata through `accessibleText`, `accessibleRole` and `Label#setLabelFor`;
- Luciole for general text and Hack for numeric/data text when installed or imported locally.

Keyboard shortcuts:

- `Ctrl+L`: focus the metric selector;
- `Ctrl+R`: request the selected current value;
- `Ctrl+S`: subscribe to the selected metric;
- `Delete`: stop the selected stream.

The palette targets WCAG AA contrast for normal text by using very dark surfaces with light foreground colors. Accent colors are used as borders, badges and call-to-action fills, not as the only carrier of information.
