# V0.3.19 multi-API keyed forecast fallbacks

## Purpose

The public Open-Meteo Forecast API and MET Norway Locationforecast can both be unavailable from some networks or during upstream incidents. This iteration adds more optional fallback providers before the Historical Weather API fallback.

## Fallback chain

```text
Open-Meteo Forecast
→ WeatherAPI.com Forecast, optional API key
→ Visual Crossing Timeline Weather API, optional API key
→ OpenWeather Current + 5-day / 3-hour Forecast, optional API key
→ Pirate Weather Forecast, optional API key
→ Weatherbit Current + 7-day Daily Forecast, optional API key
→ MET Norway Locationforecast, disabled by default
→ Open-Meteo Historical Weather API
→ Simulation, handled by application mode and startup policy
```

## Runtime properties

### WeatherAPI.com

```bash
-Daelia.weatherapi.enabled=true
-Daelia.weatherapi.apiKey=YOUR_KEY
```

### Visual Crossing

```bash
-Daelia.visualcrossing.enabled=true
-Daelia.visualcrossing.apiKey=YOUR_KEY
```

### OpenWeather

```bash
-Daelia.openweather.enabled=true
-Daelia.openweather.apiKey=YOUR_KEY
```

### Pirate Weather

```bash
-Daelia.pirateweather.enabled=true
-Daelia.pirateweather.apiKey=YOUR_KEY
```

### Weatherbit

```bash
-Daelia.weatherbit.enabled=true
-Daelia.weatherbit.apiKey=YOUR_KEY
```

### MET Norway

MET Norway is now disabled by default because it was reported unavailable in the current environment. It can still be enabled explicitly:

```bash
-Daelia.metnorway.enabled=true
```

## Notes

- Keyed providers are skipped automatically when their key is missing or their `enabled` property is false.
- OpenWeather uses two free-plan endpoints: current weather and 5-day / 3-hour forecast.
- Weatherbit free plan has only daily forecasts, so hourly cards are approximated from current and daily values.
- Historical Weather API remains a partial fallback and must not be presented as a real future forecast.
- Fallback providers are mapped into the same Open-Meteo-shaped intermediate payload so the dashboard rendering remains unchanged.
