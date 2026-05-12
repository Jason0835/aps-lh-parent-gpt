package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 硫化在机信息表
 * <p>
 * 记录轮胎硫化过程中，机台当前正在生产的物料信息，包括机台编号、物料信息、模具信息等。
 * 用于实时跟踪硫化机台的生产状态，支持生产调度和现场管理。
 * </p>
 *
 * @author APS Team
 * @since 2026/04/09
 */
@ApiModel(value = "硫化在机信息表", description = "硫化在机信息表")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "T_LH_MACHINE_ONLINE_INFO")
public class LhMachineOnlineInfo extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 上机日期
     * <p>记录物料上机的日期，用于统计每日生产情况</p>
     */
    @ApiModelProperty(value = "上机日期", name = "onlineDate")
    @Excel(name = "ui.data.column.lhMachineOnlineInfo.onlineDate", dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "ONLINE_DATE")
    private Date onlineDate;

    /**
     * 硫化机台编号
     * <p>标识当前正在生产的硫化机台编码</p>
     */
    @ApiModelProperty(value = "硫化机台", name = "lhCode")
    @Excel(name = "ui.data.column.lhMachineOnlineInfo.lhCode")
    @TableField(value = "LH_CODE")
    private String lhCode;

    /**
     * 物料编码
     * <p>APS系统中的物料编码，用于关联物料主数据</p>
     */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @Excel(name = "ui.data.column.lhMachineOnlineInfo.materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * MES物料编码
     * <p>MES系统中的物料编码，用于与MES系统对接</p>
     */
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @Excel(name = "ui.data.column.lhMachineOnlineInfo.mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 物料规格描述
     * <p>物料的规格型号描述信息</p>
     */
    @ApiModelProperty(value = "物料描述", name = "specDesc")
    @Excel(name = "ui.data.column.lhMachineOnlineInfo.specDesc")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /**
     * 左右模标识
     * <p>标识当前生产的是左模还是右模</p>
     */
    @ApiModelProperty(value = "左右模", name = "lrMolds")
    @Excel(name = "ui.data.column.lhMachineOnlineInfo.lrMolds")
    @TableField(value = "LR_MOLDS")
    private String lrMolds;

    /**
     * 数据版本号
     * <p>用于数据同步的版本控制，防止数据冲突</p>
     */
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    @Excel(name = "ui.data.column.lhMachineOnlineInfo.dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    /**
     * 分公司编码
     * <p>所属分公司的编码</p>
     */
    @ApiModelProperty(value = "分公司编码", name = "companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /**
     * 工厂编码
     * <p>所属工厂的编码，用于多工厂数据隔离</p>
     */
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @Excel(name = "ui.data.column.lhMachineOnlineInfo.factoryCode", dictType = "biz_factory_name")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 在机模具编号
     * <p>当前安装在机台上的模具编号</p>
     */
    @ApiModelProperty(value = "在机模号", name = "inMachineMouldCode")
    @Excel(name = "ui.data.column.lhMachineOnlineInfo.inMachineMouldCode")
    @TableField(value = "IN_MACHINE_MOULD_CODE")
    private String inMachineMouldCode;
}

