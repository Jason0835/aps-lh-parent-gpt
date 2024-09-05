package com.zlt.aps.common.core.enums;

import com.ruoyi.common.utils.StringUtils;
import lombok.Getter;

/**
 * APS模具变动计划更换模具类型
 */
@Getter
public enum MoldChangeTypeEnums {

    LEFT_CLOSE_MERGE("左模收尾合并","Left die close merge","1"),
    RIGHT_CLOSE_MERGE("右模收尾合并","Right die close merge","2"),
    LEFT_POINT_MERGE("左模点数合并","Left modulo number merge","3"),
    RIGHT_POINT_MERGE("右模点数合并","Right modulo number merge","4"),
    LEFT_MOLD_MERGE("左模合并","Left modular merge","5"),
    RIGHT_MOLD_MERGE("右模合并","Right modular merge","6"),
    CLOSE_OUT_CHANGE("收尾换","Closing change","7"),
    SPLIT_OUT_CHANGE("拆模换","Formwork removal and replacement","8"),
    POINT_OUT_CHANGE("点数换","Point exchange","9"),
    CHANGE_ORDER_NO("换工单号","change order No","10"),
    CLOSE_OUT_MEGER("收尾合并","Close out consolidation","11"),
    SPLIT_MOLD_MEGER("拆模合并","Formwork removal and consolidation","12");
    private String zhName;
    private String enName;
    private String value;

    /**
     * 国际化中文
     */
    public static final String  LANG_ZH_CN="zh_CN";

    /**
     * 国际化英语
     */
    public static final String  LANG_EN_US="en_US";


    private MoldChangeTypeEnums(String zhName, String enName, String value) {
        this.zhName=zhName;
        this.enName=enName;
        this.value=value;
    }

    /**
     *
     * @param name
     * @param lang
     * @return
     */
    public static String getMoldChangeTypeByName(String name,String lang){
       if(StringUtils.isEmpty(lang)){
           lang=LANG_ZH_CN;
       }
       for(MoldChangeTypeEnums moldChangeTypeEnums:MoldChangeTypeEnums.values()){
           String enName=moldChangeTypeEnums.getEnName();
           String zhName=moldChangeTypeEnums.getZhName();
            if(LANG_EN_US.equals(lang)&&enName.equals(name)){
                return moldChangeTypeEnums.getValue();
            }else if(zhName.equals(name)){
                return moldChangeTypeEnums.getValue();
            }
       }
       return null;
    }

    /**
     * 通过字典值获取枚举对象
     * @param value
     * @return
     */
    public static MoldChangeTypeEnums getMoldChangeTypeByValue(String value) {

        if(StringUtils.isEmpty(value)){
            return null;
        }

        for (MoldChangeTypeEnums type : MoldChangeTypeEnums.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
