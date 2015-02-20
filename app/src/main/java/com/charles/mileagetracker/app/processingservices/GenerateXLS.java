package com.charles.mileagetracker.app.processingservices;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.charles.mileagetracker.app.database.orm.TripRow;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by charles on 2/18/15.
 */
public class GenerateXLS extends AsyncTask<Long, Void, Boolean> {

    private Context context;
    private ProgressDialog csvDialog;
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");
    protected static final String dir = "MileageTracker";
    protected static final String fileName = "TripReport.xlsx";

    private final String[] header = {"Date", "Start Address", "End Address", "Distance(mi)"};


    public GenerateXLS(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Long... params) {
        long start = params[0];
        long end = params[1];

        String entries[] = {Long.toString(start), Long.toString(end)};
        List<TripRow> rows = TripRow.find(TripRow.class, "time_start BETWEEN ? AND ? ", entries, null, "id DESC", null);
        //If nothing is found, continue no further
        if (rows == null || rows.isEmpty() || rows.size() < 2) {
            return false;
        }

        Workbook wb = new HSSFWorkbook();
        Cell c = null;

        //Cell style for header row
        CellStyle cs = wb.createCellStyle();
        cs.setFillForegroundColor(HSSFColor.BLUE_GREY.index);
        cs.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        Sheet sheet = wb.createSheet("Trips Data");
        setupHeader(c, cs, sheet);

        Iterator<TripRow> it = rows.iterator();
        TripRow lastStop = it.next();
        Row row = null;
        int rowIndex = 1;
        while (it.hasNext()) {
            TripRow stop = it.next();
            if (stop.businessRelated) {
                row = sheet.createRow(rowIndex);
                rowIndex = rowIndex + 1;
                c = row.createCell(0);
                c.setCellValue(stop.timeStart);
                c = row.createCell(1);
                c.setCellValue(lastStop.address);
                c = row.createCell(2);
                c.setCellValue(stop.address);
                c = row.createCell(3);
                c.setCellValue(stop.distance * .621);//Convert KM to Mi
            }
            lastStop = stop;
        }
        if (sheet.getLastRowNum() > 1) {
            writeToFile(wb);
            String mailAddress = getEmail();
            emailFile(mailAddress);
            return true;
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean s) {
        super.onPostExecute(s);
        if (s == true) {
            csvDialog.dismiss();
            Toast.makeText(context, "Email Sent", Toast.LENGTH_LONG).show();
        } else {
            csvDialog.dismiss();
            Toast.makeText(context, "No Data Found", Toast.LENGTH_LONG).show();
        }
    }

    private void setupHeader(Cell c, CellStyle cs, Sheet sheet) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < header.length; i++) {
            c = row.createCell(i);
            c.setCellValue(header[i]);
            c.setCellStyle(cs);
            sheet.setColumnWidth(i, (15*500));
        }
    }

    private void createRow(HashMap values, Cell c, Row row, double distance) {

    }

    //Save the CSV array to a file.
    private boolean writeToFile(Workbook wb) {
        if (isExternalStorageWritable()) {
            File dir = new File(Environment.getExternalStorageDirectory(), this.dir);
            if (!dir.exists()) dir.mkdir();
        }


        File file = new File(Environment.getExternalStorageDirectory(), dir + File.separator + fileName);
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            wb.write(os);
            Log.w("FileUtils", "Writing file" + file);
            return true;
        } catch (IOException e) {
            Log.w("FileUtils", "Error writing " + file, e);
            return false;
        } catch (Exception e) {
            Log.w("FileUtils", "Failed to save file", e);
            return false;
        } finally {
            try {
                if (null != os)
                    os.close();
            } catch (Exception ex) {
            }
        }
    }

    private void readFromFile() {
        File file = new File(context.getApplicationContext().getFilesDir(), "email.csv");

        try {
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] line = null;
            while ((line = reader.readNext()) != null) {
                Log.v("CSV: ", Arrays.toString(line));
            }
        } catch (Exception e) {
            Log.v("CSV: ", "FILE NOT FOUND");
            e.printStackTrace();
        }

    }

    //Create the intent to email the file.  It gets the default emaila address from the Android
    //system, then attaches the file to that email.
    private void emailFile(String mailAddress) {
        File file = new File(Environment.getExternalStorageDirectory(), dir + File.separator + fileName);
        //File file = new File(context.getFilesDir(), "email.csv");
        if (file.exists()) {
            Log.v("File URI: ", Uri.fromFile(file).toString());
            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, mailAddress);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Your Trip Report");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Your Trip Report");
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(emailIntent);

        }
    }

    private String getEmail() {
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                String possibleEmail = account.name;
                if (possibleEmail.contains("gmail")) {
                    return possibleEmail;
                }
                Log.v("Email Address: ", possibleEmail);
            }
        }
        return null;
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}
