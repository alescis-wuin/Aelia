# Design system

## Principles

- Flat dark interface.
- No gradients, shadows, blur or heavy animations.
- Strong contrast and explicit focus states.
- Bright but coherent accents: cyan, yellow, lime, orange and red.
- Rounded cards and rows to avoid sharp corners.
- Minimal visible text; details are moved to tooltips and accessibility metadata.

## Typography

- General UI text: Luciole when imported, then system fallbacks.
- Numeric values and compact data: Hack when imported, then monospace fallbacks.
- The largest value is the current temperature in the hero card.

## Color roles

| Role | Color |
| --- | --- |
| Application background | `#070B14` |
| Sidebar | `#0B1220` |
| Cards | `#0E1724` |
| Hero card | `#101B31` |
| Main accent | `#00E5FF` |
| Cold/minimum accent | `#00B4D8` |
| Sun/maximum accent | `#FFD600` |
| Good/low-risk accent | `#A8FF3E` |
| Warning accent | `#FF6D00` |

The palette avoids low-opacity text for important content. Decorative or repeated labels may be visually quieter, but they remain readable on the dark background.
