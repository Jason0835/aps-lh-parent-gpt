package com.zlt.aps.gsq.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 钢丝圈库存管理导出VO
 * 用于导出时按指定模板样式输出
 *
 * @author zlt
 * @date 2026-07-08
 */
@Data
public class GsqStockExportVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 库存日期 */
    @Excel(name = "ui.data.column.gsq.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date stockDate;

    /** 钢丝圈代码 */
    @Excel(name = "ui.data.column.gsq.stock.steelRingCode")
    private String steelRingCode;

    /** 库存量(米) */
    @Excel(name = "ui.data.column.gsq.stock.stockNum", scale = 1)
    private BigDecimal stockNum;

    /** 修正数量(米) */
    @Excel(name = "ui.data.column.gsq.stock.modifyNum", scale = 1)
    private BigDecimal modifyNum;

    /** 不良数量(米) */
    @Excel(name = "ui.data.column.gsq.stock.badNum", scale = 1)
    private BigDecimal badNum;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    private String remark;

    /** 更新时间 */
    @Excel(name = "ui.data.column.gsq.stock.updateDate", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
