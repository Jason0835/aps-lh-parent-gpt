package com.zlt.aps.nc.engine.vo;

import lombok.Data;

@Data
public class NcScheduleBaseInfoVo {

    /**
     * 对应的成型批次号
     */
    private String cxBatchNo;

    /**
     * 内衬代码
     */
    private String liningCode;

    /**
     * 胶料代码
     */
    private String glueCode;

    private String wholeGlueCode;

    /**
     * 单耗（毫米）
     */
    private Double unitConsume;

    /**
     * 对应成型一班的内衬胶计划量
     */
    private Double cxClass1Plan;

    /**
     * 对应成型二班的内衬胶计划量
     */
    private Double cxClass2Plan;

    /**
     * 对应成型三班的内衬胶计划量
     */
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的内衬胶计划量
     */
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的内衬胶计划量
     */
    private Double cxClass5Plan;

    /**
     * 机台code$胎胚代码，多个逗号分割， 用来计算成型平均定额使用
     */
    private String quotaKeys;
}
