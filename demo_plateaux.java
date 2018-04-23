import org.kaaproject.kaa.client.DesktopKaaPlatformContext;
import org.kaaproject.kaa.client.Kaa;
import org.kaaproject.kaa.client.KaaClient;
import org.kaaproject.kaa.client.SimpleKaaClientStateListener;
import org.kaaproject.kaa.client.configuration.base.ConfigurationListener;
import org.kaaproject.kaa.client.configuration.base.SimpleConfigurationStorage;
import org.kaaproject.kaa.client.logging.strategies.RecordCountLogUploadStrategy;
import org.kaaproject.kaa.schema.sample.Configuration;
import org.kaaproject.kaa.schema.sample.DataCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


import java.util.Scanner;
import java.util.Iterator;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

/**
 * Class implement functionality for First Kaa application. Application send temperature data
 * from the Kaa endpoint with required configured sampling period
 */
public class demo_plateaux {

    private static final long DEFAULT_START_DELAY = 1000L;

    private static final Logger LOG = LoggerFactory.getLogger(demo_plateaux.class);

    private static KaaClient kaaClient;

    private static ScheduledFuture<?> scheduledFuture;
    private static ScheduledExecutorService scheduledExecutorService;

    public static void main(String[] args) {
        LOG.info(demo_plateaux.class.getSimpleName() + " app starting!");

        scheduledExecutorService = Executors.newScheduledThreadPool(1);

        //Create the Kaa desktop context for the application.
        DesktopKaaPlatformContext desktopKaaPlatformContext = new DesktopKaaPlatformContext();

        /*
         * Create a Kaa client and add a listener which displays the Kaa client
         * configuration as soon as the Kaa client is started.
         */
        kaaClient = Kaa.newClient(desktopKaaPlatformContext, new FirstKaaClientStateListener(), true);

        /*
         *  Used by log collector on each adding of the new log record in order to check whether to send logs to server.
         *  Start log upload when there is at least one record in storage.
         */
        RecordCountLogUploadStrategy strategy = new RecordCountLogUploadStrategy(1);
        strategy.setMaxParallelUploads(1);
        kaaClient.setLogUploadStrategy(strategy);

        /*
         * Persist configuration in a local storage to avoid downloading it each
         * time the Kaa client is started.
         */
        kaaClient.setConfigurationStorage(new SimpleConfigurationStorage(desktopKaaPlatformContext, "saved_config.cfg"));

        kaaClient.addConfigurationListener(new ConfigurationListener() {
            @Override
            public void onConfigurationUpdate(Configuration configuration) {
                LOG.info("Received configuration data. New sample period: {}", configuration.getSamplePeriod());
                onChangedConfiguration(TimeUnit.SECONDS.toMillis(configuration.getSamplePeriod()));
            }
        });

        //Start the Kaa client and connect it to the Kaa server.
        kaaClient.start();

        LOG.info("--= Press any key to exit =--");
        try {
            System.in.read();
        } catch (IOException e) {
            LOG.error("IOException has occurred: {}", e.getMessage());
        }
        LOG.info("Stopping...");
        scheduledExecutorService.shutdown();
        kaaClient.stop();
    }

    /*
     * Method, that emulate getting temperature from real sensor.
     * Retrieves random temperature.
     */
    private static int getTemperatureRand() {
        return new Random().nextInt(10) + 25;
    }

    private static void onKaaStarted(long time) {
        if (time <= 0) {
            LOG.error("Wrong time is used. Please, check your configuration!");
            kaaClient.stop();
            System.exit(0);
        }

        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Updated speed vehicle #1 ; timestamp" + Long.toString(System.currentTimeMillis()));

                        Scanner in = new Scanner(System.in);
                        try {
                            Object obj = new JSONParser().parse(in.nextLine());
                            if (obj instanceof JSONArray)
                                handleInput((JSONArray) obj);
                            else if (obj instanceof JSONObject)
                                handleInput((JSONObject) obj);
                        }
                        catch(org.json.simple.parser.ParseException e){

                        }

                    }
                }, 0, time, TimeUnit.MILLISECONDS);
    }

    private static void onChangedConfiguration(long time) {
        if (time == 0) {
            time = DEFAULT_START_DELAY;
        }
        scheduledFuture.cancel(false);

        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        int temperature = getTemperatureRand();
                        kaaClient.addLogRecord(new DataCollection(temperature,System.currentTimeMillis()));
                        LOG.info("Sampled Temperature: {}", temperature);
                    }
                }, 0, time, TimeUnit.MILLISECONDS);
    }

    static void handleInput(JSONArray list)
    {
      System.out.println(
      "handleInput():  got a JSONArray (list) containing " +
      list.size() + " elements");

      for (Object o : list){
        kaaClient.addLogRecord(new DataCollection(Integer.parseInt(o.toString()),System.currentTimeMillis()));
        LOG.info("Speed: {}", o);

        System.out.println(o);
      }
    }


    static void handleInput(JSONObject map)
    {
      /*      System.out.println(
      "handleInput():  got a JSONObject (map) containing " +
      map.size() + " items");
      */
      for (Iterator it = map.keySet().iterator(); it.hasNext();)
      {
        Object key = it.next();
        kaaClient.addLogRecord(new DataCollection(Integer.parseInt(map.get(key).toString()), System.currentTimeMillis()));
        LOG.info("Speed: {}", map.get(key));
        System.out.println(key + ": " + map.get(key));
      }
    }


    private static class FirstKaaClientStateListener extends SimpleKaaClientStateListener {

        @Override
        public void onStarted() {
            super.onStarted();
            LOG.info("Kaa client started");
            Configuration configuration = kaaClient.getConfiguration();
            LOG.info("Default sample period: {}", configuration.getSamplePeriod());
            onKaaStarted(TimeUnit.SECONDS.toMillis(configuration.getSamplePeriod()));
        }

        @Override
        public void onStopped() {
            super.onStopped();
            LOG.info("Kaa client stopped");
        }
    }



}
