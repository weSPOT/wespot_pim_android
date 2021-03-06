package net.wespot.pim.view;

/**
 * ****************************************************************************
 * Copyright (C) 2013 Open Universiteit Nederland
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import net.wespot.pim.R;
import net.wespot.pim.compat.controller.InquiryActivityBack;
import net.wespot.pim.controller.Adapters.Answers.AnswerDelegator;
import net.wespot.pim.controller.Adapters.InquiryLazyListAdapter;
import net.wespot.pim.controller.InquiryActivity;
import net.wespot.pim.controller.InquiryPhasesActivity;
import net.wespot.pim.utils.Constants;
import net.wespot.pim.utils.layout.BaseFragmentActivity;
import net.wespot.pim.utils.layout.ButtonManager;
import net.wespot.pim.utils.layout.InquiryDialogFragment;
import org.celstec.arlearn.delegators.INQ;
import org.celstec.arlearn.delegators.QuestionDelegator;
import org.celstec.arlearn2.android.delegators.ARL;
import org.celstec.arlearn2.android.listadapter.ListItemClickInterface;
import org.celstec.dao.gen.InquiryLocalObject;
import org.celstec.events.InquiryEvent;

import java.util.ArrayList;

/**
 * A fragment that launches other parts of the demo application.
 */
public class PimInquiriesFragment extends BaseFragmentActivity implements ListItemClickInterface<InquiryLocalObject> {

    private static final String TAG = "PimInquiriesFragment";
    private InquiryLazyListAdapter adapterInq;
    private InquiryDialogFragment dialog;

    private static final int DIALOG_FRAGMENT = 0;
    private static final int NEW_INQUIRY = 12350;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        INQ.init(this);
        INQ.accounts.syncMyAccountDetails();
        ARL.eventBus.register(this);
        if (savedInstanceState != null) {


        }

        INQ.inquiry.syncInquiries();

        setContentView(R.layout.fragment_inquiries);

        ListView inquiries = (ListView) findViewById(R.id.list_inquiries);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.inquiries_new_inquiry);

        ////////////////////////////////////
        // When coming from notification bar
        ////////////////////////////////////
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            ArrayList<String> list_id =  extras.getStringArrayList(InqCommunicateFragment.INQUIRIES_ID);


            adapterInq =  new InquiryLazyListAdapter(this, list_id);
        }else{

            adapterInq =  new InquiryLazyListAdapter(this);
        }


        inquiries.setAdapter(adapterInq);
        adapterInq.setOnListItemClickCallback(this);

        // Instantiation of the buttonManager
        ButtonManager buttonManager = new ButtonManager(this);

        // Creation of layout params
        LinearLayout.LayoutParams secondLayoutParams = buttonManager.generateLayoutParams(
                R.dimen.mainscreen_margintop_zero,
                (int)getResources().getDimension(R.dimen.mainscreen_margintop_zero));

        // New inquiry button
        buttonManager.generateButton(linearLayout, secondLayoutParams, Constants.ID_NEW_INQUIRY,
                Constants.INQUIRY_MAIN_LIST.get(Constants.ID_NEW_INQUIRY),
                Constants.INQUIRY_ICON_MAIN_LIST.get(Constants.ID_NEW_INQUIRY), "", true)
                .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                INQ.inquiry.setCurrentInquiry(null);
                Intent intent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    intent = new Intent(getApplicationContext(), InquiryActivity.class);
                } else {
                    intent = new Intent(getApplicationContext(), InquiryActivityBack.class);
                }
                startActivity(intent);
            }
        });

        setTitle(R.string.common_title);
    }

    public void onEventAsync(InquiryEvent inquiryObject){
//        Toast.makeText(getApplicationContext(), "Loaded: "+DaoConfiguration.getInstance().getInquiryLocalObjectDao().loadAll().size()+" inquiries",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        adapterInq.close();
        ARL.eventBus.unregister(this);
        super.onDestroy();
    }

    @Override
    public void onListItemClick(View v, int position, InquiryLocalObject object) {
        Intent intent = new Intent(getApplicationContext(), InquiryPhasesActivity.class);
        INQ.inquiry.setCurrentInquiry(object);
        QuestionDelegator.getInstance().syncQuestions(object);
        AnswerDelegator.getInstance().syncAnswers(object);
        startActivity(intent);
    }

    @Override
    public boolean setOnLongClickListener(View v, int position, final InquiryLocalObject object) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Do your Yes progress
                        //TODO add delete functionality
                        object.delete();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //Do your No progress
                        break;
                }
            }
        };
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setMessage("Are you sure to delete?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_inquiry, menu);

        menu.setGroupVisible(R.id.actions_general, true);
        menu.setGroupVisible(R.id.actions_wonder_moment, false);
        menu.setGroupVisible(R.id.actions_data_collection, false);
        menu.setGroupVisible(R.id.actions_friends, false);
        menu.setGroupVisible(R.id.actions_questions, false);
        menu.setGroupVisible(R.id.actions_usersite, false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                Toast.makeText(this, getResources().getString(R.string.menu_refreshing), Toast.LENGTH_SHORT).show();
                INQ.inquiry.syncInquiries();
                break;
//            case R.id.menu_new_inquiry:
//                createNewInquiry();
//                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createNewInquiry() {
//        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
//        if (prev != null) {
//            ft.remove(prev);
//        }
//        ft.addToBackStack(null);
//
//        dialog = new InquiryDialogFragment();
//        dialog.show(getSupportFragmentManager(), "dialog");


        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = InquiryDialogFragment.newInstance();
        newFragment.show(ft, "dialog");


// /        FragmentTransaction ft = getFragmentManager().beginTransaction();
//        DialogFragment newFragment = new InquiryDialogFragment();
//        ft.add(R.id.content, newFragment);
//        ft.commit();


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case DIALOG_FRAGMENT:

                if (resultCode == Activity.RESULT_OK) {
                    // After Ok code.
                    Log.e(TAG, "OK code");
                } else if (resultCode == Activity.RESULT_CANCELED){
                    // After Cancel code.
                    Log.e(TAG, "cancel code");
                }
                break;
        }
    }

    public void doPositiveClick() {

    }

    public void doNegativeClick() {

    }

    private class CreateInquiryObject {
        public InquiryLocalObject inquiry;
    }
}