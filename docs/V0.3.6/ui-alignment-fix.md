# V0.3.6 UI alignment correction

This iteration refines the dashboard reproduction from the supplied screenshots.

## Applied changes

- The hero sun is vertically centered and horizontally anchored inside the right visual area of the current-weather card.
- The current temperature block is moved upward as a group while preserving its horizontal placement.
- The date pill remains centered in the hero card.
- The wind gauge value and unit are centered inside the circular gauge, while the direction badge keeps a safe gap from the gauge and gust row.
- The pressure value and unit are lowered inside the semicircular gauge, and the normal-pressure label is widened so that it is readable.
- The hourly forecast strip starts closer to the left edge and supports horizontal scrolling from mouse wheel or touchpad input.
- The UV value is displayed as a number placed next to the segmented scale.
- The UV warning is embedded inside the UV card, with a larger card and a larger warning badge.
- The temperature chart starts after the y-axis labels and now begins with Monday.
- Bottom navigation icons are centered inside fixed icon slots above their labels.
- The sun path card is taller and its bottom aligns with the weekly forecast card.
- The sun-path elapsed arc stops before the sun marker to prevent overshoot.

## Implementation notes

The layout still uses deterministic JavaFX coordinates because the target is a faithful desktop mockup reproduction. Reusable components are kept isolated in `fr.alescis.aelia.ui.components` to avoid leaking visual alignment details into the provider or service layers.
