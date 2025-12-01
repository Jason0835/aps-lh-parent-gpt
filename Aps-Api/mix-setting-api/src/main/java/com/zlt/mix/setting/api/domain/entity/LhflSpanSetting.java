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
 * 硫磺辅料跨区设置对象 t_lhfl_span_setting
 * 
 * @author chen
 * @date 2022-08-12
 */
@ApiModel(value = "硫磺辅料跨区设置对象", description = "硫磺辅料跨区设置对象 ")
@TableName("t_lhfl_span_setting")
@KeySequence(value = "seq_t_lhfl_span_setting", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class LhflSpanSetting extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_T_LHFL_SPAN_SETTING */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_LHFL_SPAN_SETTING", position = 10)
    private Long id;
    /** 委托密炼区(对应数据字典code：MIX_AREA) */
    @Excel(name = "setting.lhflSpanSetting.entrustMixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.lhflSpanSetting.entrustMixArea", required=true,maxLength=10)
    @ApiModelProperty(value = "委托密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String entrustMixArea;
    /** 被委托密炼区(对应数据字典code：MIX_AREA) */
    @Excel(name = "setting.lhflSpanSetting.entrustedMixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.lhflSpanSetting.entrustedMixArea", required=true,maxLength=10)
    @ApiModelProperty(value = "被委托密炼区(对应数据字典code：MIX_AREA)", position = 30)
    private String entrustedMixArea;
    /** 硫磺辅料名称 */
    @Excel(name = "setting.lhflSpanSetting.materialName")
    @ImportValidated(name = "setting.lhflSpanSetting.materialName", required=true,maxLength=50)
    @ApiModelProperty(value = "硫磺辅料名称", position = 40)
    private String materialName;
    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength=300)
    @ApiModelProperty(value = "备注", position = 100)
    private String remark;

}
