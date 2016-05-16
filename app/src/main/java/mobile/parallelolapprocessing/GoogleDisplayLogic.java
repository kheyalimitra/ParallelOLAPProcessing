package mobile.parallelolapprocessing;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import DataRetrieval.Dimension;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.CacheProcessUpto1Level;
import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;
import mobile.parallelolapprocessing.UI.DimensionMeasureGoogleHTMLTable;
import mobile.parallelolapprocessing.UI.DimensionMeasuresGoogleHTMLBarChart;
import mobile.parallelolapprocessing.UI.DisplayThread;
import mobile.parallelolapprocessing.UI.IDimensionMeasureDisplay;

/**
 * Created by jayma on 3/13/2016.
 */
public class GoogleDisplayLogic extends AppCompatActivity {
    public final static String View_PARAM = "fragment";
    public  static String formattedTable = null;
    public static String formattedBarChart =  null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.googledisplaycomponent);


        final TextView timeStampDisplay = (TextView)findViewById(R.id.timeToDownload);

        Long timeTaken = DimensionTree.timeTaken;
        StringBuilder timeStampsDetails =new StringBuilder();
        timeStampsDetails.append("Data Fetch (ms): " + timeTaken.toString());
        DisplayThread dth =  new DisplayThread();
        long startDisplayTime = System.currentTimeMillis();
        Log.d("Display Query:", "Google chart-Table display table-bar chart call: " + String.valueOf(System.currentTimeMillis()));

        dth.run();

        //dth.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        long wait = (long) 0.1;
        /*while(DisplayThread.isFilled == false){
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/
        //formattedTable =
        //if(formattedTable!= null) {
            _generateTable(formattedTable);
        //}
        //if(formattedBarChart != null) {
            _generateBarChart(formattedBarChart);
        //}
        _displayPerformanceInfo(timeStampDisplay, timeStampsDetails, startDisplayTime);

        Log.d("Display Query:", "Google chart-Table display ends: " + String.valueOf( System.currentTimeMillis()));

        // flush previous query by user:
        MDXUserQuery.isComplete =  false;
        formattedBarChart = null;
        formattedTable =  null;
        DisplayThread.isFilled = false;
        //Serial execution
       // DimensionTree dt = new DimensionTree();
       // dt.startAsyncThreads();

    }

    private void _displayPerformanceInfo(TextView timeStampDisplay, StringBuilder timeStampsDetails, Long startDisplayTime) {
        Long endDisplayTime =  System.currentTimeMillis()-startDisplayTime;
        timeStampsDetails.append("| Display (ms):" + endDisplayTime.toString());
        if(DimensionMeasureGoogleHTMLTable.DataDisplaySize>=1024) {
            Long size = (DimensionMeasureGoogleHTMLTable.DataDisplaySize / 1024);
            timeStampsDetails.append("| Size (kb):" + size.toString());
        }
        else
        {
            timeStampsDetails.append("| Size (b):" + DimensionMeasureGoogleHTMLTable.DataDisplaySize.toString());
        }
        timeStampsDetails.append(" | % of Hit:" + String.valueOf(QueryProcessor.hitcount*100));
        timeStampDisplay.setText(timeStampsDetails.toString());
        //new DimensionTree().startAsyncThreads();
    }


    private void _generateTable(String formattedTable) {
        WebView tableView = (WebView) findViewById(R.id.tableView);
        WebSettings webSettings = tableView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        tableView.requestFocusFromTouch();
        tableView.loadDataWithBaseURL("file:///android_asset/", formattedTable, "text/html", "utf-8", null);
    }
    private void _generateBarChart(String formattedTable) {
        WebView tableView = (WebView) findViewById(R.id.barChartView);
        WebSettings webSettings = tableView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        tableView.requestFocusFromTouch();
        tableView.loadDataWithBaseURL("file:///android_asset/", formattedTable, "text/html", "utf-8", null);
    }

}
