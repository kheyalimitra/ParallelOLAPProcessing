package mobile.parallelolapprocessing;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.redisson.Redisson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import DataStructure.TreeNode;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.Call.DimensionHierarchy;
import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;
import mobile.parallelolapprocessing.Async.Call.Measures;
import mobile.parallelolapprocessing.Async.Call.RootDimension;
import mobile.parallelolapprocessing.Async.ParameterWrapper.MDXUserQueryInput;

public class MainActivity extends Activity {
    public static HashMap<Integer,String> MeasuresList;
    public static TreeNode DimensionTreeNode;
    public static HashMap<String, HashMap<Long, Long>> CachedDataCubes = new HashMap();
    //public static Redisson CachedDataCubes1 = Redisson.create();
    //public static ConcurrentMap<String,ConcurrentMap<Long, Long>> cachedDataMap = CachedDataCubes1.getMap("cachedData");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onAsyncClick(View v) {
        TextView dimension= (TextView) findViewById(R.id.textView1);
        TextView measures= (TextView) findViewById(R.id.textView2);
        // for dimensions
        new RootDimension(dimension).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        // for measures
        new Measures(measures).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void onGetHierarchyClick(View v) {

        TextView t= (TextView) findViewById(R.id.textView1);
        String selection="[Dimension].[Account].[Account Number]";
        new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selection);
        selection="[Dimension].[Account].[Account Type]";
        new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selection);
        selection="[Dimension].[Customer].[Country]";
        new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selection);
        selection="[Dimension].[Geography].[Country]";
        new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selection);
        selection="[Dimension].[Geography].[Geography]";
        new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selection);
        selection="[Dimension].[Product].[Color]";
        new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selection);
        selection="[Dimension].[Employee].[Gender]";
        new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selection);
        selection="[Dimension].[Date].[Calendar Quarter of Year]";
        new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selection);
        selection="[Dimension].[Customer].[Education]";
        new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selection);
        selection="[Dimension].[Date].[Calendar]";
        new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selection);

        //not working
       // selection="[Dimension].[Geography].[Geography].[All Geographies].[Australia]";
        //not working
        //selection="[Dimension].[Date].[Calendar].[All Periods].[CY 2006]";
        //new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selection);
    }
    public void onDimensionAndMeasureSelectionClick(View v)
    {
        TextView t= (TextView) findViewById(R.id.textView1);
        List<String> hardcodedInputDim = new ArrayList<String>();
       // hardcodedInputDim.add("[Dimension].[Geography].[Geography].[All Geographies].[Australia]");not working
        hardcodedInputDim.add("[Dimension].[Date].[Calendar].[All Periods]");
        hardcodedInputDim.add("[Dimension].[Customer].[Education].[All Customers].[Bachelors]");
       // hardcodedInputDim.add("[Dimension].[Customer].[Country].[All Customers]");

        //hardcodedInputDim.add("[Dimension].[Geography].[Geography]");
        //hardcodedInputDim.add("[Dimension].[Account].[Account Type].[All Accounts].[Revenue]");
        //hardcodedInputDim.add("[Dimension].[Account].[Account Type].[All Accounts].[Expenditures]");
        //hardcodedInputDim.add("[Dimension].[Date].[Calendar Quarter of Year]");
        //hardcodedInputDim.add("[Dimension].[Employee].[Gender].[All Employees].[Male]");
        //hardcodedInputDim.add("[Dimension].[Employee].[Gender].[All Employees].[Female]");
        //this is hardcoded now should come from user input
        List<String> hardcodedInputMeasures  = new ArrayList<String>();
		hardcodedInputMeasures.add("Internet Sales Amount");
        hardcodedInputMeasures.add("Internet Order Count");
        int [] entryPerDimension ={1,1} ;
        DataRetrieval.Measures measuresObj = new DataRetrieval.Measures(QueryProcessor.olapServiceURL);//pass url
       // MDXParameterWrapper mdxWrapper = new MDXParameterWrapper(entryPerDimension, MainActivity.DimensionTreeNode,hardcodedInputDim,
          //      measuresObj,MainActivity.MeasuresList)
        MDXUserQueryInput mdxInputObj = new MDXUserQueryInput(entryPerDimension, MainActivity.DimensionTreeNode,hardcodedInputDim,
        measuresObj,MainActivity.MeasuresList,hardcodedInputMeasures) ;
        new MDXUserQuery().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,mdxInputObj);
        //new CacheProcess().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
