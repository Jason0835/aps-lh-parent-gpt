package com.zlt.aps.cx.engine.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.Date;

/**
 * 成型工序在投产规格
 */
@ApiModel(value = "成型工序在产规格对象", description = "成型工序在产规格对象")
@Data
public class CxInProductionSpec extends ApsBaseEntity {

    private Long id;

    /**
     * 生产日期
     */
    private Date productDate;

    /**
     * 成型机台编号
     */
    private String cxMachineCode;

    /**
     * 胎胚对应sap品号
     */
    private String sapCode;

    /**
     * 胎胚代码
     */
    private String embryoCode;

    /**
     * 胎胚施工版本信息
     */
    private String bomDataVersion;

    /**
     * 生产日期yyyyMMdd搜索条件
     */
    private String productDateStr;
}
