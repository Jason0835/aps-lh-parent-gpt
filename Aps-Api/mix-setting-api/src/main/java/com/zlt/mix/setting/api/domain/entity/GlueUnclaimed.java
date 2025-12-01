package com.zlt.mix.setting.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.zlt.mix.common.core.annotation.ImportValidated;

/**
 * 胶料白班待支领对象 t_glue_unclaimed
 * 
 * @author zlt
 * @date 2022-09-05
 */
@ApiModel(value = "胶料白班待支领对象", description = "胶料白班待支领对象 ")
@TableName("t_glue_unclaimed")
@KeySequence(value = "SEQ_GLUE_UNCLAIMED", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueUnclaimed extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    public GlueUnclaimed() {
    }

    public GlueUnclaimed(String mixArea, Date scheduleDate, String glue, Integer shelfNum2, String remark) {
        this.mixArea = mixArea;
        this.scheduleDate = scheduleDate;
        this.glue = glue;
        this.shelfNum2 = shelfNum2;
        this.remark = remark;
    }

    /** 主键ID，对应自增序列为：SEQ_GLUE_UNCLAIMED */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_GLUE_UNCLAIMED", position = 10)
    private Long id;
    /** 密炼区(对应数据字典code：MIX_AREA) */
    @Excel(name = "setting.unclaimed.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.unclaimed.mixArea", maxLength=10)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /** 排产日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "setting.unclaimed.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排产日", position = 30)
    private Date scheduleDate;
    /** 胶料 */
    @Excel(name = "setting.unclaimed.glue")
    @ImportValidated(name = "setting.unclaimed.glue", maxLength=16, required = true)
    @ApiModelProperty(value = "胶料", position = 40)
    private String glue;
    /** 白班待支领数1，通过MES出库数据计算得到 */
    @Excel(name = "setting.unclaimed.shelfNum1")
    @ImportValidated(name = "setting.unclaimed.shelfNum1", number=true, min=0, max=99999999)
    @ApiModelProperty(value = "白班待支领数1，通过MES出库数据计算得到", position = 50)
    private Integer shelfNum1;

    /** 白班待支领数2，通过导入记录 */
    @Excel(name = "setting.unclaimed.shelfNum2")
    @ImportValidated(name = "setting.unclaimed.shelfNum2", number=true, min=0, max=99999999, required = true)
    @ApiModelProperty(value = "白班待支领数2，通过导入记录", position = 60)
    private Integer shelfNum2;
    /** 备注 */
    @Excel(name = "ui.remark")
    @ImportValidated(name = "ui.remark", maxLength=300)
    @ApiModelProperty(value = "备注", position = 70)
    private String remark;

}
