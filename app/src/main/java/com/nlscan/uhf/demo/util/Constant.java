package com.nlscan.uhf.demo.util;

/**
 * Created by zhouyi on 2019/2/26.
 */

public class Constant {
    public static final int UNKOWNERR = -1;
    public static final int NO_UHF = 0; //没有检测到UHF模块
    public static final int HAVE_UHF = 1; //检测到UHF模块
    public static final int UHF_POWERON = 2; //UHF上电成功
    public static final int UHF_POWERON_ERR = 3; //UHF上电失败

    public static final int EPC_INPUT_BY_WRITE_OR_SCAN_ONE = 4;//用户扫描或手动输入一个EPC
    public static final int EPC_INPUT_BY_SCAN_MORE = 5;//用户扫描输入多个EPC

    public static final int GET_UHF_TAG_FROM_BROADCAST = 6;//获得uhf标签

    public static final int WRITE_DATA_TO_XLS_SUCCESS = 7;//数据写入xls成功

    public static final int WRITE_DATA_TO_XLS_FAILED = 8;//数据写入xls失败

    public static final int GET_SCAN_DATA = 9; //获得扫描结果

    public final static String SCAN_RESULT_1 = "scan_result_1";
    public final static String SCAN_RESULT_2 = "scan_result_2";
    public final static String SCAN_BROADTYPE = "scan_board";


    /**
     * 读码结果发送的广播action
     */
    public final static String ACTION_UHF_RESULT_SEND = "nlscan.intent.action.uhf.ACTION_RESULT";
    /**
     * 读码结果发送的广播Extra
     */
    public final static String EXTRA_TAG_INFO = "tag_info";

    //UR90[芯联创：MODOULE_SLR1200]
    public final static String NEWLAND_MODULE_NAME_UR90 = "UR90";
    //[芯联创：MODOULE_SLR1200]
    public final static String SILION_MODULE_NAME_SLR1200 = "MODOULE_SLR1200";

    //UR90_V2.0[芯联创：MODOULE_SIM7100]
    public final static String NEWLAND_MODULE_NAME_UR90_V2_0 = "UR90_V2.0";
    //[芯联创：MODOULE_SIM7100]
    public final static String SILION_MODULE_NAME_SIM7100 = "MODOULE_SIM7100";

    //==============================

    //当前模块名称（芯联创的型号）
    public static String mCurSilionModuleName = SILION_MODULE_NAME_SLR1200;

    //是否是UR90_V2.0[芯联创：MODOULE_SIM7100]
    public static boolean isSIM7100(String moduleName)
    {
        return SILION_MODULE_NAME_SIM7100.equals(moduleName) || NEWLAND_MODULE_NAME_UR90_V2_0.equals(moduleName);
    }

    //是否是UR90[Silion：MODOULE_SLR1200]
    public static boolean isSLR1200(String moduleName)
    {
        return SILION_MODULE_NAME_SLR1200.equals(moduleName) || NEWLAND_MODULE_NAME_UR90.equals(moduleName);
    }
}
