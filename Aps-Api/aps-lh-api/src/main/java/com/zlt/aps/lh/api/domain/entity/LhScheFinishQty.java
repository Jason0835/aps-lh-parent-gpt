package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.google.common.collect.Lists;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * APS硫化排程完成量回报接口
 *
 * @author APS Team
 * @since 2026/04/09
 */
@ApiModel(value = "APS硫化排程完成量回报接口", description = "APS硫化排程完成量回报接口")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_LH_SCHE_FINISH_QTY")
public class LhScheFinishQty extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "硫化工单号")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    @ApiModelProperty(value = "排程日期")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    @ApiModelProperty(value = "硫化机台编号")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    @ApiModelProperty(value = "物料编码（NC）")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "物料编码（MES）")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    @ApiModelProperty(value = "一班(夜班)完成量")
    @TableField(value = "CLASS1_FINISH_QTY")
    private BigDecimal class1FinishQty;

    @ApiModelProperty(value = "二班(早班)完成量")
    @TableField(value = "CLASS2_FINISH_QTY")
    private BigDecimal class2FinishQty;

    @ApiModelProperty(value = "三班(中班)完成量")
    @TableField(value = "CLASS3_FINISH_QTY")
    private BigDecimal class3FinishQty;

    @ApiModelProperty(value = "一班(夜班)未完成原因")
    @TableField(value = "CLASS1_UN_REASON")
    private String class1UnReason;

    @ApiModelProperty(value = "二班(早班)未完成原因")
    @TableField(value = "CLASS2_UN_REASON")
    private String class2UnReason;

    @ApiModelProperty(value = "三班(中班)未完成原因")
    @TableField(value = "CLASS3_UN_REASON")
    private String class3UnReason;

    @ApiModelProperty(value = "一班(夜班)作业人员")
    @TableField(value = "CLASS1_PERSON")
    private String class1Person;

    @ApiModelProperty(value = "二班(早班)作业人员")
    @TableField(value = "CLASS2_PERSON")
    private String class2Person;

    @ApiModelProperty(value = "三班(中班)作业人员")
    @TableField(value = "CLASS3_PERSON")
    private String class3Person;

    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "一班(夜班)示方号")
    @TableField(value = "CLASS1_LH_NO")
    private String class1LhNo;

    @ApiModelProperty(value = "一班(夜班)示方类型")
    @TableField(value = "CLASS1_LH_TYPE")
    private String class1LhType;

    @ApiModelProperty(value = "二班(早班)示方号")
    @TableField(value = "CLASS2_LH_NO")
    private String class2LhNo;

    @ApiModelProperty(value = "二班(早班)示方类型")
    @TableField(value = "CLASS2_LH_TYPE")
    private String class2LhType;

    @ApiModelProperty(value = "三班(中班)示方号")
    @TableField(value = "CLASS3_LH_NO")
    private String class3LhNo;

    @ApiModelProperty(value = "三班(中班)示方类型")
    @TableField(value = "CLASS3_LH_TYPE")
    private String class3LhType;

    /**
     * 计划类型
     *
     * @return
     */
    public String getLhType() {
        List<String> allLhType = Lists.newArrayList();
        allLhType.add(class1LhType);
        allLhType.add(class2LhType);
        allLhType.add(class3LhType);
        for (String singleClassLhType : allLhType) {
            if (StringUtils.isNotBlank(singleClassLhType)) {
                return singleClassLhType;
            }
        }
        return null;
    }

    /**
     * 获取物料+计划类型Key
     *
     * @return
     */
    public String getFactoryMaterialStatusKey() {
        String keyFormat = "%s|*|%s|*|%s";
        String trimmedProductStatus = StringUtils.trimToEmpty(getLhType());
        return String.format(keyFormat, factoryCode, materialCode, trimmedProductStatus);
    }
}
