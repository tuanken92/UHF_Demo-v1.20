package com.nlscan.uhf.demo;

import android.support.multidex.MultiDexApplication;

import com.nlscan.uhf.demo.util.ScreenUtil;
import com.nlscan.uhf.demo.util.constant.SharePreferenceConfig;
import com.nlscan.uhf.demo.util.storage.LocalStorageManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppApplication extends MultiDexApplication {
    public static AppApplication instance;

    private List<String> mTagDatas;
    public static AppApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mTagDatas = new ArrayList<>();
        ScreenUtil.resetDensity(getApplicationContext());
        LocalStorageManager.setBoolean(SharePreferenceConfig.Key.SHOULD_REFRESH_LIST, false);
    }

    public List<String> getTagDatas()
    {
        if(mTagDatas == null)
            mTagDatas = new ArrayList<>();
        return mTagDatas;
    }

    public void addTagData(String data)
    {
        if(!mTagDatas.contains(data))
            mTagDatas.add(data);
    }

    public void clearTagData()
    {
        mTagDatas.clear();
    }
}
