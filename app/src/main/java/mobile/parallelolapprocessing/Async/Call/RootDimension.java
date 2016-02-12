package mobile.parallelolapprocessing.Async.Call;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import DataStructure.TreeNode;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.MainActivity;

/**
 * Created by KheyaliMitra on 1/31/2016.
 */
public class RootDimension extends AsyncTask<Void,Void,TreeNode> {
    TextView dimen;
    public  RootDimension(TextView dimensions) {
        dimen = dimensions;
    }

    private Handler messageHandler = new Handler() {

        public void handleMessage(Message msg) {
            dimen.setText(MainActivity.DimensionTreeNode.getReference().toString());
        }
    };

    @Override
    protected TreeNode doInBackground(Void... params) {
        TreeNode rootDimension= new TreeNode("Dimension");
        try {
            rootDimension = QueryProcessor.GetRootDimension();
        }
        catch (Exception ex){

        }
        return rootDimension;
    }

    protected void onPostExecute(TreeNode resultNode) {
        MainActivity.DimensionTreeNode = resultNode;
        messageHandler.sendEmptyMessage(0);
        this.cancel(true);
    }
}
