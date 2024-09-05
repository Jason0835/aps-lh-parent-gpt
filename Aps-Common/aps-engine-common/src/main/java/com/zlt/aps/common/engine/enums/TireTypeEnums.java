package com.zlt.aps.common.engine.enums;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.constants.PatternConstants;

import java.util.regex.Pattern;

/**
  * 轮胎类型枚举
  * @ClassName TireTypeEnums
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/28 14:47
  * @Version 1.0
**/
public enum TireTypeEnums {

    SPARE_TIRE("0","备胎"),LIGHT_TRUCK_TIRE("1","轻卡胎"),FLAG_TIRE("2","缺气保用胎"),COMMON_TIRE("3","普通胎");
    /**
     * 轮胎类型编码,对应数据字典
     */
    private String tireTypeCode;

    /**
     * 轮胎类型
     */
    private String tireTypeName;

    private TireTypeEnums(String tireTypeCode, String tireTypeName){
        this.tireTypeCode=tireTypeCode;
        this.tireTypeName=tireTypeName;
    }

    /**
     * 解析轮胎规格获取对应的轮胎类型
     * @param specDesc
     * @return
     */
    public static String getTireTypeCode(String specDesc){
        String tireTypeCode="";
        if(StringUtils.isNotEmpty(specDesc)){
            if(StringUtils.startsWithIgnoreCase(specDesc,"T")){//备胎以T开头
                tireTypeCode=TireTypeEnums.SPARE_TIRE.tireTypeCode;
            }else if(Pattern.compile(PatternConstants.RC_RULE_PATTERN).matcher(specDesc).matches()
                    ||Pattern.compile(PatternConstants.LT_ST_RULE_PATTERN).matcher(specDesc).matches()){//轻卡胎
                tireTypeCode= TireTypeEnums.LIGHT_TRUCK_TIRE.tireTypeCode;
            }else if(Pattern.compile(PatternConstants.RF_LRF_RULE_PATTERN).matcher(specDesc).matches()){//缺气保用胎
                tireTypeCode= TireTypeEnums.FLAG_TIRE.tireTypeCode;
            }else{
                tireTypeCode= TireTypeEnums.COMMON_TIRE.tireTypeCode;
            }
        }
        return tireTypeCode;
    }
}
