package mobile.parallelolapprocessing.Async.Call;

import android.app.Application;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import DataStructure.TreeNode;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.CacheProcessUpto1Level;
import mobile.parallelolapprocessing.Async.ParameterWrapper.MDXUserQueryInput;
import mobile.parallelolapprocessing.CacheProcess;
import mobile.parallelolapprocessing.DimensionTree;
import mobile.parallelolapprocessing.MainActivity;
import mobile.parallelolapprocessing.OriginaQuery;

/**
 * Created by KheyaliMitra on 2/1/2016.
 */
public class MDXUserQuery implements Runnable{//extends AsyncTask<MDXUserQueryInput,Void, Boolean> {//implements Runnable
    public static List<List<List<Integer>>> allAxisDetails;
    public static List<Integer> selectedMeasures;
    public static HashMap<Integer, String> measureMap;
    public static HashMap<Integer, TreeNode> keyValPairsForDimension;
    public static List<List<String>>cellOrdinalCombinations;
    private Thread inflatedDataDnldThread;
    private MDXUserQueryInput MDXQObj;
    public  static boolean isComplete=false;
   public MDXUserQuery(MDXUserQueryInput obj)
   {
       this.MDXQObj = obj;
   }

   @Override
    public void run() {
        QueryProcessor qp = new QueryProcessor();
        try {
            //Standard priority of the most important display threads, for compositing the screen and retrieving input events.

            Log.d("Original Query", "Query process starts " + String.valueOf(System.currentTimeMillis()));
             qp.GetUserRequestedData(MDXQObj.entryPerDimension, MDXQObj.rootDimensionTree, MDXQObj.DimensionInput, MDXQObj.measuresObj,
                     MDXQObj.measureMap, MDXQObj.measureInput);

            isComplete = true;
            // remove this when done
           // new DimensionTree().executeSerially();

        } catch (Exception e) {
            e.printStackTrace();
            isComplete = false;

        }

    }

    public void start ()
    {
        if (inflatedDataDnldThread == null)
        {
            inflatedDataDnldThread =new Thread (this);
            inflatedDataDnldThread.start ();

        }
    }

   // @Override
    protected Boolean doInBackground(MDXUserQueryInput... params) {
        this.run();
        return true;
    }
}
