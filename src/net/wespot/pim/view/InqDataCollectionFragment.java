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
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.*;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import net.wespot.pim.R;
import net.wespot.pim.compat.view.InqDataCollectionTaskCompatFragment;
import net.wespot.pim.controller.Adapters.DataCollectionLazyListAdapter;
import net.wespot.pim.utils.layout.NoticeDialogFragment;
import org.celstec.arlearn.delegators.INQ;
import org.celstec.arlearn2.android.delegators.ARL;
import org.celstec.arlearn2.android.events.GeneralItemEvent;
import org.celstec.arlearn2.android.events.ResponseEvent;
import org.celstec.arlearn2.android.listadapter.ListItemClickInterface;
import org.celstec.dao.gen.GameLocalObject;
import org.celstec.dao.gen.GeneralItemLocalObject;

/**
 * Fragment to display Data Collection Task (General Item) from an Inquiry (Game)
 */
public class InqDataCollectionFragment extends Fragment implements ListItemClickInterface<GeneralItemLocalObject>{

    private static final String TAG = "InqDataCollectionFragment";
    private static final int DIALOG_FRAGMENT = 0;
    private ListView data_collection_tasks;
    private TextView text_default;
    private TextView data_collection_tasks_title_list;
    private NoticeDialogFragment dialog;

    private DataCollectionLazyListAdapter datAdapter;

    private static final String RUN_ID = "runId";
    private static final String GENERAL_ITEM = "generalItem";

    private GeneralItemLocalObject generalItemLocalObject;

    public InqDataCollectionFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("currentInquiry", INQ.inquiry.getCurrentInquiry().getId());
        if(INQ.inquiry.getCurrentInquiry().getRunLocalObject()!=null){
            outState.putLong("currentInquiryRunLocalObject", INQ.inquiry.getCurrentInquiry().getRunLocalObject().getId());
            Log.i(TAG, "Recover in InqDataCollectionFragment > onSaveInstanceState & current inq = null");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ARL.eventBus.register(this);
    }


    @Override
    public void onResume() {
        super.onResume();
//        addContentValidation();
        datAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.fragment_data_collection, container, false);
        data_collection_tasks = (ListView) rootView.findViewById(R.id.data_collection_tasks);
        text_default = (TextView) rootView.findViewById(R.id.text_default);
        data_collection_tasks_title_list = (TextView) rootView.findViewById(R.id.data_collection_tasks_title_list);
        data_collection_tasks = (ListView) rootView.findViewById(R.id.data_collection_tasks);

        addContentValidation();

        datAdapter =  new DataCollectionLazyListAdapter(this.getActivity(),INQ.inquiry.getCurrentInquiry().getRunLocalObject().getGameId());
        datAdapter.setOnListItemClickCallback(this);
        data_collection_tasks.setAdapter(datAdapter);

        return rootView;
    }

    public void addContentValidation() {

        if(INQ.inquiry.getCurrentInquiry().getRunLocalObject()!=null){

            GameLocalObject gameLocalObject = INQ.inquiry.getCurrentInquiry().getRunLocalObject().getGameLocalObject();

            if (gameLocalObject != null){
                if (gameLocalObject.getGeneralItems().size() != 0){
                    // Synchronize data from server
                    INQ.responses.syncResponses(INQ.inquiry.getCurrentInquiry().getRunLocalObject().getId());

                    // Clear object to make link to the new ones
                    for (GeneralItemLocalObject generalItemLocalObject : gameLocalObject.getGeneralItems()){
                        generalItemLocalObject.resetResponses();
                    }

                }else{
                    Log.i(TAG, "There are no data collection tasks for this inquiry");
                }
            }else{
                Log.i(TAG, "There is no game for this run.");
            }
        }else{
            Log.i(TAG, "Data collection task are not enabled on this inquiry");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_inquiry, menu);

        menu.setGroupVisible(R.id.actions_general, false);
        menu.setGroupVisible(R.id.actions_wonder_moment, false);
        menu.setGroupVisible(R.id.actions_data_collection, true);
        menu.setGroupVisible(R.id.actions_friends, false);
        menu.setGroupVisible(R.id.actions_usersite, false);
        menu.setGroupVisible(R.id.actions_questions, false);



        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_data_collection:
                create_new_data_collection();
                break;
            case R.id.menu_refresh_data_collection:

                Toast.makeText(getActivity(), "Updating data collections...", Toast.LENGTH_SHORT).show();

                GameLocalObject gameLocalObject = INQ.inquiry.getCurrentInquiry().getRunLocalObject().getGameLocalObject();
                for (GeneralItemLocalObject generalItemLocalObject : gameLocalObject.getGeneralItems()){
                    generalItemLocalObject.resetResponses();
                }
//                addContentValidation();
                datAdapter.notifyDataSetChanged();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void create_new_data_collection() {

        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        dialog = new NoticeDialogFragment();

        dialog.setTargetFragment(this, DIALOG_FRAGMENT);

        dialog.show(getFragmentManager().beginTransaction(), "dialog");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case DIALOG_FRAGMENT:

                if (resultCode == Activity.RESULT_OK) {
                    // After Ok code.
                    Log.i(TAG, "ok code");

                    INQ.dataCollection.createDataCollectionTask(
                            INQ.inquiry.getCurrentInquiry().getRunLocalObject().getGameLocalObject().getId(),
                            dialog.getTitle(),
                            dialog.getDescription(),
                            dialog.isImage(),
                            dialog.isAudio(),
                            dialog.isText(),
                            dialog.isVideo(),
                            dialog.isNumber()
                    );
                    Toast.makeText(getActivity(), getResources().getString(R.string.data_collection_dialog_creating), Toast.LENGTH_SHORT).show();

                } else if (resultCode == Activity.RESULT_CANCELED){
                    // After Cancel code.
                    Log.i(TAG, "cancel code");
                }
                break;
        }
    }

    public void onEventMainThread(GeneralItemEvent generalItem){
        Log.i(TAG, "Adding data collection in background");
        addContentValidation();
    }


    public void onEventBackgroundThread(ResponseEvent responseEvent){
        Log.i(TAG, " response for "+responseEvent.getRunId());

    }

    @Override
    public void onListItemClick(View v, int position, GeneralItemLocalObject object) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            intent = new Intent(getActivity(), InqDataCollectionTaskFragment.class);
        } else {
            intent = new Intent(getActivity(), InqDataCollectionTaskCompatFragment.class);
        }
        intent.putExtra(InqDataCollectionTaskFragment.DATA_COLLECTION_TASK_ID, object.getId());

        startActivity(intent);
    }

    @Override
    public boolean setOnLongClickListener(View v, int position, GeneralItemLocalObject object) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ARL.eventBus.unregister(this);
    }
}
