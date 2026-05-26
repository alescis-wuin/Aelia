# V0.3.9 weather and environment API research

## Recommended first provider

Open-Meteo is the best first remote provider for Aelia because it can cover most dashboard cards without an API key during non-commercial development:

- current conditions;
- hourly forecast;
- daily forecast;
- temperature, apparent temperature, humidity, precipitation, pressure, wind, gusts and weather codes;
- UV index;
- sunrise, sunset and daylight duration;
- air quality and common pollutants;
- European pollen indicators.

The project should start with a single `OpenMeteoWeatherDashboardProvider`, then split the implementation into smaller provider clients only when the remote code becomes large enough.

## Secondary sources

### WeatherAPI

WeatherAPI is useful for astronomy, alerts, AQI and paid pollen features. It is not the first recommended source because it requires an API key and several relevant features depend on paid plans.

### OpenWeather One Call

OpenWeather One Call is useful for moon information, government alerts and broad weather coverage. It is a good secondary source for moon phase, moonrise and moonset when Aelia adds an astronomy card.

### MET Norway Locationforecast

MET Norway is a strong fallback for weather forecasts, but it requires explicit client identification, cache-aware usage and attribution. It is better suited to a carefully cached provider than to a direct high-frequency desktop polling approach.

### OpenAQ and WAQI

OpenAQ and WAQI are relevant for station-oriented air-quality data. They should be treated as optional advanced providers because they involve separate quotas, attribution and station-to-location mapping concerns.

### Google Pollen API

Google Pollen API is useful for richer pollen and allergen experiences. It should remain optional because it requires a Google Cloud project, billing and key management.

## Provider design implications

Aelia should model provider capabilities explicitly instead of assuming that every provider can return every card. A future provider result should expose:

- source name;
- source license or attribution label;
- measured-at timestamp;
- freshness status;
- missing-data reason when a card cannot be filled;
- remote quota information when known.

This allows the dashboard to display partial data without hiding the whole screen.
