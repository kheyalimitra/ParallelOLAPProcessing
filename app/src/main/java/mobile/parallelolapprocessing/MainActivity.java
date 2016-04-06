package mobile.parallelolapprocessing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.redisson.Redisson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

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
    public static HashMap<String, HashMap<Integer, Long>> CachedDataCubes = new HashMap();
    public static HashMap<String,List<Long>> ThreadProcesshingDetails = new HashMap<>();
    public static Context MainContext;
    //Class for
    private class SimpleArrayAdapter extends ArrayAdapter<String> {
        public SimpleArrayAdapter(Context context, List<String> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //List view entry for first screen
        final LinkedHashMap<String, Class<?>> listItems = new LinkedHashMap<>();
        //First screen Output for List window
        listItems.put("Get Dimensions and Measures", DimensionTree.class);

        ///Display data in Text view and List View
        final AlertDialog ad = new AlertDialog.Builder(this).create();
        final Activity activityObj = this;
        MainContext = activityObj;
        //Put list into string list
        final List<String> list = new ArrayList(listItems.keySet());

        //Activity control in first screens ActivityMain.xml
        final ListView listview = (ListView) findViewById(R.id.listview);

        //Initialize array Adpater
        final SimpleArrayAdapter adapter = new SimpleArrayAdapter(this, list);
        //Sets Adapter
        listview.setAdapter(adapter);

        //Onclick of List view
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            //btn.setOnClickListener(new View.OnClickListener() {

                                            /**
                                             * Populates JSON data into view
                                             */
                                            @Override
                                            // public void onClick(View arg0) {
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                try {
                                                    Intent i = new Intent(MainActivity.this, TreeViewActivity.class);
                                                    Class<?> clazz = listItems.values().toArray(new Class<?>[]{})[position];
                                                    i.putExtra(TreeViewActivity.View_PARAM, clazz);
                                                    MainActivity.this.startActivity(i);


                                                //getDimensionAndMeasures();
                                                } catch (Exception e) {
                                                    String s = e.getMessage();
                                                }
                                            }
                                        }
        );

}

    public void getDimensionAndMeasures() throws ExecutionException, InterruptedException {
        MainActivity.ThreadProcesshingDetails =  new HashMap<>();
        List<Long> threadProcessDimen = new ArrayList<>();
        List<Long> threadProcessMeasure = new ArrayList<>();
        threadProcessDimen.add(System.currentTimeMillis());
        threadProcessMeasure.add(System.currentTimeMillis());
        RootDimension rootDimObj= new RootDimension();
        Measures measObj =  new Measures();
        try {
            rootDimObj.join();
            rootDimObj.start();
            while(!RootDimension.isCallSucceed){
                Thread.sleep(10);
            }
        }
        catch (Exception e){

        }
        finally {
            rootDimObj =null;
            threadProcessDimen.add(System.currentTimeMillis());
            ThreadProcesshingDetails.put("Main_Dimen_Sync", threadProcessDimen);
        }
        try{
            measObj.join();
            measObj.start();
            while (!Measures.isCallSucceed){
                Thread.sleep(10);
            }
        }
        catch (Exception e){

        }
        finally{
            measObj =null;
            threadProcessMeasure.add(System.currentTimeMillis());
            ThreadProcesshingDetails.put("Main_Measure_Sync", threadProcessMeasure);
        }

    }
    public com.unnamed.b.atv.model.TreeNode PopulateTreeHierarchy() {
        com.unnamed.b.atv.model.TreeNode dRoot = new com.unnamed.b.atv.model.TreeNode("Dimension/Hierarchy:");
        com.unnamed.b.atv.model.TreeNode dimRoot = new com.unnamed.b.atv.model.TreeNode("Dimension");

        try {

            ArrayList<String> outerList = new ArrayList<String>();
            /// sort map table using treemap
            List<TreeNode>dimensionTree = this.DimensionTreeNode.getChildren();

            Iterator it = dimensionTree.iterator();
            while (it.hasNext()) {
                TreeNode node =(TreeNode) it.next();
                com.unnamed.b.atv.model.TreeNode t = new com.unnamed.b.atv.model.TreeNode(node.getReference());
                outerList.add((String)node.getReference());
                if (node.getChildren().size() > 1) {
                    List<TreeNode> innerchild = (List<TreeNode>) node.getChildren();
                    Iterator inner = innerchild.iterator();
                    while (inner.hasNext()) {
                        TreeNode child = (TreeNode) inner.next();

                        com.unnamed.b.atv.model.TreeNode c = new com.unnamed.b.atv.model.TreeNode(child.getReference());
                        t.addChild(c);

                    }
                }

                dRoot.addChild(t);
            }

        } catch (Exception e) {
            String s = e.getMessage();
        }
        dimRoot.addChild(dRoot);
        return dimRoot;
    }

    public com.unnamed.b.atv.model.TreeNode PopulateMeasures() {
        com.unnamed.b.atv.model.TreeNode mRoot = new com.unnamed.b.atv.model.TreeNode("Measures:");
        com.unnamed.b.atv.model.TreeNode mesRoot = new com.unnamed.b.atv.model.TreeNode("Measures");
        List<String> mesList = new ArrayList<>(MeasuresList.values());
        //sort tree.
        Collections.sort(mesList);
        try {

            Iterator itr = mesList.iterator();
            while (itr.hasNext()) {
                String key = (String) itr.next();
                com.unnamed.b.atv.model.TreeNode t = new com.unnamed.b.atv.model.TreeNode(key);
                mRoot.addChild(t);
            }
        }
        catch (Exception e)

        {

        }
        mesRoot.addChild(mRoot);
        return mesRoot;
    }




    public void fetchHierarchyRecordsFromServer(String param) throws InterruptedException {
        DimensionHierarchy dimenHierarchyObj = new DimensionHierarchy(param);//.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, param);
        // RootDimension rootObj = new RootDimension(param,true);
        try {
            //RootDimension.isCallSucceed =false;
            //rootObj.join();
            //rootObj.start();
            // wait for asynchronous call to fetch the records.
            dimenHierarchyObj.join();
            dimenHierarchyObj.start();
            // we have to make it sleep for certain amount of time so that
            // data gets time to loaded from server from another thread.
            // else it will throw exception
            Thread.sleep(900);
            while (!DimensionHierarchy.isCallSucceed) {
             Thread.sleep(10);

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            dimenHierarchyObj = null;
        }

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
