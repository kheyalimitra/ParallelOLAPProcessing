package mobile.parallelolapprocessing.Async.Call;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Objects;

import DataStructure.TreeNode;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.ParameterWrapper.DimensionHierarchyInput;
import mobile.parallelolapprocessing.MainActivity;

/**
 * Created by KheyaliMitra on 1/31/2016.
 */
public class DimensionHierarchy extends AsyncTask<Object,Void, TreeNode>{

    TextView dimension;
    public  DimensionHierarchy(TextView dimension) {
        this.dimension = dimension;
    }
    private Handler messageHandler = new Handler() {

        public void handleMessage(Message msg) {
            dimension.setText("Hierarchy downloaded");
        }
    };
    @Override
    protected TreeNode doInBackground(Object... params) {
        try {
            String selection = (String)params[0];
            return  QueryProcessor.GetHierarchyDimension(MainActivity.DimensionTreeNode,selection);
        }
        catch (Exception ex)
        {

        }
        return null;
    }
    protected void onPostExecute(TreeNode resultTree) {
        MainActivity.DimensionTreeNode = resultTree;
        messageHandler.sendEmptyMessage(0);
        this.cancel(true);
    }
}
