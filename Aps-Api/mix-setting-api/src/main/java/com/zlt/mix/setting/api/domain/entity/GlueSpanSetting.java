package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.zlt.mix.common.core.annotation.ImportValidated;

/**
 * 终炼母炼胶料跨区设置对象 t_glue_span_setting
 *
 * @author chen
 * @date 2022-08-12
 */
@ApiModel(value = "终炼母炼胶料跨区设置对象", description = "终炼母炼胶料跨区设置对象 ")
@TableName("t_glue_span_setting")
@KeySequence(value = "seq_t_glue_span_setting", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueSpanSetting extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_GLUE_SPAN_SETTING
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_SPAN_SETTING", position = 10)
    private Long id;
    /**
     * 委托密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.glueSpanSetting.entrustMixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.glueSpanSetting.entrustMixArea", required = true, maxLength = 10)
    @ApiModelProperty(value = "委托密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String entrustMixArea;
    /**
     * 被委托密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.glueSpanSetting.entrustedMixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.glueSpanSetting.entrustedMixArea", required = true, maxLength = 10)
    @ApiModelProperty(value = "被委托密炼区(对应数据字典code：MIX_AREA)", position = 30)
    private String entrustedMixArea;
    /**
     * 胶料名称
     */
    @Excel(name = "setting.glueSpanSetting.glue")
    @ImportValidated(name = "setting.glueSpanSetting.glue", required = true, maxLength = 30)
    @ApiModelProperty(value = "胶料名称", position = 40)
    private String glue;
    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 100)
    private String remark;
}
