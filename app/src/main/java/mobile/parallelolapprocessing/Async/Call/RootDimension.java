package mobile.parallelolapprocessing.Async.Call;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import DataRetrieval.Cube;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.MainActivity;

/**
 * Created by KheyaliMitra on 1/31/2016.
 */
public class RootDimension extends Thread{//extends AsyncTask<Void,Void,TreeNode> {
    private Thread dataDnldThread;
    public  static Boolean isCallSucceed =false;
    @Override
    public void run() {
        try {
            MainActivity.DimensionTreeNode = QueryProcessor.GetRootDimension();
            isCallSucceed =true;
        }
        catch(Exception e){
            String ex =  e.getMessage();
            isCallSucceed = false;
        }

    }
    public void start ()
    {
        if (dataDnldThread == null)
        {
            dataDnldThread = new Thread (this);
            dataDnldThread.start ();


        }
    }

   /* public  RootDimension(MainActivity main) {
        mainObj = main;
    }

    private Handler messageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            //mainObj.PopulateTreeHierarchy();
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
    }*/
}
