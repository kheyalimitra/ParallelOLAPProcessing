package mobile.parallelolapprocessing.Async.Call;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import java.util.HashMap;

import Processor.QueryProcessor;
import mobile.parallelolapprocessing.MainActivity;

/**
 * Created by KheyaliMitra on 1/31/2016.
 */
public class Measures extends AsyncTask<Void,Void,HashMap<Integer,String>> {

    TextView measures;
    public  Measures(TextView measures) {
        this.measures = measures;
    }
    @Override
    protected HashMap<Integer, String> doInBackground(Void... params) {
        try {
            return  QueryProcessor.GetMeasures();
        }
        catch (Exception ex)
        {

        }
        return null;
    }



    private Handler messageHandler = new Handler() {

        public void handleMessage(Message msg) {
            measures.setText(MainActivity.MeasuresList.get(0));
        }
    };

        protected void onPostExecute(HashMap<Integer, String> resultList) {
        MainActivity.MeasuresList = resultList;
        messageHandler.sendEmptyMessage(0);
    }
}
