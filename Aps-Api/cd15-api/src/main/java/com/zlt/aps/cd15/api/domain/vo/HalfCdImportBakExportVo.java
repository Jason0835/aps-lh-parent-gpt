package com.zlt.aps.cd15.api.domain.vo;

import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：HalfYcImportBak.java
 * 描    述：线下计划导入对象 half_yc_import_bak
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-26
 */
@ApiModel(value = "线下计划导入对象", description = "线下计划导入对象")
@Data
public class HalfCdImportBakExportVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 代码
     */
    private String code;

    /**
     * 机台名称
     */
    private String machineName;

    /**
     * 夜班计划量
     */
    private Double dayPlanQty;

    /**
     * 早班计划量
     */
    private Double nightPlanQty;

    /**
     * 次日夜班班计划量
     */
    private Double nextDayPlanQty;

    /**
     * 卷长
     */
    private Double curlLength;

    /**
     * 夜班计划卷数
     */
    private Double dayPlanQtyRollNum;

    /**
     * 早班计划卷数
     */
    private Double nightPlanQtyRollNum;

    /**
     * 次日夜班计划卷数
     */
    private Double nextDayPlanQtyRollNum;

    /**
     * 成型计划
     */
    private Integer cxPlanQty;
}
