package org.infinispan.tutorial.embedded;

import org.infinispan.Cache;

public class CachingWeatherService implements WeatherService
{
    final private Cache<String, LocationWeather> cache;
    
    final private WeatherService service;
   
    public CachingWeatherService(Cache<String, LocationWeather> cache, WeatherService service)
    {
        this.cache = cache;
        this.service = service;
    }

    @Override
    final public LocationWeather getWeatherForLocation(String location)
    {
        LocationWeather result = cache.get(location);
        if (result == null) {
            result = service.getWeatherForLocation(location);
            cache.put(location, result);
        }
        return result;
    }
}
