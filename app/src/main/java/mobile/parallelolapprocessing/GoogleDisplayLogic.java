package mobile.parallelolapprocessing;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import android.webkit.WebSettings;
import android.webkit.WebView;

import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DataRetrieval.Dimension;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.CacheProcessUpto1Level;
import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;
import mobile.parallelolapprocessing.UI.DimensionMeasureGoogleHTMLTable;
import mobile.parallelolapprocessing.UI.DimensionMeasuresGoogleHTMLBarChart;
import mobile.parallelolapprocessing.UI.IDimensionMeasureDisplay;

/**
 * Created by jayma on 3/13/2016.
 */
public class GoogleDisplayLogic extends AppCompatActivity {
    public final static String View_PARAM = "fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.googledisplaycomponent);
        //////////.View v = (View) findViewById(R.layout.display);
        //EditText Dimsize = (EditText)findViewById();
        TextView timeStampDisplay = (TextView)findViewById(R.id.timeToDownload);
        Long timeTaken = DimensionTree.timeTaken;
        StringBuilder timeStampsDetails =new StringBuilder();
        timeStampsDetails.append("Data Fetch (ms): " + timeTaken.toString() );

        //String s = _displayResultInTable();
        Long startDisplayTime =  System.currentTimeMillis();
        DimensionTree.UserSelectedDimensionCombinations = this._sortEachCombination(DimensionTree.UserSelectedDimensionCombinations);
        IDimensionMeasureDisplay displayResult = new DimensionMeasureGoogleHTMLTable();
        String formattedTable = displayResult.GetDisplay(MainActivity.CachedDataCubes,
                DimensionTree.UserSelectedDimensionCombinations,
                DimensionTree.UserSelectedMeasures,
                Dimension.dimensionHiearchyMap,
                MainActivity.MeasuresList);
        _generateTable(formattedTable);

        IDimensionMeasureDisplay displayResult2 = new DimensionMeasuresGoogleHTMLBarChart();
        String formattedBarChart = displayResult2.GetDisplay(MainActivity.CachedDataCubes,
                DimensionTree.UserSelectedDimensionCombinations,
                DimensionTree.UserSelectedMeasures,
                Dimension.dimensionHiearchyMap,
                MainActivity.MeasuresList);

        _generateBarChart(formattedBarChart);
        _displayPerformanceInfo(timeStampDisplay, timeStampsDetails, startDisplayTime);
        Log.d("Original Query", "Display process ends " + String.valueOf(System.currentTimeMillis()));

        //run sequential execution of inflated queries
        // flush previous query by user:
        MDXUserQuery.isComplete =  false;

        //QueryProcessor.resultSet = new HashMap<>();
        // start asynchronous threads for inflated values

    }

    private void _displayPerformanceInfo(TextView timeStampDisplay, StringBuilder timeStampsDetails, Long startDisplayTime) {
        //Long endDisplayTime =  System.currentTimeMillis()-startDisplayTime;
        //timeStampsDetails.append("| Display (ms):" + endDisplayTime.toString());
        //if(DimensionMeasureGoogleHTMLTable.DataDisplaySize>=1024) {
         //   Long size = (DimensionMeasureGoogleHTMLTable.DataDisplaySize / 1024);
        //    timeStampsDetails.append("| Size (kb):" + size.toString());
        //}
       // else
       // {
       //     timeStampsDetails.append("| Size (b):" + DimensionMeasureGoogleHTMLTable.DataDisplaySize.toString());
       // }
        timeStampsDetails.append(" | % of Hit:" + String.valueOf(QueryProcessor.hitcount*100));
        timeStampDisplay.setText(timeStampsDetails.toString());
    }

    private List<String> _sortEachCombination(List<String> userSelectedDimensionCombinations) {
        for(int i=0;i<userSelectedDimensionCombinations.size();i++){
            userSelectedDimensionCombinations.set(i, this._sortKey(userSelectedDimensionCombinations.get(i)));
        }
        return userSelectedDimensionCombinations;
    }

    private String _sortKey(String keyCombination) {
        String[] keyArray = keyCombination.split("#");
        Integer[] keyIntArray = new Integer[keyArray.length];
        for(int i=0;i<keyIntArray.length;i++){
            keyIntArray[i] = Integer.parseInt(keyArray[i]);
        }

        Arrays.sort(keyIntArray);
        return TextUtils.join("#",keyIntArray);
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
