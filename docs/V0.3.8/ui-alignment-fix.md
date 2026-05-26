# V0.3.8 UI alignment correction

This iteration refines two remaining dashboard alignment issues observed in the current rendering screenshots.

## Changes

- The bottom navigation icons are now centered by their JavaFX layout parent instead of being rendered as unmanaged nodes.
- The bottom navigation icon slots have been moved slightly downward so every icon sits directly above its label.
- The current weather hero no longer duplicates the city in the upper-left technical label.
- The selected city is now displayed inside the centered date badge using a clear separator.
- The weather condition label is placed below the decorative sun icon.
- The temperature block containing the current, maximum and minimum values is grouped and vertically centered more consistently within the hero card.

## Design rationale

The previous layout mixed absolute positioning with unmanaged vector wrappers. That made icons look visually centered in code while being rendered from the upper-left corner when placed inside layout containers. The new approach keeps centered wrappers managed so `StackPane` can align them correctly.

The hero card now has a clearer information hierarchy: date and city at the top, temperature as the left focal block, and the condition attached to the weather icon on the right.
