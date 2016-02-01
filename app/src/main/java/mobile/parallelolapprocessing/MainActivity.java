package mobile.parallelolapprocessing;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;

import DataStructure.TreeNode;
import mobile.parallelolapprocessing.Async.Call.DimensionHierarchy;
import mobile.parallelolapprocessing.Async.Call.Measures;
import mobile.parallelolapprocessing.Async.Call.RootDimension;

public class MainActivity extends Activity {
    public static HashMap<Integer,String> MeasuresList;
    public static TreeNode DimensionTreeNode;
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
        new DimensionHierarchy(t).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selection);
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
