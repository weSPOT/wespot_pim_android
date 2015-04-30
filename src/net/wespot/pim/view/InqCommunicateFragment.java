package net.wespot.pim.view;

/**
 * ****************************************************************************
 * Copyright (C) 2013-2015 Open Universiteit Nederland
 * <p/>
 * This library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * Contributors: Angel Suarez
 * ****************************************************************************
 */

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import daoBase.DaoConfiguration;
import net.wespot.pim.MainActivity;
import net.wespot.pim.R;
import net.wespot.pim.SplashActivity;
import net.wespot.pim.controller.Adapters.ChatAdapter;
import net.wespot.pim.controller.InquiryPhasesActivity;
import org.celstec.arlearn.delegators.INQ;
import org.celstec.arlearn2.android.delegators.ARL;
import org.celstec.arlearn2.android.events.MessageEvent;
import org.celstec.arlearn2.android.gcm.NotificationListenerInterface;
import org.celstec.dao.gen.*;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

public class InqCommunicateFragment extends Fragment implements NotificationListenerInterface, AbsListView.OnScrollListener {

    private static final String TAG = "InqCommunicateFragment";
    private static final String NUMBER = "0123";
    public static final String INQUIRY_ID = "runId";
    private EditText message;
    private ImageButton send;
    private Button show_more_messages;
    private View rootView;

    private static ListView listViewMessages;

    private static Context mContext;

    private static int numMessages = 0;
    private static NotificationCompat.Builder mBuilder;
    private static NotificationManager mNotificationManager;
    private static NotificationCompat.InboxStyle mNotificationStyle;
    private static int limit;

    private HashMap<String, AccountLocalObject> accounts = new HashMap<String, AccountLocalObject>();

    public static ChatAdapter chatAdapter;
    public static HashMap<MessageLocalObject, View> messages_views = new HashMap<MessageLocalObject, View>();
    public static ArrayList<MessageLocalObject> messages = new ArrayList<MessageLocalObject>();
    public static ArrayList<MessageLocalObject> notications_queue_messages = new ArrayList<MessageLocalObject>();

    private List<MessageLocalObject> messageLocalObjectList;
    private static List<Long> runIdList = new ArrayList<Long>();


    private static final int LIMIT_INQUIRIES = 10;
    private static final String CURRENT_INQUIRY = "currentInquiry";
    private static final String CURRENT_INQUIRY_RUN = "currentInquiryRun";
    private static final String CURRENT_MESSAGES = "currentMessages";

