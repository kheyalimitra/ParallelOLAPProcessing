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
    public  static Boolean isHierarchyCall ;

    public  RootDimension() {
        isHierarchyCall =false;
    }

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


}
