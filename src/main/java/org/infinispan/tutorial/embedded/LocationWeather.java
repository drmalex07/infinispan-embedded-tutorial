package org.infinispan.tutorial.embedded;

import java.io.Serializable;

public class LocationWeather implements Serializable 
{
   final Double temperature;
   final Double relativeHumidity;
   final String conditions;
   final String location;

   public LocationWeather(Double temperature, Double relativeHumidity, String conditions, String location) {
      this.temperature = temperature;
      this.relativeHumidity = relativeHumidity;
      this.conditions = conditions;
      this.location = location;
   }

   @Override
   public String toString() {
      return String.format("Location: [%s], Temperature: %.2f°C, Humidity: %.2f%%, Conditions: %s", 
          location, temperature, relativeHumidity * 100.0, conditions);
   }
}
