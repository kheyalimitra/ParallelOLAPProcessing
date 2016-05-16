package mobile.parallelolapprocessing.UI;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import DataRetrieval.Dimension;
import mobile.parallelolapprocessing.DimensionTree;
import mobile.parallelolapprocessing.GoogleDisplayLogic;
import mobile.parallelolapprocessing.MainActivity;

/**
 * Created by jayma on 4/5/2016.
 */
public class DisplayThread extends AsyncTask{ //implements Runnable {

    private Thread displayThreadInstance =  null;
    public static boolean isFilled = false;
    public static HashMap<String, HashMap<Integer, Long>> downloadedResultSet = new HashMap();
    //@Override
    public void run() {
        //String s = _displayResultInTable();

        DimensionTree.UserSelectedDimensionCombinations = this._sortEachCombination(DimensionTree.UserSelectedDimensionCombinations);
        IDimensionMeasureDisplay displayResult = new DimensionMeasureGoogleHTMLTable();
        GoogleDisplayLogic.formattedTable = displayResult.GetDisplay(MainActivity.CachedDataCubes,//downloadedResultSet,
                DimensionTree.UserSelectedDimensionCombinations,
                DimensionTree.UserSelectedMeasures,
                Dimension.dimensionHiearchyMap,
                MainActivity.MeasuresList);
        IDimensionMeasureDisplay displayResult2 = new DimensionMeasuresGoogleHTMLBarChart();
        GoogleDisplayLogic.formattedBarChart = displayResult2.GetDisplay(MainActivity.CachedDataCubes,//downloadedResultSet,
                DimensionTree.UserSelectedDimensionCombinations,
                DimensionTree.UserSelectedMeasures,
                Dimension.dimensionHiearchyMap,
                MainActivity.MeasuresList);
        isFilled = true;

    }
    public void start ()
    {
        if (displayThreadInstance == null)
        {
           // displayThreadInstance =new Thread (this);
           // isFilled = false;
           // displayThreadInstance.start ();

        }
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
        return TextUtils.join("#", keyIntArray);
    }


    @Override
    protected Object doInBackground(Object[] params) {
        isFilled = false;
        this.run();
        return "Success";
    }
}
