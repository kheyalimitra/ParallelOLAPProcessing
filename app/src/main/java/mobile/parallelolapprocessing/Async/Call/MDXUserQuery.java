package mobile.parallelolapprocessing.Async.Call;

import android.app.Application;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
import DataStructure.TreeNode;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.ParameterWrapper.MDXUserQueryInput;
import mobile.parallelolapprocessing.CacheProcess;
import mobile.parallelolapprocessing.MainActivity;

/**
 * Created by KheyaliMitra on 2/1/2016.
 */
public class MDXUserQuery implements Runnable {//extends AsyncTask<MDXUserQueryInput,Void, Boolean> {
    public static List<List<List<Integer>>> allAxisDetails;
    public static List<Integer> selectedMeasures;
    public static HashMap<Integer, String> measureMap;
    public static HashMap<Integer, TreeNode> keyValPairsForDimension;
    public static List<List<String>>cellOrdinalCombinations;
    private long start=0;
    private Thread inflatedDataDnldThread;
    private MDXUserQueryInput MDXQObj;
    public  static boolean isComplete=false;
    public static boolean isNewQuery = false;
   public MDXUserQuery(MDXUserQueryInput obj)
   {
       this.MDXQObj = obj;
   }

    @Override
    public void run() {
        start = System.currentTimeMillis();

        if (!Log.isLoggable("MDXQueryDownload", Log.VERBOSE))
            Log.v("MyApplicationTag", "StartMDXQueryDownload started. Time: ");

        QueryProcessor qp = new QueryProcessor();
        try {
            //Standard priority of the most important display threads, for compositing the screen and retrieving input events.
            //Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
             qp.GetUserRequestedQueryData(MDXQObj.entryPerDimension,MDXQObj.rootDimensionTree,MDXQObj.DimensionInput,MDXQObj.measuresObj,
                    MDXQObj.measureMap,MDXQObj.measureInput);
            isComplete = true;


            // ... do some work C ...

        } catch (Exception e) {
            e.printStackTrace();
            isComplete = false;

        }
    }
    public void start ()
    {
        if (inflatedDataDnldThread == null)
        {
            inflatedDataDnldThread = new Thread (this);
            inflatedDataDnldThread.start ();
            start = System.currentTimeMillis();
            try {
                CacheProcess cache = new CacheProcess(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
                        MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL);
                //cache.start();
                cache.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            catch(Exception e)
            {

            }
        }
    }

}
