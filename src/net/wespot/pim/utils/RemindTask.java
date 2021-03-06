package net.wespot.pim.utils;

import android.util.Log;
import org.celstec.arlearn2.android.delegators.ARL;

import java.util.TimerTask;

/**
 * Created by titogelo on 26/06/14.
 */
public class RemindTask extends TimerTask{


    private static final String TAG = "Timer";

    public RemindTask() {
        Log.e(TAG,"____ Constructor :)");
        ARL.eventBus.register(this);
    }

    @Override
    public void run() {
        Log.e(TAG, "Start");
        doSomeWork();
        Log.e(TAG, "Stop");

    }

    public void doSomeWork() {
        Log.e(TAG,"____ :)");
        ARL.eventBus.post(new doSomeWorkClass());
    }


    public void onEventAsync(doSomeWorkClass sge) {

        ARL.eventBus.post(new TimeEvent());
    }

    private class doSomeWorkClass {}

}
