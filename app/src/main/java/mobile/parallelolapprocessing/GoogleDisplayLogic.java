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

        _genertateTable(formattedTable);

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
        timeStampDisplay.setText(timeStampsDetails.toString());
        // takes care of past dimensions selected and if needed flushing cache
        _recordQueryHistory();
        // flush previous query by user:
        MDXUserQuery.isComplete =  false;
        //QueryProcessor.resultSet = new HashMap<>();
        // start asynchronous threads for inflated values
        //new DimensionTree().startAsyncThreads();

    }
    private void _recordQueryHistory() {
        // if 0% hit and usr is moving to different direction that previous one flush memory
        if(!_isCurrentSelectionRelatedToPastDimensions())
        {
            if(MainActivity.CachedDataCubes.size()>0) {
                MainActivity.CachedDataCubes.clear();
                MDXQProcessor.lastTenSelectedDimensions.clear();
                CacheProcess.inflatedQueries.clear();
                CacheProcessUpto1Level.inflatedQueries.clear();
                System.gc();
            }
        }
        else {
            // add current selection in exisiting SET
            _setLastSelectedDimensions();
        }
    }

    private  boolean _isCurrentSelectionRelatedToPastDimensions(){
        if(MDXQProcessor.lastTenSelectedDimensions.size()>0) {
            for (String dimensions : DimensionTree.UserSelectedDimensionCombinations) {
                String eachDimension[] = dimensions.split("#");
                for (String key : eachDimension) {
                    if (MDXQProcessor.lastTenSelectedDimensions.contains(_findRootParent(key))) {
                        return true;
                    }

                }
            }
            return false;
        }
        else {
            return true;
        }

    }
    private void  _setLastSelectedDimensions(){
        for (String dimensions : DimensionTree.UserSelectedDimensionCombinations){
            String eachDimension []= dimensions.split("#");
            for (String key : eachDimension){
                MDXQProcessor.lastTenSelectedDimensions.add(_findRootParent(key));
            }
        }
    }

    private Integer _findRootParent(String key) {
        DataStructure.TreeNode node = Dimension.dimensionHiearchyMap.get(Integer.parseInt(key));
        Integer parentNodeKey = -1;
        if(node!=null){
            while(node.getParent().getReference() != "Dimension"){
                node = node.getParent();
            }
            parentNodeKey = node.getNodeCounter();
        }
        return  parentNodeKey;
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
        tableView.loadDataWithBaseURL("file:///android_asset/", formattedTable, "text/html", "utf-8", null);
    }


}
