package mobile.parallelolapprocessing.Async.Call;

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
public class MDXUserQuery extends AsyncTask<MDXUserQueryInput,Void, Boolean> {
    public static List<List<List<Integer>>> allAxisDetails;
    public static List<Integer> selectedMeasures;
    public static HashMap<Integer, String> measureMap;
    public static HashMap<Integer, TreeNode> keyValPairsForDimension;
    public static List<List<String>>cellOrdinalCombinations;
    private long start=0;
    private Handler messageHandler = new Handler() {

        public void handleMessage(Message msg) {
            //dimension.setText("Hierarchy downloaded");
            //MainActivity.CachedDataCubes.get(0);
            String str =TextUtils.join(", ", MainActivity.CachedDataCubes.values());
            CacheProcess cache = new CacheProcess(allAxisDetails, selectedMeasures, measureMap, keyValPairsForDimension,
                    cellOrdinalCombinations, QueryProcessor.olapServiceURL);
            //   cache.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            cache.start();

        }
    };
    @Override
    protected Boolean doInBackground(MDXUserQueryInput... params) {
        start = System.currentTimeMillis();

        if (!Log.isLoggable("MDXQueryDownload", Log.VERBOSE))
            Log.v("MyApplicationTag", "StartMDXQueryDownload started. Time: ");

        QueryProcessor qp = new QueryProcessor();
        try {
            //Standard priority of the most important display threads, for compositing the screen and retrieving input events.
            //Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
            return qp.GetUserRequestedQueryData(params[0].entryPerDimension,params[0].rootDimensionTree,params[0].hardcodedInputDim,params[0].measuresObj,
                    params[0].measureMap,params[0].hardcodedInputMeasures);
            // ... do some work C ...

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    protected void onPostExecute(Boolean result) {
        if (result) {
           messageHandler.sendEmptyMessage(0);

            if (!Log.isLoggable("MDXQueryDownload", Log.VERBOSE)) {
                long elapsedTime = System.currentTimeMillis() - start;
                Log.v("MyApplicationTag", "EndMDXQueryDownloaded. Time elapsed: "+ elapsedTime);
            }
            this.cancel(true);
       }
    }
}
