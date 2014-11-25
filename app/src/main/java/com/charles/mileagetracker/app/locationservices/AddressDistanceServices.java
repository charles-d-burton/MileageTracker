package com.charles.mileagetracker.app.locationservices;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Created by charles on 6/23/14.
 */
public class AddressDistanceServices {

    private Context context;

    public AddressDistanceServices(Context context) {
        this.context = context;
    }


    public String getRoadName(String lat, String lng) throws JSONException {

        String roadName = "";
        String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=__LAT__,__LNG__&sensor=false";

        url = url.replaceAll("__LAT__", lat);
        url = url.replaceAll("__LNG__", lng);
        //Log.v("DEBUG:","URL: " + url.toString());


        String result = getStringFromUrl(url);
        if (result == null) return "";
        JSONObject jObject = new JSONObject(result);
        JSONArray jArray = jObject.getJSONArray("results");
        if (jArray != null && jArray.length() > 0) {
            for (int i = 0; i < jArray.length(); i++) {
                try {
                    JSONObject oneObject = jArray.getJSONObject(i);
                    // Pulling items from the array
                    roadName = oneObject.getString("formatted_address");
                    break;
                } catch (JSONException e) {
                    // Oops
                }
            }
        }
        return roadName;
    }

    public String getRoadName(double lat, double lon) {
        String name = "";
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                name = address.getAddressLine(0) + ",\n" + address.getAddressLine(1) + ",\n" + address.getAddressLine(2);

            }
        } catch (IOException ioe) {

        }
        return name;
    }

    public double getDistance(double lat1, double lon1, double lat2, double lon2) {

        String url = "https://maps.google.com/maps/api/directions/json?origin=" + lat1 +"," + lon1 + "&destination=" + lat2 + "," + lon2 + "&mode=driving&sensor=false&units=metric";
        String result = getStringFromUrl(url);

        if (result == null) return -1;
        if (result.trim().length() == 0) return -1;
        JSONObject jObject = null;
        try {
            jObject = new JSONObject(result);
            JSONArray array = jObject.getJSONArray("routes");

            JSONObject routes = array.getJSONObject(0);

            JSONArray legs = routes.getJSONArray("legs");

            JSONObject steps = legs.getJSONObject(0);

            Integer distance = (Integer)steps.getJSONObject("distance").get("value");
            Log.v("DISTANCE: ", distance.toString());
            return (distance / 1000);
            //return getDoubleFromString(distance.toString()) / 1000;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String getDirectionsURL (double sourcelat, double sourcelog, double destlat, double destlog ){
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString
                .append(Double.toString( sourcelog));
        urlString.append("&destination=");// to
        urlString
                .append(Double.toString( destlat));
        urlString.append(",");
        urlString.append(Double.toString( destlog));
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        return urlString.toString();
    }

    //Pull down the JSON from the provided URL
    public String getStringFromUrl(String url) {
        DefaultHttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());
        HttpGet httpget = new HttpGet(url);

        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        String result = null;
        try {
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            inputStream = entity.getContent();
            // json is UTF-8 by default
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder sb = new StringBuilder();

            String line = null;
            while ((line = bufferedReader.readLine()) != null)
            {
                sb.append(line + "\n");
            }
            result = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            // Oops
        }
        finally {
            try{
                //if(bufferedReader != null) bufferedReader.close();
                if(inputStream != null)inputStream.close();
            }catch(Exception squish){}
        }
        return result;
    }

    public LinkedList<LatLng> decodePoly(String encoded) {

        LinkedList<LatLng> poly = new LinkedList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }

    public String getAddressFromLatLng(LatLng latLng) throws IOException {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(context, Locale.getDefault());
        addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

        String address = addresses.get(0).getAddressLine(0);
        String city = addresses.get(0).getAddressLine(1);
        String country = addresses.get(0).getAddressLine(2);

        return address + "\n" + city + "\n" + country;
    }
}
