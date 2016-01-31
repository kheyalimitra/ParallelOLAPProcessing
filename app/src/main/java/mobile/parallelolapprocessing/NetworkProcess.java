package mobile.parallelolapprocessing;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import Processor.QueryProcessor;

/**
 * Created by KheyaliMitra on 1/31/2016.
 */
public class NetworkProcess extends AsyncTask {
    TextView tView;
    public  NetworkProcess(TextView tV)
    {
        tView= tV;
    }

    private Handler messageHandler = new Handler() {

        public void handleMessage(Message msg) {
            tView.setText("From BG");
        }
    };
    protected String doInBackground(Object[] params) {
        try {
            QueryProcessor.StartActivity();
        }
        catch(Exception ex){

        }
        String backGroundThread = "Back ground thread";
        return callonPostExecute(backGroundThread);
    }
    private String callonPostExecute(String val)
    {
        messageHandler.sendEmptyMessage(0);
        return "Success";
    }
}
