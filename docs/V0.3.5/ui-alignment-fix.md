# V0.3.6 UI alignment fix

## Scope

This iteration corrects the visual alignment issues reported after the V0.3.4 dashboard rendering test.

## Changes

- The current weather sun is repositioned inside the hero card and aligned with the right-side visual space.
- The date badge is centered horizontally in the hero card.
- The temperature value and min/max row are moved slightly down to better balance the vertical space.
- The hourly forecast strip now starts near the left edge and uses a horizontal ScrollPane for overflow.
- The UV value is displayed as a bare number next to the segmented UV scale.
- The UV alert is embedded inside the UV card.
- Circular gauge value blocks now use centered vertical text containers.
- The wind direction badge is placed between the gauge and gust row with explicit margins.
- The pressure card no longer truncates the normal-pressure reference.
- The temperature chart starts after the axis labels and now starts with Monday.
- Bottom navigation icons are positioned directly above their text labels.
- The sun-path arc is shortened so that it does not visually overshoot the sun marker.
- The default application window is slightly larger to make the dashboard easier to read.

## Rationale

The main cause of the visual offsets was absolute placement using shape-local coordinates and text baselines. The fix uses explicit center points, fixed value blocks, and layout slots for repeated elements.
