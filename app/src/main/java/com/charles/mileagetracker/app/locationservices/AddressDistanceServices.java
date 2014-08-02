package com.charles.mileagetracker.app.locationservices;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
        Double result_in_kms = -1.0;

        String url = "https://maps.google.com/maps/api/directions/json?origin=" + lat1 +"," + lon1 + "&destination=" + lat2 + "," + lon2 + "&mode=driving&sensor=false&units=metric";
        String result = getStringFromUrl(url);
        if (result == null) return -1;
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
        return result_in_kms;
    }



    //Get the maps distance between two points.
    /*public double getDistance(double lat1, double lon1, double lat2, double lon2) {
        String result_in_kms = "-1";
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

            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        return getDoubleFromString(result_in_kms);
        //return Double.parseDouble(result_in_kms);
    }*/

    private double getDoubleFromString(String string) {
        Scanner st = new Scanner(string);
        while (!st.hasNextDouble()) {
            st.next();
        }
        return st.nextDouble();
    }

    private String getStringFromUrl(String url) {
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
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
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

}
