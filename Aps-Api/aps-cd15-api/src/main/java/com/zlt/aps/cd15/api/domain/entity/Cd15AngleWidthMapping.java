package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * CD15角度宽度对应关系
 */
@Data
@ApiModel(value = "CD15角度宽度对应关系", description = "CD15角度宽度对应关系")
@TableName("t_cd15_angle_width_mapping")
public class Cd15AngleWidthMapping extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15AngleWidthMapping.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    private String factoryCode;

    /** 裁断角度 */
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("CUT_ANGLE")
    @Excel(name = "ui.data.column.cd15AngleWidthMapping.cutAngle", dictType = "cd15_cut_angle")
    @ApiModelProperty(value = "裁断角度", name = "cutAngle")
    private String cutAngle;

    /** 该角度支持的最大宽度 */
    @ImportExcelValidated(required = true)
    @TableField("CLOTH_WIDTH_MAX")
    @Excel(name = "ui.data.column.cd15AngleWidthMapping.clothWidthMax")
    @ApiModelProperty(value = "该角度支持的最大宽度", name = "clothWidthMax")
    private BigDecimal clothWidthMax;
}
