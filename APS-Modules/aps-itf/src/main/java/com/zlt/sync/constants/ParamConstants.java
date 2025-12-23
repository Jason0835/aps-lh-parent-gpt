package com.zlt.sync.constants;

/**
 *  基础常量
 */
public class ParamConstants {

    public static final String DEFAULTS = "defaults";

    // SEARCH_TYPE部分
    public static final String SYNC_SUCCESSED_METHOD = "syncSuccessdTask";
    public static final String SYNC_SUCCESSED_CNT_METHOD = "syncSuccessdCntTask";

    public static final String SEAR_TYPE_SUCCESSED_METHOD = "syncSuccessd";
    public static final String SEAR_TYPE_SUCCESSED_CNT_METHOD = "syncSuccessdCnt";

    public static final String SEAR_TYPE_CAN_SYNC_DATAS = "canSyncDatas";

    // CUSTOMS 部分
    // {"methodFrom":"syncKeys", syncKeys:"xxx,xxxx", method: "", bean: "sapHandle"}
    public static final String CUSTOM_KEY_METHOD_FROM = "methodFrom";
    public static final String CUSTOM_KEY_SYNC_KEYS = "syncKeys";
    public static final String CUSTOM_KEY_METHOD = "method";
    public static final String CUSTOM_KEY_BEAN = "bean";

    public static final String CUSTOM_VALUE_METHOD_FROM_SYNCKEYS = "syncKeys";
    public static final String CUSTOM_VALUE_METHOD_FROM_CUSTOM = "custom";

    // 其他
    public static final String DEFAULT_START_DATE = "1970-01-01";



}
