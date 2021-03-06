/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wespot.pim.compat.controller;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import daoBase.DaoConfiguration;
import net.wespot.pim.R;
import net.wespot.pim.controller.Adapters.InquiryPagerAdapter;
import net.wespot.pim.controller.Adapters.NewInquiryPagerAdapter;
import net.wespot.pim.utils.layout.ActionBarCompat;
import net.wespot.pim.view.InqCreateInquiryFragment;
import org.celstec.arlearn.delegators.INQ;

public class InquiryActivityBack extends ActionBarCompat {

    private static final String TAG = "InquiryActivity";
    private static final String PHASE = "num_phase";
    private int mStackLevel = 0;

    private NewInquiryPagerAdapter mNewInquiryPagerAdapter;

    private long num_phase;

    public InquiryActivityBack() {
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (INQ.inquiry.getCurrentInquiry() == null){
            Log.e(TAG, "Back pressed - New inquiry");
        }else{
            Log.e(TAG, "Back pressed - Show inquiry");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("currentInquiry", INQ.inquiry.getCurrentInquiry().getId());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Avoiding NULL exceptions when resuming the PIM
//        if (INQ.inquiry == null ){
////            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
////            startActivity(intent);
//            INQ.init(this);
//            INQ.accounts.syncMyAccountDetails();
//            INQ.inquiry.syncInquiries();
//            Log.e(TAG, "recover INQ.inquiry is needed in InquiryActivity.");
//        }

        if (savedInstanceState != null) {
            INQ.init(this);
            INQ.accounts.syncMyAccountDetails();
//            INQ.inquiry.syncInquiries();
            INQ.inquiry.setCurrentInquiry(DaoConfiguration.getInstance().getInquiryLocalObjectDao().load(savedInstanceState.getLong("currentInquiry")));
            Log.e(TAG, "recover INQ.inquiry is needed in InquiryActivity.");
        }

        if (INQ.inquiry.getCurrentInquiry() == null){
            Log.e(TAG, "New inquiry");

            setContentView(R.layout.wrapper);
            FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction;

            fragmentTransaction = fragmentManager.beginTransaction();
            InqCreateInquiryFragment fragment = new InqCreateInquiryFragment();
            fragmentTransaction.add(R.id.content, fragment);
            fragmentTransaction.commit();

            getSupportActionBar().setTitle(R.string.actionbar_inquiry_list);

        }else{

            setContentView(R.layout.activity_inquiry);

            Log.e(TAG, "Show inquiry");

            // Create an adapter that when requested, will return a fragment representing an object in
            // the collection.
            // ViewPager and its adapters use support library fragments, so we must use
            // getSupportFragmentManager.
            /*
              The {@link android.support.v4.view.PagerAdapter} that will provide fragments representing
              each object in a collection. We use a {@link android.support.v4.app.FragmentStatePagerAdapter}
              derivative, which will destroy and re-create fragments as needed, saving and restoring their
              state in the process. This is important to conserve memory and is a best practice when
              allowing navigation between objects in a potentially large collection.
             */


            // Set up the ViewPager, attaching the adapter.
            /*
              The {@link android.support.v4.view.ViewPager} that will display the object collection.
             */
            ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);

            InquiryPagerAdapter mInquiryPagerAdapter = new InquiryPagerAdapter(getSupportFragmentManager(), mViewPager);
            getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

            mViewPager.setAdapter(mInquiryPagerAdapter);
            mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    // When swiping between different app sections, select the corresponding tab.
                    // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                    // Tab.
//                    getmActionBarHelper().setSelectedNavigationItem(position);
                }
            });

            getSupportActionBar().setTitle(getResources().getString(R.string.actionbar_inquiry)+" - "+INQ.inquiry.getCurrentInquiry().getTitle());

            Bundle extras = getIntent().getExtras();
            if (extras != null){
                mViewPager.setCurrentItem(extras.getInt(PHASE));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
