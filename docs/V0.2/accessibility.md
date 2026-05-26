# Accessibility

Aelia uses native JavaFX controls first so that keyboard traversal and focus behavior remain predictable.

Implemented accessibility choices:

- high-contrast dark palette;
- visible yellow focus outline;
- no gradient, shadow, blur or motion-heavy effect;
- clear labels associated with input controls;
- concise tooltips for action controls;
- JavaFX accessibility metadata through `accessibleText`, `accessibleRole` and table roles;
- mnemonic buttons and global keyboard shortcuts.

JavaFX is not a web runtime, so ARIA attributes are not available directly. The equivalent implementation point is the JavaFX accessibility API on `Node`, especially `accessibleText`, `accessibleHelp` and `accessibleRole`.

Manual checks recommended for each UI iteration:

1. navigate the whole screen using only the keyboard;
2. verify that the focused element is always visible;
3. verify that text remains legible at increased OS scale;
4. verify that color is never the only source of information;
5. run a screen reader smoke test on the target operating system.
