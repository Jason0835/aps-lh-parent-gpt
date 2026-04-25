package com.zlt.aps.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * APS结构整车胎面配置
 *
 * @author zlt
 * @since 2026/04/09
 */
@ApiModel(value = "APS结构整车胎面配置", description = "APS结构整车胎面配置")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_STRUCTURE_TREAD_CONFIG")
public class CxStructureTreadConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂")
    @Excel(name = "ui.data.column.cxStructureTreadConfig.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true)
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "结构")
    @Excel(name = "ui.data.column.cxStructureTreadConfig.structureCode")
    @ImportExcelValidated(required = true)
    @TableField(value = "STRUCTURE_CODE")
    private String structureCode;

    @ApiModelProperty(value = "整车胎面条数")
    @Excel(name = "ui.data.column.cxStructureTreadConfig.treadCount")
    @ImportExcelValidated(required = true)
    @TableField(value = "TREAD_COUNT")
    private Integer treadCount;

    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

}
