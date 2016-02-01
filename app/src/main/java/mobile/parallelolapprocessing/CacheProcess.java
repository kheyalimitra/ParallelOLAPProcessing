package mobile.parallelolapprocessing;

import android.os.AsyncTask;

import java.util.List;

import DataRetrieval.Cube;
import MDXQueryProcessor.MDXQProcessor;

/**
 * Created by KheyaliMitra on 1/31/2016.
 */
public class CacheProcess extends AsyncTask<MDXParameterWrapper,Void,String> {// this is for passing parameters in doInBackground method

     private String callonPostExecute(String val)
    {
        //messageHandler.sendEmptyMessage(0);
        return "Success";
    }


    @Override
    protected String doInBackground(MDXParameterWrapper... params) {
        try {
            Cube c =  new Cube(params[0].olapServerURL);
            MDXQProcessor mdxQ = new MDXQProcessor();
            List<String> inflatedQueries = mdxQ.GenerateQueryString(params[0].allAxisDetails, params[0].selectedMeasures, params[0].measureMap,
                    params[0].keyValPairsForDimension, true);
            List<List<Long>>cubeInflated = c.GetCubeData(inflatedQueries.get(0));
            mdxQ.CheckAndPopulateCache(params[0].cellOrdinalCombinations.get(0), cubeInflated);// assuming only 1 query entry

        }
        catch(Exception ex){
            String e =  ex.getMessage();
        }
        String backGroundThread = "Back ground thread";
        return callonPostExecute(backGroundThread);
    }
}
