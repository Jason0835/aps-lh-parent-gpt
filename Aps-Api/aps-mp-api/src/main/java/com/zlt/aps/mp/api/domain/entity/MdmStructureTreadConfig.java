package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * APS结构整车胎面配置
 *
 * @author zlt
 * @since 2025/12/25
 */
@ApiModel(value = "APS结构整车胎面配置", description = "APS结构整车胎面配置")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_STRUCTURE_TREAD_CONFIG")
public class MdmStructureTreadConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "结构")
    @TableField(value = "STRUCTURE_CODE")
    private String structureCode;

    @ApiModelProperty(value = "整车胎面条数")
    @TableField(value = "TREAD_COUNT")
    private Integer treadCount;

    @ApiModelProperty(value = "删除标识：0-正常，1-已删除")
    @TableField(value = "DEL_FLAG")
    private Integer delFlag;

    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

}
