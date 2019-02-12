package org.infinispan.tutorial.embedded;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;

public class RandomWeatherService implements Serializable, WeatherService
{
    private static final long serialVersionUID = 1L;
    
    private static final Random random = new Random();

    public RandomWeatherService() {}

    @Override
    public LocationWeather getWeatherForLocation(String location)
    {
        // try { TimeUnit.MILLISECONDS.sleep(25); }
        // catch (InterruptedException e) {}
        return new LocationWeather(random.nextDouble() * 20.0 + 0.5,
            random.nextDouble() * 0.5 + 0.40, "sunny", location);
    }
}
