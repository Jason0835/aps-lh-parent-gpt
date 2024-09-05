package com.zlt.aps.common.engine.constants;

/**
 * 配置正则表达式常量
 */
public class PatternConstants {

    //轻卡胎正则R+寸口+C
    public static final String RC_RULE_PATTERN = "^.*(R)([0-9]{1,4})+(\\.?[0-9]{0,2})(C|LT).*$";
    //轻卡胎正则LT或ST+断面宽
    public static final String LT_ST_RULE_PATTERN = "^(LT|ST)([0-9]*)+(\\.?[0-9]{0,2})/([0-9]*)+(\\.?[0-9]{0,2}).*$";
    //缺气保用胎规则RF+寸口或者找LRF或者找LRS
    public static final String RF_LRF_RULE_PATTERN = "^.*(RF|LRF|LRS)([0-9]*)+(\\.?[0-9]{0,2}).*$";
}
