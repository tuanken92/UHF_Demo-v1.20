package com.nlscan.uhf.demo.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFModuleInfo;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.uhf.demo.AppApplication;
import com.nlscan.uhf.demo.R;
import com.nlscan.uhf.demo.fragment.BaseFragment;
import com.nlscan.uhf.demo.fragment.InventoryFragment;
import com.nlscan.uhf.demo.fragment.TagWriteFragment;
import com.nlscan.uhf.demo.fragment.UHFSettingsFragment;
import com.nlscan.uhf.demo.util.Constant;
import com.nlscan.uhf.demo.util.constant.UHFSilionParams;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private Context mContext;
    private UHFManager mUHFMgr = UHFManager.getInstance();
    private BaseFragment mInventoryFragment;
    private BaseFragment mSettingsFragment;
    private BaseFragment mTagFragment;
    private BaseFragment mCurFragment;

    private TextView tv_inv_mode_label;
    private TextView tv_inventory;
    private TextView tv_settings;
    private TextView tv_tags;
    private ImageView im_actionbar_settings;

    private PopupWindow mInvPolicyPopupWindow;
    private View mInvPolicyContentview;

    private boolean mPaused = false;
    private ProgressDialog mLoadingPD = null;
    private Dialog mReLoadDialog = null;

    private final int MSG_LOAD_MODULE_COMPLETED = 0x01;
    private final int MSG_LOAD_MODULE = 0x02;
    private final int MSG_SHOW_RELOAD_WINDOW = 0x03;
    private final int MSG_UPDATE_ACTION_BAR = 0x04;

    private Handler mUIHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {

                case MSG_LOAD_MODULE_COMPLETED:
                    if(mUHFMgr.getUHFModuleInfo() != null)
                        doPowerOn();
                    else
                        showReloadModuleWindow();
                    break;
                case MSG_LOAD_MODULE:
                    loadModule();
                    break;
                case MSG_SHOW_RELOAD_WINDOW:
                    showReloadModuleWindow();
                    break;
                case MSG_UPDATE_ACTION_BAR:
                    updateActionBarInfo();
                    break;

            }//end switch
        }

    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        initView();
        registerUHFStateReceiver();

        if(mUHFMgr.isPowerOn())
            showInventoryFragment();
    }

    private long mLastKeyBackTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            if(mLastKeyBackTime == 0 || (System.currentTimeMillis() - mLastKeyBackTime) > 1000)
            {
                Toast.makeText(mContext,getString(R.string.exit_confirm_tip),Toast.LENGTH_SHORT).show();
                mLastKeyBackTime = System.currentTimeMillis();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPaused = false;
        UHFModuleInfo module = mUHFMgr.getUHFModuleInfo();
        if(module == null) //Load rfid mode
            loadModule();
        else if(!mUHFMgr.isPowerOn()) //Do power on
            doPowerOn();
        else
            updateActionBarInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPaused = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterUHFStateReceiver();
        //Clear datas of this inventory period
        AppApplication.getInstance().clearTagData();
        mUHFMgr.powerOff();

    }

    private void initView()
    {
        tv_inventory = (TextView) findViewById(R.id.tv_inventory);
        tv_settings = (TextView) findViewById(R.id.tv_settings);
        tv_tags = (TextView) findViewById(R.id.tv_tags);
        im_actionbar_settings = (ImageView) findViewById(R.id.im_actionbar_settings);
        tv_inv_mode_label = (TextView) findViewById(R.id.tv_inv_mode_label);

        View.OnClickListener mClick = new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {
                switch (v.getId())
                {
                    case R.id.tv_inventory:
                        showInventoryFragment();
                        break;
                    case R.id.tv_settings:
                        showSettingsFragment();
                        break;
                    case R.id.tv_tags:
                        showTagsFragment();
                        break;
                    case R.id.im_actionbar_settings:
                        showInvPolicyPopWindow();
                        break;
                }
            }
        };

        tv_inventory.setOnClickListener(mClick);
        tv_settings.setOnClickListener(mClick);
        tv_tags.setOnClickListener(mClick);
        im_actionbar_settings.setOnClickListener(mClick);
    }

    private void updateActionBarInfo()
    {
        String invModeName = null;
        //--SLR1200[UR90] part
        if(Constant.isSLR1200(mUHFMgr.getUHFDeviceModel()))
        {
            int iQuickMode= getUHFIntSetting(UHFSilionParams.INV_QUICK_MODE.KEY,0);
            int[] iGenSessions = getUHFIntArraySetting(UHFSilionParams.POTL_GEN2_SESSION.KEY);
            iGenSessions = iGenSessions == null ? new int[]{-1}:iGenSessions;
            boolean q1enable1200 =  ( iQuickMode == 1 && iGenSessions[0] > 0 );
            boolean q0enable1200 =  ( iQuickMode == 1 && iGenSessions[0] ==  0 );
            if(q1enable1200)
                invModeName = getString(R.string.start_quick_mode_s1);
            else if(q0enable1200)
                invModeName = getString(R.string.start_quick_mode_s0);
            else
                invModeName = getString(R.string.uhf_inv_mode_normal);
        }

        //--SIM7100[UR90_V2.0] part
        if(Constant.isSIM7100(mUHFMgr.getUHFDeviceModel()))
        {
            int curInvPolicy= getUHFIntSetting(UHFSilionParams.INV_POLICY.KEY,mContext.getResources().getInteger(R.integer.inv_policy_balance_value));
            int normalPolicy = mContext.getResources().getInteger(R.integer.inv_policy_normal_value);
            int balancePolicy = mContext.getResources().getInteger(R.integer.inv_policy_balance_value);
            int quickPolicy = mContext.getResources().getInteger(R.integer.inv_policy_quickly_value);

            if(curInvPolicy == normalPolicy)
                invModeName = getString(R.string.inv_policy_normal_label);
            else if(curInvPolicy == balancePolicy)
                invModeName = getString(R.string.inv_policy_balance_label);
            else if(curInvPolicy == quickPolicy)
                invModeName = getString(R.string.inv_policy_quickly_label);
            else
                invModeName = getString(R.string.inv_policy_normal_label);
        }

        invModeName = invModeName == null ? "" : invModeName;
        tv_inv_mode_label.setText(invModeName);

    }

    private void focusTab(int id)
    {
        tv_inventory.setTextColor(Color.WHITE);
        tv_settings.setTextColor(Color.WHITE);
        tv_tags.setTextColor(Color.WHITE);
        switch (id)
        {
            case R.id.tv_inventory:
                tv_inventory.setTextColor(getResources().getColor(R.color.dark_blue));
                break;
            case R.id.tv_settings:
                tv_settings.setTextColor(getResources().getColor(R.color.dark_blue));
                break;
            case R.id.tv_tags:
                tv_tags.setTextColor(getResources().getColor(R.color.dark_blue));
                break;
        }
    }

    private void showInventoryFragment()
    {
        if(mInventoryFragment == null)
            mInventoryFragment = new InventoryFragment();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.body,mInventoryFragment);
        transaction.commit();
        im_actionbar_settings.setVisibility(View.VISIBLE);
        tv_inv_mode_label.setVisibility(View.VISIBLE);
        focusTab(R.id.tv_inventory);
        mCurFragment = mInventoryFragment;

        updateActionBarInfo();
    }

    private void showSettingsFragment()
    {
        if(mSettingsFragment == null)
            mSettingsFragment = new UHFSettingsFragment();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.body,mSettingsFragment);
        transaction.commit();
        im_actionbar_settings.setVisibility(View.INVISIBLE);
        tv_inv_mode_label.setVisibility(View.INVISIBLE);
        focusTab(R.id.tv_settings);
        mCurFragment = mSettingsFragment;
    }

    private void showTagsFragment()
    {
        if(mTagFragment == null)
            mTagFragment = new TagWriteFragment();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.body,mTagFragment);
        transaction.commit();
        im_actionbar_settings.setVisibility(View.INVISIBLE);
        tv_inv_mode_label.setVisibility(View.INVISIBLE);
        focusTab(R.id.tv_tags);
        mCurFragment = mTagFragment;
    }

    private void showInvPolicyPopWindow()
    {
        if(mUHFMgr.isInInventory()) {
            Toast.makeText(mContext,getString(R.string.stop_inventory_first),Toast.LENGTH_SHORT).show();
            return;
        }

        initInvModePopupView();
        if (mInvPolicyPopupWindow == null)
        {
            mInvPolicyPopupWindow = new PopupWindow(mInvPolicyContentview, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mInvPolicyPopupWindow.setBackgroundDrawable(mContext.getDrawable(android.R.drawable.spinner_dropdown_background));
            mInvPolicyPopupWindow.setOutsideTouchable(true);
            mInvPolicyPopupWindow.setFocusable(true);
        }

        mInvPolicyPopupWindow.showAsDropDown(im_actionbar_settings, -80, 0);
    }//

    private void initInvModePopupView()
    {
        if(mInvPolicyContentview == null)
            mInvPolicyContentview = getLayoutInflater().inflate(R.layout.layout_inv_policy, null);
        RadioGroup rg_inv_policy_slr1200 = (RadioGroup) mInvPolicyContentview.findViewById(R.id.rg_inv_policy_slr1200);
        RadioGroup rg_inv_policy_sim7100 = (RadioGroup) mInvPolicyContentview.findViewById(R.id.rg_inv_policy_sim7100);

        boolean isSLR1200 = Constant.isSLR1200(mUHFMgr.getUHFDeviceModel());
        boolean isSIM7100 = Constant.isSIM7100(mUHFMgr.getUHFDeviceModel());

        rg_inv_policy_slr1200.setVisibility(isSLR1200 ? View.VISIBLE : View.GONE);
        rg_inv_policy_sim7100.setVisibility(isSIM7100 ? View.VISIBLE : View.GONE);

        if(isSLR1200)
            initPopuViewSLR1200();//--SLR1200[UR90] part
        else if(isSIM7100)
            initPopuViewSIM7100();//--SIM7100[UR90_V2.0] part
    }

    private void initPopuViewSLR1200()
    {
        //--SLR1200[UR90] part
        RadioGroup rg_inv_policy_slr1200 = (RadioGroup) mInvPolicyContentview.findViewById(R.id.rg_inv_policy_slr1200);
        int iQuickMode= getUHFIntSetting(UHFSilionParams.INV_QUICK_MODE.KEY,0);
        int[] iGenSessions = getUHFIntArraySetting(UHFSilionParams.POTL_GEN2_SESSION.KEY);
        iGenSessions = iGenSessions == null ? new int[]{-1}:iGenSessions;
        boolean q1enable1200 =  ( iQuickMode == 1 && iGenSessions[0] > 0 );
        boolean q0enable1200 =  ( iQuickMode == 1 && iGenSessions[0] ==  0 );
        if(q1enable1200)
            rg_inv_policy_slr1200.check(R.id.rb_item_inv_quickly_s1);
        else if(q0enable1200)
            rg_inv_policy_slr1200.check(R.id.rb_item_inv_quickly_s0);
        else
            rg_inv_policy_slr1200.check(R.id.rb_item_inv_quickly_normal);

        rg_inv_policy_slr1200.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                selectInvPolicy(checkedId);
                mInvPolicyPopupWindow.dismiss();
            }
        });
    }

    private void initPopuViewSIM7100()
    {
        //--SIM7100[UR90_V2.0] part
        RadioGroup rg_inv_policy_sim7100 = (RadioGroup) mInvPolicyContentview.findViewById(R.id.rg_inv_policy_sim7100);
        int curInvPolicy= getUHFIntSetting(UHFSilionParams.INV_POLICY.KEY,mContext.getResources().getInteger(R.integer.inv_policy_balance_value));
        int normalPolicy = mContext.getResources().getInteger(R.integer.inv_policy_normal_value);
        int balancePolicy = mContext.getResources().getInteger(R.integer.inv_policy_balance_value);
        int quickPolicy = mContext.getResources().getInteger(R.integer.inv_policy_quickly_value);

        if(curInvPolicy == normalPolicy)
            rg_inv_policy_sim7100.check(R.id.rb_item_inv_normal);
        else if(curInvPolicy == balancePolicy)
            rg_inv_policy_sim7100.check(R.id.rb_item_inv_balance);
        else if(curInvPolicy == quickPolicy)
            rg_inv_policy_sim7100.check(R.id.rb_item_inv_quickly);
        else
            rg_inv_policy_sim7100.check(R.id.rb_item_inv_normal);

        rg_inv_policy_sim7100.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                selectInvPolicy(checkedId);
                mInvPolicyPopupWindow.dismiss();
            }
        });

    }//End initPopuViewSIM7100

    //Select inventory strategy
    private void selectInvPolicy(int vId)
    {
        UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR;
        //--SLR1200[UR90] part
        if(vId == R.id.rb_item_inv_quickly_normal ||
                vId == R.id.rb_item_inv_quickly_s1 ||
                vId == R.id.rb_item_inv_quickly_s0)
        {
            boolean enableQuickMode = (vId == R.id.rb_item_inv_quickly_s1 || vId == R.id.rb_item_inv_quickly_s0);
            er = mUHFMgr.setParam(UHFSilionParams.INV_QUICK_MODE.KEY, UHFSilionParams.INV_QUICK_MODE.PARAM_INV_QUICK_MODE, enableQuickMode?"1":"0");
            if(enableQuickMode && er == UHFReader.READER_STATE.OK_ERR){
                String session = vId == R.id.rb_item_inv_quickly_s1 ?  "1" : "0";
                er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_SESSION.KEY, UHFSilionParams.POTL_GEN2_SESSION.PARAM_POTL_GEN2_SESSION, session);
            }
            if(er != UHFReader.READER_STATE.OK_ERR)
                Toast.makeText(mContext,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
        }

        //--SIM7100[UR90_V2.0] part
        if(vId == R.id.rb_item_inv_normal ||
                vId == R.id.rb_item_inv_balance ||
                vId == R.id.rb_item_inv_quickly)
        {
            int iValue = mContext.getResources().getInteger(R.integer.inv_policy_balance_value);
            if(vId == R.id.rb_item_inv_normal)
                iValue = mContext.getResources().getInteger(R.integer.inv_policy_normal_value);
            if(vId == R.id.rb_item_inv_quickly)
                iValue = mContext.getResources().getInteger(R.integer.inv_policy_quickly_value);
            er = mUHFMgr.setParam(UHFSilionParams.INV_POLICY.KEY,UHFSilionParams.INV_POLICY.PARAM_INV_POLICY,String.valueOf(iValue));
            if(er != UHFReader.READER_STATE.OK_ERR) {
                Toast.makeText(mContext, getString(R.string.setting_fail) + ",err: " + er.name(), Toast.LENGTH_SHORT).show();
            }
        }

        //Update action bar's information
        if(er == UHFReader.READER_STATE.OK_ERR)
            updateActionBarInfo();

        //Notify inventory to update view state
        if(mCurFragment instanceof InventoryFragment)
            ((InventoryFragment) mCurFragment).updateViewData();
    }

    protected void registerUHFStateReceiver()
    {
        IntentFilter iFilter = new IntentFilter(UHFManager.ACTOIN_UHF_STATE_CHANGE);
        mContext.registerReceiver(mUHFStateReceiver, iFilter);

        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mBatteryStateReceiver, batteryFilter);

    }

    protected void unRegisterUHFStateReceiver()
    {
        try {
            mContext.unregisterReceiver(mUHFStateReceiver);
            mContext.unregisterReceiver(mBatteryStateReceiver);
        } catch (Exception e) {
        }
    }

    private void doPowerOn()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                UHFReader.READER_STATE er = mUHFMgr.powerOn();
                if(er != UHFReader.READER_STATE.OK_ERR)
                    mUIHandler.sendEmptyMessage(MSG_SHOW_RELOAD_WINDOW);
            }
        }).start();
    }

    protected void showLoadingWindow()
    {
        if(mPaused)
            return ;

        if(mLoadingPD != null && mLoadingPD.isShowing())
            return ;

        mLoadingPD = new ProgressDialog(MainActivity.this);
        mLoadingPD.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mLoadingPD.setCancelable(true);
        mLoadingPD.setCanceledOnTouchOutside(false);
        mLoadingPD.setMessage(getString(R.string.power_oning));
        mLoadingPD.show();
    }

    //Reload module information
    private void loadModule()
    {
        synchronized (this) {

            if(mPaused)
                return ;

            showLoadingWindow();
            new Thread(new Runnable() {

                @Override
                public void run() {
                    mUHFMgr.loadUHFModule();
                    mUIHandler.sendEmptyMessage(MSG_LOAD_MODULE_COMPLETED);
                }

            }).start();
        }
    }

    //Module not exists , show window
    private void showReloadModuleWindow()
    {
        synchronized (this) {

            if(mReLoadDialog != null)
            {
                if(!mReLoadDialog.isShowing() && !mPaused) {
                    mReLoadDialog.show();
                }else
                    return ;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.common_tip);
            builder.setMessage(getString(R.string.uhf_module_unavailable));
            builder.setPositiveButton(R.string.search_again, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mUIHandler.sendEmptyMessageDelayed(MSG_LOAD_MODULE, 50);
                }
            }).setNegativeButton(R.string.common_exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });

            mReLoadDialog = builder.create();
            mReLoadDialog.setCanceledOnTouchOutside(false);
            if(!mPaused)
                mReLoadDialog.show();
            else
                mReLoadDialog = null;
        }
    }


    //Power oning
    protected void onUhfPowerOning()
    {
        Log.d("TAG","---power oning---");
        showLoadingWindow();
        if(mCurFragment != null)
            mCurFragment.onUhfPowerOning();
    }

    /**
     * Power on complete
     */
    protected void onUhfPowerOn()
    {
        if(mLoadingPD != null )
            mLoadingPD.dismiss();
        if(mCurFragment != null)
            mCurFragment.onUhfPowerOn();
        else
            showInventoryFragment();
        updateActionBarInfo();
    }

    /**
     * Power off complete
     */
    protected void onUhfPowerOff()
    {
        if(mLoadingPD != null )
            mLoadingPD.dismiss();
        if(mCurFragment != null)
            mCurFragment.onUhfPowerOff();
    }

    /**
     * It's starting inventory
     */
    public void onUhfStartInventory()
    {
        if(mCurFragment != null)
            mCurFragment.onUhfStartInventory();
    }

    /**
     * It's stoping inventory
     */
    public void onUhfStopInventory()
    {
        if(mCurFragment != null)
            mCurFragment.onUhfStopInventory();
    }

    private int getUHFIntSetting(String key,int defaultValue)
    {
        Map<String,Object> settingsMap = mUHFMgr.getAllParams();
        int result = defaultValue;
        if(settingsMap != null && settingsMap.get(key) != null) {
            result = (Integer) settingsMap.get(key);
        }
        return result;
    }

    private int[] getUHFIntArraySetting(String key)
    {
        Map<String,Object> settingsMap = mUHFMgr.getAllParams();
        int[] result = null;
        if(settingsMap != null && settingsMap.get(key) != null)
            result = (int[]) settingsMap.get(key);
        return result;
    }

    //---------------------------------------------------------
    // Inner Class
    //---------------------------------------------------------
    private BroadcastReceiver mUHFStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent == null)
                return ;
            if(UHFManager.ACTOIN_UHF_STATE_CHANGE.equals(intent.getAction()))
            {
                int uhf_state = intent.getIntExtra(UHFManager.EXTRA_UHF_STATE, -1);
                switch (uhf_state) {
                    case UHFSilionParams.UHF_STATE_POWER_ONING:
                        onUhfPowerOning();
                        break;
                    case UHFSilionParams.UHF_STATE_POWER_ON:
                        onUhfPowerOn();
                        break;
                    case UHFSilionParams.UHF_STATE_POWER_OFF:
                        onUhfPowerOff();
                        break;
                    case UHFSilionParams.UHF_STATE_START_INVENTORY:
                        onUhfStartInventory();
                        break;
                    case UHFSilionParams.UHF_STATE_STOP_INVENTORY:
                        onUhfStopInventory();
                        break;
                }
            }
        }
    };


    private BroadcastReceiver mBatteryStateReceiver= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action=intent.getAction();
            if(Intent.ACTION_BATTERY_CHANGED.equals(action))
            {
                int btemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
                float fbtemperature = (float)btemperature/10;

                String sHighestTemper = mUHFMgr.getParam(UHFSilionParams.HIGH_TEMPERATURE.KEY,
                        UHFSilionParams.HIGH_TEMPERATURE.PARAM_HIGH_TEMPERATURE_VALUE,
                        String.valueOf(UHFSilionParams.HIGH_TEMPERATURE.DEFAULT_TEMPERATURE_VALUE));

                int highestTemper = 100;
                if(sHighestTemper != null && TextUtils.isDigitsOnly(sHighestTemper))
                    highestTemper = Integer.parseInt(sHighestTemper);

                if(fbtemperature > highestTemper) //Higher than settings most temperature
                {
                    mUIHandler.sendEmptyMessageDelayed(MSG_UPDATE_ACTION_BAR,500);
                }
            }
        }
    };//End BatteryStateReceiver
}
