package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.charles.mileagetracker.app.database.orm.HomePoints;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.Status;
import com.charles.mileagetracker.app.processingservices.GetCurrentLocation;
import com.charles.mileagetracker.app.services.ActivityRecognitionService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class PostBootGeofenceService extends IntentService implements
        GetCurrentLocation.GetLocationCallback,
        LocationClient.OnAddGeofencesResultListener {
    private Context context;
    private GetCurrentLocation getCurrentLocation = null;

    //private HashMap<Integer, LatLng> fenceCenters = new HashMap<Integer, LatLng>();
    private List<HomePoints> homePointsList = null;
    private boolean startedActivityRecognition = false;

    public PostBootGeofenceService() {
        super("PostBootGeofenceService");
    }


    /*
    Received the boot Intent.  Check if there are geofence HomePoints that need to be added.  If there are
    then start the connection to the Location Services and then add the HomePoints.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        context = getApplicationContext();
        if (intent != null) {
            final String action = intent.getAction();
            homePointsList = HomePoints.listAll(HomePoints.class);
            if (!homePointsList.isEmpty()) {
                getCurrentLocation = new GetCurrentLocation(context, 10, GetCurrentLocation.PRECISION.HIGH);
                getCurrentLocation.updateLocation(this, false);
            }

            Log.v("DEBUG: ", "Trying to start locationclient from boot");

        }
    }

    @Override
    public void onAddGeofencesResult(int i, String[] strings) {

    }

    /*
    I know that you add Geofences by List and the calling method here is actually generating basically
    a list of Geofences so this might seem inefficient.  It however is not what it seems, I need to
    maintain requestId coherency with the associated SQL _id of the column.  This means that I cannot
    add them based on index and instead have to add them one at a time using the associated ID.
     */
    private void addProximityAlert(LatLng latLng, int id) {
        //this.context = context;
        Intent intent = new Intent("com.charles.mileagetracker.app.ACTION_RECEIVE_GEOFENCE");
        intent.putExtra("id", id);
        intent.putExtra("lat", latLng.latitude);
        intent.putExtra("lon", latLng.longitude);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Geofence fence = new Geofence.Builder()
                .setRequestId(Integer.toString(id))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(latLng.latitude, latLng.longitude, 500)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        java.util.List fencesList = new ArrayList();
        fencesList.add(fence);
        getCurrentLocation.addGeoFence(fencesList, pendingIntent, this);
        Log.d("DEBUG: ", "Adding proximity alert");
    }

    @Override
    public void retrievedLocation(double resolution, Location location) {
        int infenceId = checkInFence(new LatLng(location.getLatitude(), location.getLongitude()));
        if (infenceId != -1) {
            startUpdates(location, infenceId);
        }
    }

    /*
    This will only be called because above the code was ran to connect to the Location Services.
    Once the LocationClient is connected this means that geofences can be added.
     */
    @Override
    public void locationClientConnected() {
        if (!homePointsList.isEmpty()) {
            for (HomePoints homePoint : homePointsList) {
                //After a reboot you have to re-add the geofences.  This handles that.
                addProximityAlert(new LatLng(homePoint.lat, homePoint.lon), homePoint.getId().intValue());
            }
        }
    }

    @Override
    public void locationConnectionFailed() {

    }

    /*
    Check if currently inside a geofence
     */
    private int checkInFence(LatLng currentLocation) {
        Log.v("DEBUG: ", "Checking if in fence");

        Iterator<HomePoints> it = homePointsList.iterator();
        double smallestDistance = Double.MAX_VALUE;
        int closestId = -1;
        while (it.hasNext()) {
            HomePoints homePoint = it.next();
            int id = homePoint.getId().intValue();
            LatLng center = new LatLng(homePoint.lat, homePoint.lon);
            double distance = getDistance(currentLocation, center);
            if (distance < 500){//Inside GeoFence Within Margin of Error
                return -1; //Known impossible value
            }
            if (distance < smallestDistance) {
                smallestDistance = distance; //Finding the nearest geofence
                closestId = id;
            }

        }
        return closestId;
    }

    /*
    Calculate the distance between two points as the crow flies
     */

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
    Check if there was a previously started record.  We want to use that if there was, if not then create
    a new one then start the service.  It uses the fence that it was closest to for the id.
     */
    private void startUpdates(Location location, int id) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        TripGroup group = new TripGroup(false);
        group.save();

        Status status = null;
        List<Status> statuses = Status.listAll(Status.class);
        if (!statuses.isEmpty()){
            status = statuses.get(0);
        } else {
            status = new Status(false, lat, lon, lat, lon, 0, new Date(), group);
        }

        status.save();

        Intent intent = new Intent(context, ActivityRecognitionService.class);
        intent.putExtra("id", id);
        intent.putExtra("lat", location.getLatitude());
        intent.putExtra("lon", location.getLongitude());
        context.startService(intent);
        startedActivityRecognition = true;
    }

}
