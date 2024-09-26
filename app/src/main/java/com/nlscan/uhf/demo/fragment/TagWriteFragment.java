package com.nlscan.uhf.demo.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.uhf.demo.AppApplication;
import com.nlscan.uhf.demo.R;
import com.nlscan.uhf.demo.util.constant.UHFSilionParams;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TagWriteFragment extends BaseFragment {

    private final String TAG = "TagWriteFragment";
    private UHFManager mUHFMgr = UHFManager.getInstance();

    private Context mContext;
    private View mLayoutView;

    //--filter tags part
    private Spinner spinner_filter_tag;
    private Spinner spinner_filter_bank;
    private RadioGroup rg_match_group;
    private RadioGroup rg_enable_group;
    private List<String> mHistoryTagsList;
    private ArrayAdapter mFilterTagAdapter;
    private ArrayAdapter mFilterBankAdapter;
    private Button btn_set_filter;
    private String[] bank_filter_labels;
    private int[] bank_filter_values;

    //--Read/Write tag
    private Spinner spinner_read_bank;
    private EditText et_start_addr;
    private EditText et_byte_count;
    private EditText et_access_pwd;
    private EditText et_read_tag_data;
    private EditText et_write_tag_data;
    private Button btn_read_tag;
    private Button btn_write_tag;
    private String[] bank_labels;
    private int[] bank_values;

    //--Lock tag part
    private Spinner spinner_lock_bank;
    private Spinner spinner_lock_type;
    private String[] mLockBankLabels;
    private String[] mLockTypeLabels;
    private EditText et_lock_access_pwd;
    private Button btn_lock_tag;

    //--Kill tag part
    private EditText et_kill_pwd;
    private Button btn_kill_tag;

    private final int MSG_READ_TAG_COMPLETE = 0x01;
    private final int MSG_WRITE_TAG_COMPLETE = 0x02;
    private final int MSG_LOCK_TAG_COMPLETE = 0x03;
    private final int MSG_KILL_TAG_COMPLETE = 0x04;
    private Handler mUIHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case MSG_READ_TAG_COMPLETE:
                    break;
                case MSG_WRITE_TAG_COMPLETE:
                    break;
                case MSG_LOCK_TAG_COMPLETE:
                    break;
                case MSG_KILL_TAG_COMPLETE:
                    break;
            }
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mLayoutView = inflater.inflate(R.layout.layout_tag,null);
        return mLayoutView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    @Override
    public void onResume() {
        super.onResume();
        enableOrDisableAllViews(!mUHFMgr.isInInventory());
        updateFilterData();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onUhfPowerOning() {
        super.onUhfPowerOning();
    }

    @Override
    public void onUhfPowerOn() {
        super.onUhfPowerOn();
    }

    @Override
    public void onUhfPowerOff() {
        super.onUhfPowerOff();
    }

    @Override
    public void onUhfStartInventory()
    {
        enableOrDisableAllViews(false);
    }

    @Override
    public void onUhfStopInventory()
    {
        enableOrDisableAllViews(true);
    }

    private void initView()
    {
        //tag filter
        initTagFilter();
        //Read / Write tag part views
        initReadWritePartViews();
        //Lock tag
        initLockTagViews();
        //Kill tag
        initKillTagViews();
    }

    private void updateFilterData()
    {
        Map<String,Object> settingsMap = mUHFMgr.getAllParams();
        Object obj = settingsMap.get(UHFSilionParams.TAG_FILTER.KEY);
        String sJson = (String)obj;
        if(sJson != null)
        {
            try {
                JSONObject jobj = new JSONObject(sJson);
                int bank = jobj.optInt("bank");
                int startaddr = jobj.optInt("startaddr");
                String sHexData = jobj.optString("fdata");
                int isMatch = jobj.optInt("isInvert");

                int tagIndex = mHistoryTagsList.indexOf(sHexData);
                if(tagIndex == -1) {
                    mHistoryTagsList.add(sHexData);
                    tagIndex = mHistoryTagsList.indexOf(sHexData);
                }

                spinner_filter_tag.setSelection(tagIndex);
                int bankIndex = 0;
                for(int i = 0; i < bank_filter_values.length; i++)
                {
                    int bankId = bank_filter_values[i];
                    if(bankId == bank)
                    {
                        spinner_filter_bank.setSelection(i);
                        break;
                    }
                }

                rg_match_group.check(isMatch == 0 ? R.id.rb_match : R.id.rb_non_match);
                rg_enable_group.check(R.id.rb_tag_filter_enable);

            } catch (Exception e) {
            }

        }else{

            rg_match_group.check(R.id.rb_match);
            rg_enable_group.check(R.id.rb_tag_filter_disable);
        }
    }

    private void enableOrDisableAllViews(boolean enable)
    {
        List<View> viewList = findAllViews(mLayoutView);
        for(View child : viewList)
        {
            child.setEnabled(enable);
        }
    }

    private List<View> findAllViews(View target)
    {
        List<View> viewList = new ArrayList<>();
        if(!(target instanceof ViewGroup))
            viewList.add(target);
        else{
            ViewGroup viewGroup = (ViewGroup) target;
            int count = viewGroup.getChildCount();
            for(int i = 0 ; i < count; i++)
            {
                View child = viewGroup.getChildAt(i);
                List<View> childViewList = findAllViews(child);
                viewList.addAll(childViewList);
            }
        }

        return viewList;
    }

    private void initTagFilter()
    {
        //filter tags
        spinner_filter_tag = (Spinner) mLayoutView.findViewById(R.id.spinner_filter_tag);
        spinner_filter_bank = (Spinner) mLayoutView.findViewById(R.id.spinner_filter_bank);
        rg_match_group = (RadioGroup) mLayoutView.findViewById(R.id.rg_match_group);
        rg_enable_group = (RadioGroup) mLayoutView.findViewById(R.id.rg_enable_group);
        btn_set_filter = (Button) mLayoutView.findViewById(R.id.btn_set_filter);

        List<String> tagList = AppApplication.getInstance().getTagDatas();
        mHistoryTagsList = new ArrayList<>();
        mHistoryTagsList.add(mContext.getString(R.string.none));
        mHistoryTagsList.addAll(tagList);

        mFilterTagAdapter = new ArrayAdapter<String>(mContext,android.R.layout.simple_spinner_item, mHistoryTagsList);
        mFilterTagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_filter_tag.setAdapter(mFilterTagAdapter);

        bank_filter_labels = new String[]{
                mContext.getString(R.string.uhf_write_tag_area_epc),
                mContext.getString(R.string.uhf_write_tag_area_tid),
                mContext.getString(R.string.uhf_write_tag_area_user)
        };

        bank_filter_values = new int[]{
                UHFReader.BANK_TYPE.EPC.value(),
                UHFReader.BANK_TYPE.TID.value(),
                UHFReader.BANK_TYPE.USER.value()
        };

        mFilterBankAdapter = new ArrayAdapter<String>(mContext,android.R.layout.simple_spinner_item, bank_filter_labels);
        mFilterBankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_filter_bank.setAdapter(mFilterBankAdapter);

        btn_set_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTagFilter();
            }
        });
    }

    private void initReadWritePartViews()
    {
        spinner_read_bank = (Spinner) mLayoutView.findViewById(R.id.spinner_read_bank);
        et_start_addr = (EditText) mLayoutView.findViewById(R.id.et_start_addr);
        et_byte_count = (EditText) mLayoutView.findViewById(R.id.et_byte_count);
        et_access_pwd = (EditText) mLayoutView.findViewById(R.id.et_access_pwd);
        et_read_tag_data = (EditText) mLayoutView.findViewById(R.id.et_read_tag_data);
        et_write_tag_data = (EditText) mLayoutView.findViewById(R.id.et_write_tag_data);
        btn_read_tag = (Button) mLayoutView.findViewById(R.id.btn_read_tag);
        btn_write_tag = (Button) mLayoutView.findViewById(R.id.btn_write_tag);

        bank_labels = new String[]{
                mContext.getString(R.string.uhf_write_tag_area_epc),
                mContext.getString(R.string.uhf_write_tag_area_user),
                mContext.getString(R.string.uhf_write_tag_area_hold),
                mContext.getString(R.string.uhf_write_tag_area_tid)
        };

        bank_values = new int[]{
                UHFReader.BANK_TYPE.EPC.value(),
                UHFReader.BANK_TYPE.USER.value(),
                UHFReader.BANK_TYPE.RESERVED.value(),
                UHFReader.BANK_TYPE.TID.value()
        };

        ArrayAdapter adapter_bank = new ArrayAdapter<String>(mContext,android.R.layout.simple_spinner_item, bank_labels);
        adapter_bank.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_read_bank.setAdapter(adapter_bank);
        spinner_read_bank.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int bank = bank_values[position];
                if(bank == UHFReader.BANK_TYPE.EPC.value()){
                    et_start_addr.setText("2");
                    et_byte_count.setText("6");
                }else if(bank == UHFReader.BANK_TYPE.USER.value()){
                    et_start_addr.setText("0");
                    et_byte_count.setText("6");
                }else if(bank == UHFReader.BANK_TYPE.RESERVED.value()){
                    et_start_addr.setText("0");
                    et_byte_count.setText("4");
                }else if(bank == UHFReader.BANK_TYPE.TID.value()){
                    et_start_addr.setText("0");
                    et_byte_count.setText("6");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btn_read_tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readTag();
            }
        });

        btn_write_tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeTag();
            }
        });

    }//End initReadWritePartViews

    private void initLockTagViews()
    {
        spinner_lock_bank = (Spinner) mLayoutView.findViewById(R.id.spinner_lock_bank);
        spinner_lock_type = (Spinner) mLayoutView.findViewById(R.id.spinner_lock_type);
        btn_lock_tag = (Button) mLayoutView.findViewById(R.id.btn_lock_tag);
        et_lock_access_pwd = (EditText) mLayoutView.findViewById(R.id.et_lock_access_pwd);

        mLockBankLabels = new String[]{
                mContext.getString(R.string.access_pwd),
                mContext.getString(R.string.destroy_pwd),
                mContext.getString(R.string.uhf_write_tag_area_epc),
                mContext.getString(R.string.uhf_write_tag_area_tid),
                mContext.getString(R.string.uhf_write_tag_area_user)};
        ArrayAdapter adapter_bank = new ArrayAdapter<String>(mContext,android.R.layout.simple_spinner_item, mLockBankLabels);
        adapter_bank.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_lock_bank.setAdapter(adapter_bank);
        spinner_lock_bank.setSelection(2);

        mLockTypeLabels = new String[]{
                getString(R.string.release_lock),
                getString(R.string.temp_lock) ,
                getString(R.string.fix_lock) };
        ArrayAdapter adapter_lock_type = new ArrayAdapter<String>(mContext,android.R.layout.simple_spinner_item, mLockTypeLabels);
        adapter_lock_type.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_lock_type.setAdapter(adapter_lock_type);


        btn_lock_tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lockTag();
            }
        });

    }//End initLockTagViews

    private void initKillTagViews()
    {
        et_kill_pwd = (EditText) mLayoutView.findViewById(R.id.et_kill_pwd);
        btn_kill_tag = (Button) mLayoutView.findViewById(R.id.btn_kill_tag);

        btn_kill_tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                killTag();
            }
        });
    }

    /**
     * Set tag filter
     */
    private void setTagFilter()
    {
        int tagPos = spinner_filter_tag.getSelectedItemPosition();
        String tagDataHex = mHistoryTagsList.get(tagPos);

        UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR;
        boolean filterEnable = rg_enable_group.getCheckedRadioButtonId() == R.id.rb_tag_filter_enable;
        if(tagPos > 0 && filterEnable)
        {
            int isMatch = rg_match_group.getCheckedRadioButtonId() == R.id.rb_match ? 0 : 1;

            int bankPos = spinner_filter_bank.getSelectedItemPosition();
            int bankId = bank_filter_values[bankPos];
            int startAddrBits = 0;
            if(bankId == UHFReader.BANK_TYPE.EPC.value())
                startAddrBits = 32;

            try {
                //,{"bank":1,"fdata":0A0B,"flen:"8,"startaddr":2,"isInvert":1}
                JSONObject jsFilter = new JSONObject();
                jsFilter.put("bank", bankId);
                jsFilter.put("startaddr", startAddrBits);
                jsFilter.put("fdata", tagDataHex);
                jsFilter.put("isInvert", isMatch);

                String sValue = jsFilter.toString();
                er = mUHFMgr.setParam(UHFSilionParams.TAG_FILTER.KEY, UHFSilionParams.TAG_FILTER.PARAM_TAG_FILTER, sValue);
                Log.d(TAG,"Set tag filter: "+er.name());
            } catch (Exception e) {
            }

        } else{
            er = mUHFMgr.setParam(UHFSilionParams.TAG_FILTER.KEY, UHFSilionParams.TAG_FILTER.PARAM_CLEAR, "1");
        }

        Toast.makeText(mContext,er == UHFReader.READER_STATE.OK_ERR ? R.string.setting_success : R.string.setting_fail,Toast.LENGTH_SHORT).show();
        Log.d(TAG,"Set tag filter: "+er.name());
    }

    private void readTag()
    {
        btn_read_tag.setEnabled(false);
        try {
            //bank
            int pos = spinner_read_bank.getSelectedItemPosition();
            int bank = bank_values[pos];

            //start address
            String sAddr = et_start_addr.getText().toString();
            int startAddrBlock = 0;
            if(sAddr != null && TextUtils.isDigitsOnly(sAddr))
                startAddrBlock = Integer.parseInt(sAddr);

            UHFReader.READER_STATE er=UHFReader.READER_STATE.CMD_FAILED_ERR;
            int trycount=3;
            byte[] rdata = null;
            do{


                //block count
                String sBlockCount = et_byte_count.getText().toString();
                int blkcnt  = 0;
                if(sBlockCount != null && TextUtils.isDigitsOnly(sBlockCount))
                    blkcnt = Integer.parseInt(sBlockCount);

                //access password
                String sHexPasswd = et_access_pwd.getText().toString();
                if(TextUtils.isEmpty(sHexPasswd))
                    sHexPasswd = "";

                rdata = mUHFMgr.GetTagData(bank, startAddrBlock, blkcnt, sHexPasswd);
                trycount--;
                if(trycount<1)
                    break;

            }while(rdata == null);

            if(rdata != null)
            {
                String tagData=UHFReader.Hex2Str(rdata);
                et_read_tag_data.setText(tagData);
                //Add to global cache
                AppApplication.getInstance().addTagData(tagData);
                if(!mHistoryTagsList.contains(tagData))
                {
                    mHistoryTagsList.add(tagData);
                    mFilterTagAdapter.notifyDataSetChanged();
                }

                Toast.makeText(mContext, R.string.success,Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(mContext, R.string.failed,Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(mContext, "Exception:"+e.getMessage(),Toast.LENGTH_SHORT).show();
        }

        btn_read_tag.setEnabled(true);

    }//End readTag

    private void writeTag()
    {

        btn_write_tag.setEnabled(false);
        try {

            //bank
            int pos = spinner_read_bank.getSelectedItemPosition();
            int bank = bank_values[pos];

            //start address
            String sAddr = et_start_addr.getText().toString();
            int startAddrBlock = 0;
            if(sAddr != null && TextUtils.isDigitsOnly(sAddr))
                startAddrBlock = Integer.parseInt(sAddr);

            //block count
            String sBlockCount = et_byte_count.getText().toString();
            int blkcnt  = 0;
            if(sBlockCount != null && TextUtils.isDigitsOnly(sBlockCount))
                blkcnt = Integer.parseInt(sBlockCount);

            //access password
            String sHexPasswd = et_access_pwd.getText().toString();
            if(TextUtils.isEmpty(sHexPasswd))
                sHexPasswd = "";

            //tag data
            String sHexData = et_write_tag_data.getText().toString();
            byte[] data = UHFReader.Str2Hex(sHexData);
            UHFReader.READER_STATE er= UHFReader.READER_STATE.OK_ERR;
            int trycount=3;
            do{
                if(bank == UHFReader.BANK_TYPE.EPC.value())
                    er = mUHFMgr.writeTagEpcEx(data,sHexPasswd);
                else
                    er = mUHFMgr.writeTagData(bank, startAddrBlock, data, sHexPasswd);
                trycount--;
                if(trycount<1)
                    break;
            }while(er != UHFReader.READER_STATE.OK_ERR);


            if( er == UHFReader.READER_STATE.OK_ERR)
            {
                Toast.makeText(mContext, getString(R.string.success)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
            }
            else
                Toast.makeText(mContext, getString(R.string.failed)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

        }catch (Exception e) {
            Toast.makeText(mContext, "Exception :"+e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        btn_write_tag.setEnabled(true);

    }//writeTag

    private void lockTag()
    {
        try {

            //setTagFilter();
            //access password
            String sHexPasswd = et_lock_access_pwd.getText().toString();
            if(TextUtils.isEmpty(sHexPasswd))
                sHexPasswd = "";

            UHFReader.Lock_Obj lobj = null;
            UHFReader.Lock_Type ltyp=null;
            int lbank=spinner_lock_bank.getSelectedItemPosition();
            int ltype=spinner_lock_type.getSelectedItemPosition();
            if(lbank==0)
            {
                lobj=UHFReader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD;
                if(ltype==0)
                    ltyp=UHFReader.Lock_Type.UNLOCK;//解锁定
                else if(ltype==1)
                    ltyp= UHFReader.Lock_Type.LOCK; //暂时锁定
                else if(ltype==2)
                    ltyp= UHFReader.Lock_Type.PERM_LOCK;//永久锁定

            }
            else if(lbank==1)
            {
                lobj= UHFReader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD;
                if(ltype==0)
                    ltyp= UHFReader.Lock_Type.UNLOCK;
                else if(ltype==1)
                    ltyp= UHFReader.Lock_Type.LOCK;
                else if(ltype==2)
                    ltyp= UHFReader.Lock_Type.PERM_LOCK;
            }
            else if(lbank==2)
            {
                lobj= UHFReader.Lock_Obj.LOCK_OBJECT_BANK1; //EPC分区
                if(ltype==0)
                    ltyp= UHFReader.Lock_Type.UNLOCK;
                else if(ltype==1)
                    ltyp= UHFReader.Lock_Type.LOCK;
                else if(ltype==2)
                    ltyp= UHFReader.Lock_Type.PERM_LOCK;
            }
            else if(lbank==3)
            {
                lobj= UHFReader.Lock_Obj.LOCK_OBJECT_BANK2;//TID分区
                if(ltype==0)
                    ltyp= UHFReader.Lock_Type.UNLOCK;
                else if(ltype==1)
                    ltyp= UHFReader.Lock_Type.LOCK;
                else if(ltype==2)
                    ltyp= UHFReader.Lock_Type.PERM_LOCK;
            }
            else if(lbank==4)
            {
                lobj= UHFReader.Lock_Obj.LOCK_OBJECT_BANK3;//USER分区
                if(ltype==0)
                    ltyp= UHFReader.Lock_Type.UNLOCK;
                else if(ltype==1)
                    ltyp= UHFReader.Lock_Type.LOCK;
                else if(ltype==2)
                    ltyp= UHFReader.Lock_Type.PERM_LOCK;
            }

            UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR;
            er = mUHFMgr.LockTag(lobj.value(), ltyp.value(), sHexPasswd);

            if( er == UHFReader.READER_STATE.OK_ERR)
            {
                Toast.makeText(mContext, getString(R.string.success)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
            }
            else
                Toast.makeText(mContext, getString(R.string.failed)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(mContext, "Exception :"+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void killTag()
    {
        try {
            //setTagFilter();
            //access password
            String sKillPasswd = et_kill_pwd.getText().toString();
            if(TextUtils.isEmpty(sKillPasswd))
            {
                Toast.makeText(mContext, "Error:  need kill password.", Toast.LENGTH_SHORT).show();
                return ;
            }

            UHFReader.READER_STATE er = mUHFMgr.destroyTag(sKillPasswd);

            if( er == UHFReader.READER_STATE.OK_ERR)
            {
                Toast.makeText(mContext, getString(R.string.success)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
            }
            else
                Toast.makeText(mContext, getString(R.string.failed)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

        } catch(Exception e) {
            Toast.makeText(mContext, "Exception :"+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
