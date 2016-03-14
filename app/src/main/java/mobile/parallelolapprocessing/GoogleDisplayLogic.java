package mobile.parallelolapprocessing;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import DataRetrieval.Dimension;
import DataStructure.TreeNode;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;
import mobile.parallelolapprocessing.UI.DimensionMeasureGoogleHTMLTable;
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
        timeStampDisplay.setText("Time taken to process: "+ timeTaken.toString()+"ms");
        //String s = _displayResultInTable();
        IDimensionMeasureDisplay displayResult = new DimensionMeasureGoogleHTMLTable();
        String formattedTable = displayResult.GetDisplay(QueryProcessor.resultSet,
                DimensionMeasureGoogleHTMLTable.keyValPairsForDimension,// no need to pass it
                DimensionMeasureGoogleHTMLTable.measureMap);// no need to pass it
        Log.d("Html string:",formattedTable);
        _genertateTable(formattedTable);
        // flush previous query by user:
        MDXUserQuery.isComplete =  false;
        //QueryProcessor.resultSet = new HashMap<>();


    }


    private String[] _getDimensionNamesFromKeys(String combination) {
        String[] keys =  combination.split("#");
        String[] dimensionNames = new String[keys.length];

        if(keys.length>0) {
            for (int i=0;i<keys.length;i++){
                Integer keyInt = Integer.parseInt(keys[i]);

                TreeNode node =  MDXUserQuery.keyValPairsForDimension.get(keyInt);
                if(node!=null) {
                    dimensionNames[i] = node.getReference().toString();
                }
            }

        }
        return dimensionNames;

    }


    private void _genertateTable(String formattedTable) {
        WebView tableView = (WebView) findViewById(R.id.tableView);
        WebSettings webSettings = tableView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        tableView.requestFocusFromTouch();
        tableView.loadDataWithBaseURL( "file:///android_asset/", formattedTable, "text/html", "utf-8", null );
    }


}
