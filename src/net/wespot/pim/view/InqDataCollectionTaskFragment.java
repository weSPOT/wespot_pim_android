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

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import daoBase.DaoConfiguration;
import net.wespot.pim.R;
import net.wespot.pim.controller.ImageDetailActivity;
import net.wespot.pim.controller.ImageGridFragment;
import net.wespot.pim.utils.layout.BaseFragmentActivity;
import net.wespot.pim.view.impl.AudioCollectionActivityImpl;
import net.wespot.pim.view.impl.TextInputCollectionActivityImpl;
import net.wespot.pim.view.impl.ValueInputCollectionActivityImpl;
import org.celstec.arlearn.delegators.INQ;
import org.celstec.arlearn2.android.dataCollection.*;
import org.celstec.arlearn2.android.listadapter.ListItemClickInterface;
import org.celstec.dao.gen.GeneralItemLocalObject;
import org.celstec.dao.gen.ResponseLocalObject;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.File;

/**
 * Fragment to display responses from a Data Collection Task (General Item)
 */
public class InqDataCollectionTaskFragment extends BaseFragmentActivity implements ListItemClickInterface<ResponseLocalObject> {

    private static final String TAG = "InqDataCollectionTaskFragment";
    public static final String GENERAL_ITEM = "generalItem";
    public static final String DATA_COLLECTION_TASK_ID = "dataCollectionTask";
    private long generalItemId;

    private GeneralItemLocalObject genObject;

    private boolean isDataCollectionAudio;
    private boolean isDataCollectionVideo;
    private boolean isDataCollectionPicture;
    private boolean isDataCollectionValue;
    private boolean isDataCollectionText;

    private PictureManager man_pic;
    private VideoManager man_vid;
    private AudioInputManager man_aud;
    private ValueInputManager man_val;
    private TextInputManager man_tex;

    public static final Object PICTURE_RESULT = 0;

    private File bitmapFile;

    private final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

    private ImageGridFragment frag;

    public InqDataCollectionTaskFragment() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        INQ.eventBus.unregister(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_data_collection_task);

        if (savedInstanceState != null) {
            INQ.init(this);
            INQ.accounts.syncMyAccountDetails();
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null){

            Log.i(TAG,"Entering in data collection number: "+extras.getLong("DataCollectionTask")+"");

            generalItemId = extras.getLong(DATA_COLLECTION_TASK_ID);
            genObject = DaoConfiguration.getInstance().getGeneralItemLocalObjectDao().load(generalItemId);

        }else{
            Log.i(TAG, "Failed because there is no extra parameter");
        }

        defineValueInputManager();
        definePictureInputManager();
        defineAudioInputManager();
        defineTextInputManager();
        defineVideoInputManager();

        JSONObject json = null;
        try {
            json = new JSONObject(genObject.getBean());
            JSONObject openQuestionJson = json.getJSONObject("openQuestion");

            isDataCollectionAudio = openQuestionJson.getBoolean("withAudio");
            isDataCollectionText = openQuestionJson.getBoolean("withText");
            isDataCollectionPicture = openQuestionJson.getBoolean("withPicture");
            isDataCollectionValue = openQuestionJson.getBoolean("withValue");
            isDataCollectionVideo = openQuestionJson.getBoolean("withVideo");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        genObject.getResponses();

        TextView data_collection_tasks_description = (TextView) findViewById(R.id.data_collection_tasks_description_list);
        TextView data_collection_tasks_title = (TextView) findViewById(R.id.data_collection_tasks_title_list);

        data_collection_tasks_title.setText(genObject.getTitle());
        data_collection_tasks_description.setText(genObject.getDescription());

        if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {

            Bundle data = new Bundle();
            data.putLong(GENERAL_ITEM, generalItemId);

            frag = new ImageGridFragment();
            frag.setArguments(data);

            ft.add(R.id.content_images, frag, TAG);
            ft.commit();
        }

        getActionBar().setTitle(getResources().getString(R.string.actionbar_list_data_collection_task));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_data_collection, menu);

        setEnabledDisabled(menu.getItem(0), isDataCollectionPicture);
        setEnabledDisabled(menu.getItem(1), isDataCollectionVideo);
        setEnabledDisabled(menu.getItem(2), isDataCollectionAudio);
        setEnabledDisabled(menu.getItem(3), isDataCollectionText);
        setEnabledDisabled(menu.getItem(4), isDataCollectionValue);