    public InqCommunicateFragment() {
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(CURRENT_INQUIRY, INQ.inquiry.getCurrentInquiry().getId());
        outState.putLong(CURRENT_INQUIRY_RUN, INQ.inquiry.getCurrentInquiry().getRunLocalObject().getId());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();

        if (savedInstanceState != null) {
            INQ.init(mContext);
            ARL.eventBus.register(this);
            INQ.accounts.syncMyAccountDetails();

            INQ.inquiry.setCurrentInquiry(DaoConfiguration.getInstance().getInquiryLocalObjectDao().load(
                    savedInstanceState.getLong(CURRENT_INQUIRY)));

            INQ.inquiry.getCurrentInquiry().setRunLocalObject(DaoConfiguration.getInstance().getRunLocalObjectDao().load(
                    savedInstanceState.getLong(CURRENT_INQUIRY_RUN) ));

            Log.e(TAG, "recovery from InqCommunicateFragment");
        }

        INQ.messages.syncMessagesForDefaultThread(INQ.inquiry.getCurrentInquiry().getRunId());

        numMessages = 0;
        mBuilder = null;
        mNotificationStyle = null;
        mNotificationManager = null;
        runIdList.clear();
        notications_queue_messages.clear();

        long two_days_ago_long = (System.currentTimeMillis()/1000 - (36 * 60 * 60))*1000;


        long twelve_hours = 0;
        long now_long = System.currentTimeMillis();
        Date two_days_ago = new Date(two_days_ago_long * 1000);
        Date now = new Date(now_long);

        Format format = new SimpleDateFormat("HH:mm:ss dd-MMM-y");

//        messageLocalObjectList = DaoConfiguration.getInstance().getMessageLocalObject().queryBuilder()
//                .where(MessageLocalObjectDao.Properties.RunId.eq(INQ.inquiry.getCurrentInquiry().getRunId()),
//                        MessageLocalObjectDao.Properties.Time.gt(two_days_ago_long))
//                .orderAsc(MessageLocalObjectDao.Properties.Time)
//                .list();

        int number_messages = DaoConfiguration.getInstance().getMessageLocalObject().queryBuilder()
                .where(MessageLocalObjectDao.Properties.RunId.eq(INQ.inquiry.getCurrentInquiry().getRunId()))
                .list().size();

        limit = 10;

        if (number_messages != 0) {

            do {
                ///////////////////////////////////////////
                // Request last messages extending the date
                ///////////////////////////////////////////
                twelve_hours = (System.currentTimeMillis() / 1000 - (limit * 60 * 60)) * 1000;

                messageLocalObjectList = DaoConfiguration.getInstance().getMessageLocalObject().queryBuilder()
                        .where(MessageLocalObjectDao.Properties.RunId.eq(INQ.inquiry.getCurrentInquiry().getRunId()),
                                MessageLocalObjectDao.Properties.Time.gt(twelve_hours))
                        .orderAsc(MessageLocalObjectDao.Properties.Time)
                        .list();

                limit *= 2;
            } while (messageLocalObjectList.size() == 0);

            //        lastTimeStamp = messageLocalObjectList.get(messageLocalObjectList.size()-1).getTime();


            for (MessageLocalObject messageLocalObject : messageLocalObjectList) {
                messages.add(messageLocalObject);
                messageLocalObject.setRead(true);
                DaoConfiguration.getInstance().getMessageLocalObject().insertOrReplace(messageLocalObject);
            }
        }

        chatAdapter = new ChatAdapter(getActivity(), messages, messages_views);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        rootView = inflater.inflate(R.layout.fragment_section_communicate, container, false);

        message = (EditText) rootView.findViewById(R.id.communication_enter_message);
        send = (ImageButton) rootView.findViewById(R.id.communication_enter_message_button);
        show_more_messages = (Button) rootView.findViewById(R.id.button_show_more_messages);

        show_more_messages.setText("Show more messages");
        show_more_messages.setVisibility(View.INVISIBLE);


        listViewMessages = (ListView) rootView.findViewById(R.id.list_messages);
        listViewMessages.setAdapter(chatAdapter);
        listViewMessages.setOnScrollListener(this);

        show_more_messages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show_more_messages.setVisibility(View.INVISIBLE);

                long last_timestamp_message = (System.currentTimeMillis() / 1000 - (limit * 60 * 60)) * 1000;


                limit *= 2;

                long next_timestamp_message = (System.currentTimeMillis() / 1000 - (limit * 60 * 60)) * 1000;


                messageLocalObjectList = DaoConfiguration.getInstance().getMessageLocalObject().queryBuilder()
                        .where(MessageLocalObjectDao.Properties.RunId.eq(INQ.inquiry.getCurrentInquiry().getRunId()),
                                MessageLocalObjectDao.Properties.Time.gt(next_timestamp_message),
                                MessageLocalObjectDao.Properties.Time.lt(last_timestamp_message))
                        .orderAsc(MessageLocalObjectDao.Properties.Time)
                        .list();

                for (MessageLocalObject messageLocalObject : messageLocalObjectList) {
                    messages.add(0, messageLocalObject);
//                    chatAdapter.notifyDataSetChanged();
//                    messageLocalObject.setRead(true);
//                    DaoConfiguration.getInstance().getMessageLocalObject().insertOrReplace(messageLocalObject);
                }




//                chatAdapter.notifyDataSetInvalidated();

//                int position = listViewMessages.getSelectedItemPosition();
//                listViewMessages.setSelection(position);

                // save index and top position
//                int index = listViewMessages.getFirstVisiblePosition();
//                View va = listViewMessages.getChildAt(0);
//                int top = (va == null) ? 0 : va.getTop();
//
//                // notify dataset changed or re-assign adapter here
                chatAdapter.notifyDataSetChanged();
//
//                // restore the position of listview
//                listViewMessages.setSelectionFromTop(index, top);
//
//
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!message.getText().toString().equals("")) {
                    MessageLocalObject messageLocalObject = new MessageLocalObject();
                    messageLocalObject.setBody(message.getText().toString());
                    messageLocalObject.setAuthor(INQ.accounts.getLoggedInAccount().getFullId());
                    messageLocalObject.setTime(INQ.time.getServerTime());
                    messageLocalObject.setRunId(INQ.inquiry.getCurrentInquiry().getRunId());
                    messageLocalObject.setSynced(false);
                    messageLocalObject.setSubject("");

                    DaoConfiguration.getInstance().getMessageLocalObject().insertOrReplace(messageLocalObject);
                    INQ.messages.postMessagesToServer();

                    messages.add(messageLocalObject);
                    messages_views.put(messageLocalObject, null);

                    message.setText("");
                    chatAdapter.notifyDataSetChanged();
                }
            }
        });
        return rootView;
    }


    @Override
    public boolean acceptNotificationType(String notificationType) {
        return true;
    }

    @Override
    public void handleNotification(HashMap<String, String> map) {
        if (map.containsKey("type")) {
            if (map.containsValue("org.celstec.arlearn2.beans.notification.MessageNotification")) {
                Log.i(TAG, "retrieve messages");
                if (!ARL.eventBus.isRegistered(this)){
                    ARL.eventBus.register(this);
                }
                Long runId = Long.parseLong(map.get("runId"));
                INQ.runs.syncRun(runId);
                INQ.messages.syncMessagesForDefaultThread(runId);
            }
        }
    }

    public synchronized void onEventMainThread(MessageEvent messageEvent) {
        Log.d(TAG, "message synced: " + messageEvent.getRunId());

        receiveMessage(messageEvent.getRunId());
    }

    public void receiveMessage(Long runId) {

        long two_days_ago_long = (System.currentTimeMillis()/1000 - (24 * 60 * 60))*1000;

        messageLocalObjectList = DaoConfiguration.getInstance().getMessageLocalObject().queryBuilder()
                .where(MessageLocalObjectDao.Properties.RunId.eq(runId),
                        MessageLocalObjectDao.Properties.Read.isNull(),
                        MessageLocalObjectDao.Properties.Time.gt(two_days_ago_long))
                .orderAsc(MessageLocalObjectDao.Properties.Time)
                .list();

        ////////////////////////////////////
        // First check if the PIM is running
        ////////////////////////////////////
        if (isPimRunning()){

            //////////////////////////////////////////
            // If there is no INQ.inquiry we create it
            //////////////////////////////////////////
            if (INQ.inquiry.getCurrentInquiry() == null){

                InquiryLocalObject inquiryLocalObject = DaoConfiguration.getInstance().getInquiryLocalObjectDao().queryBuilder().where(
                    InquiryLocalObjectDao.Properties.RunId.eq(runId)
                ).list().get(0);

                if (!inquiryLocalObject.equals(null)) {
                    INQ.inquiry.setCurrentInquiry(inquiryLocalObject);
                }
            }

            /////////////////////////////////////////////////////
            // If the current run is the same as the that belongs
            // to the message display messages
            /////////////////////////////////////////////////////
            if (INQ.inquiry.getCurrentInquiry().getRunId() == runId){

                for (MessageLocalObject messageLocalObject : messageLocalObjectList) {
                    /////////////////////////////////////////////////////
                    // For those messages that are not shown process them
                    /////////////////////////////////////////////////////
                    if (messageLocalObject.getRead() == null) {
                        // No notification but we need to update the view
                        ///////////////////////////////////////////////////////////////////////
                        // We are inside the inquiry but maybe we need an internal notification
                        ///////////////////////////////////////////////////////////////////////
                        if (!messages_views.containsKey(messageLocalObject)) {
                            if (messages != null) {
                                messages.add(messageLocalObject);
                                messages_views.put(messageLocalObject, null);
                            }
                        }
//                        }else{
//                            if (chatAdapter != null){
//                                ViewGroup viewUpdate = (ViewGroup) chatAdapter.getViewFromMessage(messageLocalObject);
//                                if (viewUpdate != null){
//                                    viewUpdate.inflate(mContext,R.layout.entry_messages, null);
//                                    viewUpdate.invalidate();
//                                }
//                            }
//                        }

                        if (chatAdapter != null){
                            chatAdapter.notifyDataSetChanged();
                            messageLocalObject.setRead(true);
                            DaoConfiguration.getInstance().getMessageLocalObject().insertOrReplace(messageLocalObject);
                        }
                    }
                }
            }else{
                for (MessageLocalObject messageLocalObject : messageLocalObjectList) {

                    if (!notications_queue_messages.contains(messageLocalObject)){
                        notications_queue_messages.add(messageLocalObject);
                        createNotification(messageLocalObject, runId);
                    }
                }
            }
        }else{
            for (MessageLocalObject messageLocalObject : messageLocalObjectList) {
                if (!notications_queue_messages.contains(messageLocalObject)){
                    notications_queue_messages.add(messageLocalObject);
                    createNotification(messageLocalObject, runId);
                }
            }
        }
    }

    private void createNotification(MessageLocalObject messageLocalObject, Long runId) {

        if (!runIdList.contains(runId)){
            runIdList.add(runId);
        }

        numMessages = 0;
        mBuilder = null;
        mNotificationStyle = null;
        mNotificationManager = null;

        mNotificationManager = (NotificationManager) ARL.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationStyle = new NotificationCompat.InboxStyle();
        mBuilder = new NotificationCompat.Builder(ARL.getContext()).setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .setSortKey("0")
                .setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE)
                .setStyle(mNotificationStyle);

        InquiryLocalObject inquiryLocalObject = DaoConfiguration.getInstance().getInquiryLocalObjectDao().queryBuilder().where(
                InquiryLocalObjectDao.Properties.RunId.eq(runId)
        ).list().get(0);


        Intent resultIntent = new Intent(ARL.getContext(), InquiryPhasesActivity.class);
        resultIntent.putExtra(INQUIRY_ID, inquiryLocalObject.getId());

        Intent parent = new Intent(ARL.getContext(), PimInquiriesFragment.class);
        Intent parent1 = new Intent(ARL.getContext(), MainActivity.class);
        Intent parent2 = new Intent(ARL.getContext(), SplashActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(ARL.getContext());
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addNextIntentWithParentStack(parent2);
        stackBuilder.addNextIntentWithParentStack(parent1);
        stackBuilder.addNextIntentWithParentStack(parent);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);


        if (runIdList.size() == 1){
            // More than 1 inquiries have messages

            mBuilder.setContentTitle(inquiryLocalObject.getTitle());
            for (MessageLocalObject me : notications_queue_messages){
                mNotificationStyle
                        .addLine(getNameUser(me.getAuthor()) + ": " + me.getBody())
                        .setSummaryText(++numMessages!= 1 ? numMessages + " new messages" : numMessages + " new message");
            }
        }

        if (runIdList.size() > 1){
            // More than 1 inquiries have messages

            for (MessageLocalObject me : notications_queue_messages){
                mBuilder.setContentTitle("Personal Inquiry Manager");
                InquiryLocalObject a = DaoConfiguration.getInstance().getInquiryLocalObjectDao().queryBuilder().where(
                        InquiryLocalObjectDao.Properties.RunId.eq(me.getRunId())
                ).list().get(0);

                mNotificationStyle
                        .addLine(getNameUser(me.getAuthor()) + " @ "+a.getTitle()+": " + me.getBody())
                        .setSummaryText(++numMessages != 1 ? numMessages + " new messages from "+runIdList.size()+" conversations" +
                                " " : numMessages + " new message from "+runIdList.size()+" conversations");
            }
        }

        mNotificationManager.notify(Integer.parseInt(NUMBER), mBuilder.build());
    }


    public boolean isPimRunning(){
        ActivityManager activityManager = (ActivityManager) ARL.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> services = activityManager
                .getRunningTasks(Integer.MAX_VALUE);
        boolean isActivityFound = false;

        if (services.get(0).topActivity.getPackageName().toString()
                .equalsIgnoreCase(ARL.getContext().getPackageName().toString())) {
            return true;
        }
        else{
            return false;
        }
    }



    public String getNameUser(String author) {
        if (accounts.containsKey(author)){
            return accounts.get(author).getName();
        }else {
            List<AccountLocalObject> list = DaoConfiguration.getInstance().getAccountLocalObjectDao().queryBuilder()
                    .where(AccountLocalObjectDao.Properties.FullId.eq(author)).list();

            if (list.size() > 0){
                if (list.get(0) != null){
                    accounts.put(author, list.get(0) );
                    return list.get(0).getName();
                }else{
                    return author;
                }
            }else{
                return author;
            }
        }
    }

    @Override
    public void onDestroy() {
        ARL.eventBus.unregister(this);
        mBuilder = null;
        messages.clear();
        messages_views.clear();
        super.onDestroy();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        Log.e(TAG, scrollState+" state ");
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//        Log.e(TAG, "first: "+firstVisibleItem+" visibleitemcount:"+visibleItemCount+" total:"+totalItemCount);
//
        show_more_messages.setVisibility(View.VISIBLE);


//        if (firstVisibleItem == 0 && visibleItemCount != 0){
////            show_more_messages.setVisibility(View.VISIBLE);
//
//
//            /////////
//            // Option 1
//            ///////
//            long last_timestamp_message = (System.currentTimeMillis() / 1000 - (limit * 60 * 60)) * 1000;
//
//
//            limit += 12;
//
//            long next_timestamp_message = (System.currentTimeMillis() / 1000 - (limit * 60 * 60)) * 1000;
//
//
//            messageLocalObjectList = DaoConfiguration.getInstance().getMessageLocalObject().queryBuilder()
//                    .where(MessageLocalObjectDao.Properties.RunId.eq(INQ.inquiry.getCurrentInquiry().getRunId()),
//                            MessageLocalObjectDao.Properties.Time.gt(next_timestamp_message),
//                            MessageLocalObjectDao.Properties.Time.lt(last_timestamp_message))
//                    .orderDesc(MessageLocalObjectDao.Properties.Time)
//                    .list();
//
//            /////////
//            // Option 2
//            ///////
//
//
//            if (messageLocalObjectList.size() != 0){
//                Toast.makeText(
//                        mContext,
//                        mContext.getString(R.string.communication_showing_more_messages, messageLocalObjectList.size()),
//                        Toast.LENGTH_SHORT).show();
//
////                Collections.reverse(messageLocalObjectList);
//
//                for (MessageLocalObject messageLocalObject : messageLocalObjectList) {
//                    messages.add(0, messageLocalObject);
//                }
//                chatAdapter.notifyDataSetChanged();
//            }
//        }
    }
}



