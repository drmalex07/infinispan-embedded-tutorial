package org.infinispan.tutorial.embedded;

import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.stats.CacheContainerStats;

import static java.util.stream.Collectors.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.stream.Collectors.joining;

/**
 * The demo application.
 * <p>
 * Usually we need to provide some jgroups-related properties (in order for the cluster to form in our local
 * network). For example, say we sit on node <tt>10.0.4.51</tt>:
 * <pre> 
 * java -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=10.0.4.51 org.infinispan.tutorial.embedded.WeatherApp
 * </pre>
 */
public class WeatherApp {

    private static final int INITIAL_CLUSTER_SIZE = 1; // usually >= 2
    
    private static Logger logger = Logger.getLogger(WeatherApp.class.getName()); 
    
    private static void info(String msgTemplate, Object ...parameters)
    {
        logger.log(Level.INFO, msgTemplate, parameters);
    }
    
    private static void warn(String msgTemplate, Object ...parameters)
    {
        logger.log(Level.WARNING, msgTemplate, parameters);
    }
    
    private static void debug(String msgTemplate, Object ...parameters)
    {
        logger.log(Level.FINE, msgTemplate, parameters);
    }
    
    private static void error(String msgTemplate, Object ...parameters)
    {
        logger.log(Level.SEVERE, msgTemplate, parameters);
    }

   private final EmbeddedCacheManager cacheManager;
   
   private final WeatherService weatherService;
   private Cache<String, LocationWeather> cache;
   private final SimpleClusterListener listener;
   
    public WeatherApp() throws InterruptedException
    {
        GlobalConfiguration globalConfig = GlobalConfigurationBuilder.defaultClusteredBuilder()
            .transport()
                .initialClusterSize(INITIAL_CLUSTER_SIZE)
                .initialClusterTimeout(15, TimeUnit.SECONDS)
            .clusterName("WeatherApp")
            .build();

        cacheManager = new DefaultCacheManager(globalConfig);
        listener = new SimpleClusterListener();
        cacheManager.addListener(listener);
        
        Configuration cacheConfig = new ConfigurationBuilder()
            .expiration()
                .lifespan(15, TimeUnit.MINUTES)
            .clustering()
            .cacheMode(CacheMode.DIST_SYNC)
            .hash()
                .numOwners(2)
                .numSegments(128)
            .build();
        cacheManager.defineConfiguration("weather", cacheConfig);
        cache = cacheManager.getCache("weather");
        weatherService = createWeatherService(cache);
        
        // Fixme: Can we do this via configuration?
        cacheManager.getStats().setStatisticsEnabled(true);
    }

    private WeatherService createWeatherService(Cache<String, LocationWeather> cache)
    {
        String apiKey = System.getenv("OWMAPIKEY");
        if (apiKey == null) {
            warn("OWMAPIKEY environment variable not set, using the RandomWeatherService.");
            return new RandomWeatherService(cache);
        } else {
            return new OpenWeatherMapService(apiKey, cache);
        }
    }

    public void shutdown()
    {
        cacheManager.stop();
    }

    private enum Command {
        HELP,
        QUIT,
        SEED,
        GET,
        STATS;
    };
            
    private boolean handleCommand(String commandLine) throws IOException
    {
        boolean done = false;
        try (Scanner scanner = new Scanner(commandLine)) {
            String commandName = scanner.next();
            if (commandName.isEmpty())
                throw new IllegalStateException("Expected a non empty command");
            Command command = null;
            try {
                command = Command.valueOf(commandName.toUpperCase());
            } catch (IllegalArgumentException x) {}
            if (command == null) {
                warn("The command `{0}` is unknown", commandName);
            } else {
                switch (command) {
                case QUIT:
                    done = true;
                    break;
                case HELP:
                    System.out.printf("Available commands: %s%n", 
                        Arrays.stream(Command.values()).collect(mapping(Object::toString, joining(", "))));
                    break;
                case SEED:
                    String inputPath = scanner.next();
                    seedFromFile(Paths.get(inputPath));
                    break;
                case STATS:
                    printStats();
                    break;
                case GET:
                default:
                    String location = scanner.next();
                    info("Fetching weather info for: {0}", location);
                    LocationWeather weather = weatherService.getWeatherForLocation(location);
                    System.out.printf("%s: %s%n", location, weather);
                    break;
                }
            }
        }
        
        return done;
    }
   
    private void printStats()
    {
        CacheContainerStats stats = cacheManager.getStats();
       
        System.out.printf(
            " -- CLUSTER [%s] --%n" +
            "  cluster: %s%n" +
            "  node-name: %s%n" +
            "  node-status: %s%n" +
            "  coordinator: %s%n" +
            "  stats.hit-ratio: %.2f%n" +
            "  stats.number-of-entries: %d%n" +
            "  stats.total-number-of-entries: %d%n" +
            "  number-of-entries: %d%n" +
            "  number-of-entries-in-node: %d%n" +
            " --%n",
            cacheManager.getClusterName(),
            cacheManager.getMembers(),
            cacheManager.getAddress(),
            cache.getStatus(),
            (cacheManager.isCoordinator() ? "T" : "F"),
            stats.getHitRatio(),
            stats.getCurrentNumberOfEntries(),
            stats.getTotalNumberOfEntries(),
            cache.entrySet().size(),
            cache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP).entrySet().size());
    }
    
    private void seedFromFile(Path inputPath) throws IOException
    {
        try (BufferedReader reader = Files.newBufferedReader(inputPath, Charset.forName("UTF-8"))) {
            long count = 0L;
            String line = null;
            while ((line = reader.readLine()) != null) {
                count++;
                String location = line.trim();
                LocationWeather weather = weatherService.getWeatherForLocation(location);
                if (count % 1000 == 0) {
                    debug("Seeding... added {0} entries, now at {1}", count, weather.country);
                }
            }
            info("Seeded {0} entries from {1}", count, inputPath);
        }
    }

    public static void main(String[] args) throws Exception
    {
        final WeatherApp app = new WeatherApp();
        
        info("Started as [{0}]; status={1}", 
            app.cacheManager.getAddress(), app.cacheManager.getStatus());
        
        try (Scanner scanner = new Scanner(System.in)) {
            // Enter a REPL loop: read a user's command and handle it
            boolean done = false;
            while (!done) {
                System.out.println();
                System.out.printf("%s> ", app.cacheManager.getAddress());
                String commandLine = scanner.nextLine();
                if (commandLine.isEmpty())
                    continue;
                done = app.handleCommand(commandLine);
            }
        }
        
        info("Shutting down...");
        app.shutdown();
    }
}