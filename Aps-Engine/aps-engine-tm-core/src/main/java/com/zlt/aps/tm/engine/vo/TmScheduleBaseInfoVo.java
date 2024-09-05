package com.zlt.aps.tm.engine.vo;

import lombok.Data;

import java.util.Date;

@Data
public class TmScheduleBaseInfoVo  {

    /**
     * 对应的成型批次号
     */
    private String cxBatchNo;

    /**
     * 规格描述信息
     */
    private String specDesc;

    /**
     * 施工代码，即胎胚代码
     */
    private String workCode;

    /**
     * 胎面代码
     */
    private String treadCode;

    /**
     * 胶料代码
     */
    private String glueCode;

    private String wholeGlueCode;

    /**
     * 口型板代码
     */
    private String mouthPlateCode;

    /**
     * 单耗（毫米）
     */
    private Double unitConsume;

    /**
     * 补强/封口胶
     */
    private String reinforceSealGlue;

    /**
     * 对应成型一班的胎面胶计划量
     */
    private Double cxClass1Plan;

    /**
     * 对应成型二班的胎面胶计划量
     */
    private Double cxClass2Plan;

    /**
     * 对应成型三班的胎面胶计划量
     */
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的胎面胶计划量
     */
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的胎面胶计划量
     */
    private Double cxClass5Plan;

    /**
     * 机台code$胎胚代码，多个逗号分割， 用来计算成型平均定额使用
     */
    private String quotaKeys;
}
