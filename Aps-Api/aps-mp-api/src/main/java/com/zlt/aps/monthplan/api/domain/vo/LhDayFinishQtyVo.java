package com.zlt.aps.monthplan.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author Chen
 * @date 2025/4/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "硫化排程日完成量对象", description = "硫化排程日完成量对象")
public class LhDayFinishQtyVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhDayFinishQty.finishDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "finishDate")
    @TableField(value = "FINISH_DATE")
    private Date finishDate;

    /**
     * 物料代码
     */
    @ApiModelProperty(value = "物料代码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 规格代码
     */
    @Excel(name = "ui.data.column.lhDayFinishQty.specCode")
    @ApiModelProperty(value = "规格代码", name = "specsCode")
    @TableField(value = "SPECS_CODE")
    private String specCode;

    /**
     * 胎胚日完成量
     */
    @Excel(name = "ui.data.column.lhDayFinishQty.dayFinishQty")
    @ApiModelProperty(value = "胎胚日完成量", name = "dayFinishQty")
    @TableField(value = "DAY_FINISH_QTY")
    private Integer dayFinishQty;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.lhDayFinishQty.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;
}
