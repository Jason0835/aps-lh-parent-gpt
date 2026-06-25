package com.zlt.aps.gdyy.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 钢带压延定额设定对象 t_gdyy_quota_setting
 * 
 * @author chen
 * @date 2021-06-30
 */
@Data
@ApiModel(value = "钢带压延定额设定对象", description = "钢带压延定额设定对象 ")
public class GdyyQuotaSettingDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_QUOTA_SETTING */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 钢带大卷编号 */
    @Excel(name = "ui.data.column.gdyy.quota.bigRollCode", sort = 10)
    @ApiModelProperty(value = "钢带大卷编号")
    @ImportValidated(name = "ui.data.column.gdyy.quota.bigRollCode", required = true, isCode = true, maxLength = 20)
    private String bigRollCode;

    /** 定额 */
    @Excel(name = "ui.data.column.quota.quota", sort = 20)
    @ApiModelProperty(value = "定额")
    @ImportValidated(name = "ui.data.column.quota.quota", required = true, number = true, min = 0, max = 9999999)
    private BigDecimal quota;

    @Excel(name = "ui.common.column.remark", sort = 30)
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
