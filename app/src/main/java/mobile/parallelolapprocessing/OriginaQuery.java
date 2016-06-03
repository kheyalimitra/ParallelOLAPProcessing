package mobile.parallelolapprocessing;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import DataRetrieval.Cube;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;

/**
 * Created by KheyaliMitra on 6/1/2016.
 */
public class OriginaQuery implements Runnable{

    private String userQuery;
    public static boolean isDownloadFinished =  false;
    public OriginaQuery(String userQuery)
    {
        this.userQuery = userQuery;
    }

    public void run() {
        try {

            Cube c =  new Cube(QueryProcessor.olapServiceURL);
            MDXQProcessor mdxQ = new MDXQProcessor();
            List<List<Long>>cubeOriginal = c.GetCubeData(userQuery);
            Log.d("Original Query", "download ends " + String.valueOf(System.currentTimeMillis()));
            mdxQ.CheckAndPopulateCache(MDXUserQuery.cellOrdinalCombinations.get(0), new ArrayList<List<TreeNode>>(), cubeOriginal, true);// assuming only 1 query entry
            isDownloadFinished = true;
            Log.d("Original Query", "Query process ends " + String.valueOf(System.currentTimeMillis()));
            Log.d("Original Query","MDX query:"+ String.valueOf(userQuery));
        }
        catch(Exception e) {
            String ex = e.getMessage();
        }
    }
}