        return super.onCreateOptionsMenu(menu);
    }

    public void setEnabledDisabled(MenuItem item, boolean shouldBeEnabled){
        if (shouldBeEnabled) {
            item.setEnabled(true);
            item.getIcon().setAlpha(255);
        } else {
            item.setEnabled(false);
            item.getIcon().setAlpha(130);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_data_collection_image:
                definePictureInputManager();
                man_pic.takeDataSample(null);
                break;
            case R.id.menu_data_collection_video:
                defineVideoInputManager();
                man_vid.takeDataSample(null);
                break;
            case R.id.menu_data_collection_audio:
                defineAudioInputManager();
                man_aud.takeDataSample(AudioCollectionActivityImpl.class);
                break;
            case R.id.menu_data_collection_text:
                defineTextInputManager();
                man_tex.takeDataSample(TextInputCollectionActivityImpl.class);
                break;
            case R.id.menu_data_collection_numeric:
                defineValueInputManager();
                man_val.takeDataSample(ValueInputCollectionActivityImpl.class);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode){
            case DataCollectionManager.PICTURE_RESULT:
                man_pic.onActivityResult(requestCode, resultCode, data);
                Toast.makeText(this, "Uploading picture...", Toast.LENGTH_LONG).show();

                break;
            case DataCollectionManager.AUDIO_RESULT:
                man_aud.onActivityResult(requestCode, resultCode, data);
                Toast.makeText(this, "Uploading audio...", Toast.LENGTH_LONG).show();

                break;
            case DataCollectionManager.VIDEO_RESULT:
                man_vid.onActivityResult(requestCode, resultCode, data);
                Toast.makeText(this, "Uploading video...", Toast.LENGTH_LONG).show();

                break;
            case DataCollectionManager.TEXT_RESULT:
                man_tex.onActivityResult(requestCode, resultCode, data);
                Toast.makeText(this, "Uploading text...", Toast.LENGTH_LONG).show();

                break;
            case DataCollectionManager.VALUE_RESULT:
                man_val.onActivityResult(requestCode, resultCode, data);
                Toast.makeText(this, "Uploading value...", Toast.LENGTH_LONG).show();

                break;
        }

        INQ.responses.syncResponses(INQ.inquiry.getCurrentInquiry().getRunLocalObject().getId());
    }

    private void defineAudioInputManager() {
        man_aud = new AudioInputManager(this);
        man_aud.setRunId(INQ.inquiry.getCurrentInquiry().getRunId());
        man_aud.setGeneralItem(genObject);
    }

    private void defineTextInputManager() {
        man_tex = new TextInputManager(this);
        man_tex.setRunId(INQ.inquiry.getCurrentInquiry().getRunId());
        man_tex.setGeneralItem(genObject);
    }

    private void defineVideoInputManager() {
        man_vid = new VideoManager(this);
        man_vid.setRunId(INQ.inquiry.getCurrentInquiry().getRunId());
        man_vid.setGeneralItem(genObject);
    }

    private void defineValueInputManager() {
        man_val = new ValueInputManager(this);
        man_val.setRunId(INQ.inquiry.getCurrentInquiry().getRunId());
        man_val.setGeneralItem(genObject);
    }

    private void definePictureInputManager() {
        man_pic = new PictureManager(this);
        man_pic.setRunId(INQ.inquiry.getCurrentInquiry().getRunId());
        man_pic.setGeneralItem(genObject);
    }

    // Not used
    @Override
    public void onListItemClick(View v, int position, ResponseLocalObject object) {
        // Not used
        Intent intent = new Intent(getApplicationContext(), ImageDetailActivity.class);
        intent.putExtra("DataCollectionTask", object.getId());
        intent.putExtra("DataCollectionTaskGeneralItemId", generalItemId);
        intent.putExtra(ImageDetailActivity.RESPONSE_POSITION, position);
        startActivity(intent);
    }

    @Override
    public boolean setOnLongClickListener(View v, int position, final ResponseLocalObject object) {
        // Not used
        return false;
    }

}
