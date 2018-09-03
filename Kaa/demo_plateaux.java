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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import java.net.ServerSocket;


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

        //Connect to the python app


        //Start the Kaa client and connect it to the Kaa server.
        kaaClient.start();

        LOG.info("--= Press any key to exit =--");
        try {
            System.in.read();
        } catch (IOException e) {
            LOG.error("IOException has occurred: {}", e.getMessage());
        }
        LOG.info("Stopping...");
        /*
        try {
            listeningsocket.close();
            actorsocket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        */
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
                        //System.out.println("Updated speed vehicle #1 ; timestamp" + Long.toString(System.currentTimeMillis()));
                        System.out.print(".");


                        /*Scanner in = new Scanner(System.in);
                        try {
                            Object obj = new JSONParser().parse(in.nextLine());
                            if (obj instanceof JSONArray)
                                handleInput((JSONArray) obj);
                            else if (obj instanceof JSONObject)
                                handleInput((JSONObject) obj);
                        }
                        catch(org.json.simple.parser.ParseException e){

                        }*/

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


    private class Dispatcher implements Runnable{
    /*the purpose of the dispatcher is to accept new connections from
    non-kaa endpoints and generate the actors */
        private ServerSocket listeningSocket ;

        private Socket newActorSocket;
        private BufferedReader actorIn;
        private PrintWriter actorOut;
        //private ArrayList<Socket> actorSocketList ;
        //private ArrayList<Actor> actorList;

        private String endpointType; //"vehicle" or "light"

        private int nEndpoints = 0; //number of endpoints connected (and that have a corresponding actor running

        @Override
        public void run() {

            try {
                listeningSocket = new ServerSocket(6969);

                System.out.println("Waiting for connection on port 6969");
                newActorSocket = listeningSocket.accept();
                System.out.println("Connected to endpoint on port " + newActorSocket.getPort());


                actorIn = new BufferedReader(new InputStreamReader(newActorSocket.getInputStream()));
                actorOut = new PrintWriter(newActorSocket.getOutputStream());



                //TODO: receive type of device from device



                if (endpointType.matches("") ){
                    Thread newAct = new Thread(new VehicleActor(newActorSocket, actorIn, actorOut));
                    newAct.start();
                }

                if (endpointType.matches("vert|rouge")){ //here we use the first message to get the type
                    Thread newAct = new Thread(new TrafficLightActor(newActorSocket, actorIn, actorOut));
                    newAct.start();
                }

                //actorSocketList.get(nEndpoints).sendUrgentData(7);

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    public class Actor{
        private Socket socket;
        protected BufferedReader in;
        protected PrintWriter out;
        private String type;


        public Actor(Socket socket, BufferedReader in, PrintWriter out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
        }
    }

    public class VehicleActor extends Actor implements Runnable{
        private String message;

        public VehicleActor(Socket socket, BufferedReader in, PrintWriter out) {
            super(socket, in, out);
        }

        @Override
        public void run() {
            try{
                try {
                    Object obj = new JSONParser().parse(in.readLine());
                    if (obj instanceof JSONArray)
                        handleInput((JSONArray) obj);
                    else if (obj instanceof JSONObject)
                        handleInput((JSONObject) obj);
                }
                catch(org.json.simple.parser.ParseException e){
            }
            catch(IOException e) {

                e.printStackTrace();
            }


        }
    }


    public class TrafficLightActor extends Actor implements Runnable{
        private String message;
        private boolean isRed = false ;

        public TrafficLightActor(Socket socket, BufferedReader in, PrintWriter out) {
            super(socket, in, out);
        }


        @Override
        public void run() {
            try{
                message = in.readLine();
                if (message.compareTo("rouge") == 0)
                    isRed = true;
                    //TODO log
                if (message.compareTo("vert") == 0)
                    isRed = false;

            }
            catch(IOException e) {

                e.printStackTrace();
        }

        }
    }

    public class Emission implements Runnable{
        @Override
        public void run() {

        }
    }


    public class Reception implements Runnable{
        @Override
        public void run() {

        }
    }
}
