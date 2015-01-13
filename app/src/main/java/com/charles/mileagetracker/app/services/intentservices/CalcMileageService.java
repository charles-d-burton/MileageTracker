package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.charles.mileagetracker.app.database.orm.HomePoints;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class CalcMileageService extends IntentService {

    private final String CLASS_NAME = ((Object)this).getClass().getName();
    private AddressDistanceServices locationServices = null;

    public CalcMileageService() {
        super("CalcMileageService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (haveNetworkConnection()) {
                if (hasInternetAccess()) {
                    Context context = getApplicationContext();
                    locationServices = new AddressDistanceServices(context);
                    iterateGroups();
                }
            }
        }
    }

    //Check that we have a good connection
    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            /*if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;*/
        }
        //return haveConnectedWifi || haveConnectedMobile;
        return haveConnectedWifi;
    }

    public boolean hasInternetAccess() {
        try {
            HttpURLConnection urlc = (HttpURLConnection)
                    (new URL("http://clients3.google.com/generate_204")
                            .openConnection());
            urlc.setRequestProperty("User-Agent", "Android");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(1500);
            urlc.connect();
            return (urlc.getResponseCode() == 204 &&
                    urlc.getContentLength() == 0);
        } catch (IOException e) {
            Log.e(CLASS_NAME, "Error checking internet connection", e);
        }
        return false;
    }

    private boolean checkDataStatus() {
        boolean isConnected = false;
        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setConnectTimeout(3000);
            urlc.connect();
            if (urlc.getResponseCode() == 200) {
                isConnected = true;
            }
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected;
    }

    private void iterateGroups() {
        List<TripGroup> groups = TripGroup.listAll(TripGroup.class);
        Iterator<TripGroup> it = groups.iterator();
        while (it.hasNext()) {
            TripGroup group = it.next();
            processGroup(group);
        }
    }

    //Process a tgroup, find rows based on their relationship with the TripGroup.  Then iterate through the
    //trips.
    private void processGroup(TripGroup group) {
        AddressDistanceServices addressDistanceServices = new AddressDistanceServices(getApplicationContext());

        String params[] = {Long.toString(group.getId())};
        List<TripRow> stops = TripRow.find(TripRow.class, " tgroup = ? ", params ,null , " ORDER BY id ASC",null);

        double totalMileage = 0;
        double billableMileage = 0;

        //Empty or unresolved Trip Set, remove it.
        if (group.group_closed && (stops.isEmpty() || stops.size() == 1)) {

            Log.v(CLASS_NAME, "Empty Trip Set");
            group.delete();

        } else {
            Iterator<TripRow> it = stops.iterator();
            TripRow stop = it.next();
            while (it.hasNext()) {
                TripRow nextStop = it.next();
                if (nextStop.distance != 0) { //Skip Rows that have been computed
                    totalMileage = totalMileage + nextStop.distance;
                    if (nextStop.businessRelated) {
                        billableMileage = billableMileage + nextStop.distance;
                    }
                    stop = nextStop;//Re-assigns stop.
                    continue;
                } else {
                    double distance = locationServices.getDistance(stop.lat, stop.lon, nextStop.lat, nextStop.lon);
                    nextStop.distance = distance;

                    totalMileage = totalMileage + distance;
                    if (nextStop.businessRelated) {
                        billableMileage = billableMileage + distance;
                    }

                    try {
                        nextStop.address = addressDistanceServices.getAddressFromLatLng(new LatLng(nextStop.lat, nextStop.lon));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    nextStop.save();
                    //updateAddress(nextStop);
                    stop = nextStop;
                }
            }
        }

        if (totalMileage != 0 && billableMileage != 0) {
            group.billableMileage = billableMileage;
            group.totalMileage = totalMileage;
            group.save();
        }
    }

    private double distanceBetweenStops(TripRow stop1, TripRow stop2) {
        double distance = 0;

        return distance;
    }

    private boolean tooCloseToHome(TripRow row, List<HomePoints> homes) {
        boolean tooClose = true;

        return tooClose;
    }

}
