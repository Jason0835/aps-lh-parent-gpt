package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 硫化在机信息表
 */
@ApiModel(value = "硫化在机信息表", description = "硫化在机信息表")
@Data
@TableName(value = "T_MDM_LH_MACHINE_ONLINE_INFO")
public class MdmLhMachineOnlineInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 日期 */
    @ApiModelProperty(value = "日期", name = "onlineDate")
    @TableField(value = "ONLINE_DATE")
    private Date onlineDate;

    /** 硫化机台 */
    @ApiModelProperty(value = "硫化机台", name = "lhCode")
    @TableField(value = "LH_CODE")
    private String lhCode;

    /** 在机物料编码（NC） */
    @ApiModelProperty(value = "在机物料编码（NC）", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 在机物料编码（MES） */
    @ApiModelProperty(value = "在机物料编码（MES）", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 在机物料描述 */
    @ApiModelProperty(value = "在机物料描述", name = "specDesc")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /** 左右模 */
    @ApiModelProperty(value = "左右模", name = "lrMolds")
    @TableField(value = "LR_MOLDS")
    private String lrMolds;

    /** 版本号 */
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    /** 分公司编码 */
    @ApiModelProperty(value = "分公司编码", name = "companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /** 厂别 */
    @ApiModelProperty(value = "厂别", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

}