package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎侧日完成量MES回报快照。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_TC_DAY_FINISH_QTY")
@ApiModel(value = "胎侧日完成量MES回报", description = "MES胎侧日完成量单总量快照")
public class TcDayFinishQty extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** MES回报业务日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    @ApiModelProperty(value = "MES回报业务日期", name = "scheduleDate")
    private Date scheduleDate;

    /** 胎侧编码。 */
    @TableField("SIDEWALL_CODE")
    @ApiModelProperty(value = "胎侧编码", name = "sidewallCode")
    private String sidewallCode;

    /** 日完成量。 */
    @TableField("FINISH_QTY")
    @ApiModelProperty(value = "日完成量", name = "finishQty")
    private BigDecimal finishQty;

    /** MES数据版本。 */
    @TableField("DATA_VERSION")
    @ApiModelProperty(value = "MES数据版本", name = "dataVersion")
    private String dataVersion;

    /** 公司编码。 */
    @TableField("COMPANY_CODE")
    @ApiModelProperty(value = "公司编码", name = "companyCode")
    private String companyCode;

    /** 工厂编码。 */
    @TableField("FACTORY_CODE")
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    private String factoryCode;
}
