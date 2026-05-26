# UI composition

The dashboard uses the mockup coordinate system as the design source of truth.

| Area | Position | Size | JavaFX component |
| --- | ---: | ---: | --- |
| Sidebar | `0,0` | `264 x 854` | `SidebarView` |
| Current weather | `280,16` | `556 x 200` | `CurrentWeatherHero` |
| Hourly forecast | `280,228` | `556 x 94` | `HourlyForecastCard` |
| Temperature trend | `280,334` | `556 x 160` | `TemperatureTrendCard` |
| Seven-day forecast | `280,506` | `556 x 332` | `DailyForecastCard` |
| Humidity | `852,16` | `162 x 164` | `DonutGaugeCard` |
| Wind | `1026,16` | `162 x 164` | `WindCard` |
| Pressure | `1200,16` | `174 x 164` | `PressureCard` |
| UV | `852,192` | `522 x 76` | `UvIndexCard` |
| Air quality | `852,280` | `256 x 254` | `AirQualityCard` |
| Pollens | `1120,280` | `254 x 254` | `PollenCard` |
| Alert | `852,546` | `522 x 52` | `AlertCard` |
| Sun path | `852,610` | `522 x 228` | `SunPathCard` |

The root view is a fixed design canvas scaled by `ScaledDashboardShell`. This keeps the visual proportions stable while still allowing window resizing.
