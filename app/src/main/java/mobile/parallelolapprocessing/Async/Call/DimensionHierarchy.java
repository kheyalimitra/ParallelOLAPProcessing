package mobile.parallelolapprocessing.Async.Call;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Objects;

import DataRetrieval.Dimension;
import DataStructure.TreeNode;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.ParameterWrapper.DimensionHierarchyInput;
import mobile.parallelolapprocessing.DimensionTree;
import mobile.parallelolapprocessing.MainActivity;

/**
 * Created by KheyaliMitra on 1/31/2016.
 */
public class DimensionHierarchy extends Thread {//extends AsyncTask<Object,Void, TreeNode>{

    String selection;

    public DimensionHierarchy(String param) {
        this.selection = param;
    }

    private Thread dataDnldThread;
    public static Boolean isCallSucceed = false;

    @Override
    public void run() {
        try {
            MainActivity.DimensionTreeNode = QueryProcessor.GetHierarchyDimension(MainActivity.DimensionTreeNode, selection);
            isCallSucceed = true;
        } catch (Exception e) {
            String ex = e.getMessage();
            isCallSucceed = false;
        }


    }

    public void start() {
        if (dataDnldThread == null) {
            dataDnldThread = new Thread(this);
            dataDnldThread.start();
        }
    }
}
