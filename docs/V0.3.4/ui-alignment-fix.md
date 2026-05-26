# UI alignment fix V0.3.4

This iteration fixes visual alignment issues detected in the Aelia dashboard mockup implementation.

## Fixed elements

- Weather icons used in hourly and daily forecasts are now centered around their logical anchor point.
- The current-weather sun illustration is moved inside the hero card and card content is clipped to rounded bounds.
- Bottom navigation icons are centered in a fixed icon slot so home, map and settings are aligned.
- The wind direction badge is moved below the circular gauge to avoid overlap.
- Gauge values and units are centered horizontally inside their circular cards.

## Implementation notes

The weather icon factory now wraps condition icons into centered groups. This makes icon placement predictable because callers can set `layoutX` and `layoutY` as the visual center of the icon instead of compensating for each shape's local bounds.

The card base class clips children to the card bounds. This prevents decorative vector elements from escaping rounded cards while keeping the layout flat and lightweight.
