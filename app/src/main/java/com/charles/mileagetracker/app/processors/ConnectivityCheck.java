package com.charles.mileagetracker.app.processors;

import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Helper class that checks to make sure that you're both connected to the internet and that you
 * have a good data connection as well.
 */
public class ConnectivityCheck {
    public ConnectivityCheck() {

    }

    public static synchronized boolean isConnected() {
        if (hasInternetAccess() && checkDataStatus()) {
            return true;
        }
        return false;
    }

    private static boolean hasInternetAccess() {
        try {
            HttpURLConnection urlc = (HttpURLConnection)
                    (new URL("http://clients3.google.com/generate_204")
                            .openConnection());
            urlc.setRequestProperty("User-Agent", "Android");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(5000);
            urlc.connect();
            return (urlc.getResponseCode() == 204 &&
                    urlc.getContentLength() == 0);
        } catch (IOException e) {
            Log.e("Error: ", "Error checking internet connection", e);
        } catch (Exception e) {
            Log.e("Error: ", "Error checking internet connection", e);
        }
        return false;
    }

    private static boolean checkDataStatus() {
        boolean isConnected = false;
        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setConnectTimeout(5000);
            urlc.connect();
            if (urlc.getResponseCode() == 200) {
                isConnected = true;
            }
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("Error: ", "Error checking internet connection", e);
        }
        return isConnected;
    }
}
