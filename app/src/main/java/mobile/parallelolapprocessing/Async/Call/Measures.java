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
public class Measures extends Thread{
    private Thread dataDnldThread;
    public  static Boolean isCallSucceed =false;
    @Override
    public void run() {
        try {
            MainActivity.MeasuresList = QueryProcessor.GetMeasures();
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
/*extends AsyncTask<Void,Void,HashMap<Integer,String>> {

    MainActivity mainObj;
    public  Measures(MainActivity main) {
        mainObj = main;
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
        protected void onPostExecute(HashMap<Integer, String> resultList) {
        MainActivity.MeasuresList = resultList;
        //messageHandler.sendEmptyMessage(0);
            this.cancel(true);
    }*/
}
