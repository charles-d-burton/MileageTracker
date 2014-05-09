package com.charles.mileagetracker.app.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.activities.MainActivity;
import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.TripTable;
import com.charles.mileagetracker.app.database.WifiAccessPoints;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class RecordTrackService extends Service {

    private static Location location = null;
    private static LocationRequest locationRequest = null;
    private static LocationClient locationClient = null;
    private static LocationListener locationListner = null;
    private HashMap<Integer, HashMap> pathSegments = null;

    private static long lastUpdate = 0l;

    private long startTime = 0;
    private long endTime = 0;

    private int id = 0;

    private int trackingNumber = 0;

    private boolean segmentRecorded = false;

    public RecordTrackService() {

    }

    @Override
    public IBinder onBind(Intent intent) {

        //Log.v("DEBUG: ", "Recording Track Intent Bound");

        return null;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("DEBUG: ", "Recording Track Command Started");
        pathSegments = new HashMap<Integer, HashMap>();
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(30000);
        locationRequest.setFastestInterval(1000);

        id = intent.getIntExtra("id", -1);



        return 0;
    }

    //TODO: Take an action when the service is stopped, that means when it returns to a home base
    @Override
    public void onDestroy() {

    }

    /*
    Helper class that listens for location change events.
    It also runs the tests to see if distance and time thresholds have been passed for recording a path
    segment.
    */

    private final class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Log.v("DEBUG: ", "Location Update");
            generateNotification("Location Updated");
            if (RecordTrackService.this.location == null) {
                RecordTrackService.this.lastUpdate = System.currentTimeMillis();
                RecordTrackService.this.startTime = System.currentTimeMillis();

                String projection[] = {StartPoints.COLUMN_ID, StartPoints.START_LAT, StartPoints.START_LON};
                String selectionClause = StartPoints.COLUMN_ID + "= ? ";
                String selectionArgs[] = {Integer.toString(id)};

                Cursor c = getContentResolver().query(TrackerContentProvider.STARTS_URI, projection, selectionClause, selectionArgs, null);
                RecordTrackService.this.location = getFirstStartPoint(c);

            } else {
                LatLng lastLatLng = new LatLng(RecordTrackService.this.location.getLatitude(), RecordTrackService.this.location.getLongitude());
                LatLng newLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                double distance = getDistance(lastLatLng, newLatLng);
                if (distance < 300) {//Traveled less than 400 meters
                    if ((System.currentTimeMillis() - lastUpdate) > 300000 && !segmentRecorded) {//More than 5 minutes loiter
                        HashMap pathSegment = createPathSegment(RecordTrackService.this.location, location, lastUpdate, System.currentTimeMillis());
                        pathSegments.put(trackingNumber, pathSegment);
                        trackingNumber = trackingNumber + 1;
                        segmentRecorded = true;
                    }
                } else {
                    lastUpdate = System.currentTimeMillis();
                    segmentRecorded = false;
                }

            }

        }
    }



    /*
    Create a Path segment to store in memory as the application is running.  These will be used to
    create path segments in the database for long-term storage later.  That's what will be used
    to generate a report.
     */
    private HashMap createPathSegment(Location start, Location end, long startTime, long endTime){

        String distance = getDistance(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude());
        if (distance.equals("")) distance = "0";

        int distanceInt = Double.valueOf(distance).intValue();

        double startLat = start.getLatitude();
        double startLon = start.getLongitude();

        double endLat = end.getLatitude();
        double endLon = end.getLongitude();

        long totalTime = (endTime - startTime)/1000;

        HashMap segment = new HashMap();
        segment.put(TripTable.TIME_START, startTime);
        segment.put(TripTable.START_LAT, startLat);
        segment.put(TripTable.START_LON, startLon);
        segment.put(TripTable.TIME_END, endTime);
        segment.put(TripTable.END_LAT, endLat);
        segment.put(TripTable.END_LON, endLon);
        segment.put(TripTable.TOTAL_DISTANCE, distanceInt);
        segment.put(TripTable.TOTAL_TIME, totalTime);

        generateNotification(segment);

        Log.v("DEBUG: ", "Distance from maps between two points: " + distance);

        return null;
    }

    /*
    Take the Cursor which is pulling the start point of this location based on @id and create a
    location object to use as the first start point.
     */
    private Location getFirstStartPoint(Cursor c) {
        Location loc = null;
        if (!(c == null) && !(c.getCount() < 1)) {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                Log.v("DEBUG: ", "Start Point Found, setting first location");
                double lat = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LAT));
                double lon = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LON));
                loc = new Location("");
                loc.setLatitude(lat);
                loc.setLongitude(lon);
            }
        }
        return loc;
    }


    //Get the maps distance between two points.
    public String getDistance(double lat1, double lon1, double lat2, double lon2) {
        String result_in_kms = "";
        String url = "http://maps.google.com/maps/api/directions/xml?origin=" + lat1 + "," + lon1 + "&destination=" + lat2 + "," + lon2 + "&sensor=false&units=metric";
        String tag[] = {"text"};
        HttpResponse response = null;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpPost httpPost = new HttpPost(url);
            response = httpClient.execute(httpPost, localContext);
            InputStream is = response.getEntity().getContent();
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            if (doc != null) {
                NodeList nl;
                ArrayList args = new ArrayList();
                for (String s : tag) {
                    nl = doc.getElementsByTagName(s);
                    if (nl.getLength() > 0) {
                        Node node = nl.item(nl.getLength() - 1);
                        args.add(node.getTextContent());
                    } else {
                        args.add(" - ");
                    }
                }
                result_in_kms = String.format("%s", args.get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result_in_kms;
    }

    //Another convenience method that just lets me get the distance between two points.
    private double getDistance(LatLng pointA, LatLng pointB) {
        double distance = 0f;

        Location a = new Location("pointA");
        a.setLatitude(pointA.latitude);
        a.setLongitude(pointA.longitude);

        Location b = new Location("pointB");
        b.setLatitude(pointB.latitude);
        b.setLongitude(pointB.longitude);
        distance = a.distanceTo(b);

        return distance;
    }

    /*
    Temporary method for testing purposes, it's a verbose notification system.
     */

    private void generateNotification(HashMap map) {
        String message = "Generating Path Segment, distance: " + Integer.toString((Integer)map.get(TripTable.TOTAL_DISTANCE));
        Context context = this.getApplicationContext();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Test")
                .setContentText(message);
        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }

    private void generateNotification(String message) {
        Context context = getApplicationContext();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Test")
                .setContentText(message);
        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }

}
