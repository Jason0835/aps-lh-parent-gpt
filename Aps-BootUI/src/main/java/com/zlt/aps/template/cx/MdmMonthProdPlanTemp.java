package com.zlt.aps.template.cx;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 主计划月度生产计划对象 t_mdm_month_prod_plan
 * 
 * @author zlt
 * @date 2021-09-15
 */
@Data
@ApiModel(value = "主计划月度生产计划对象", description = "主计划月度生产计划对象 ")
public class MdmMonthProdPlanTemp extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;


    /** 物料编号 */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.materialCode",sort = 2)
    @ApiModelProperty(value = "物料编号")
    private String materialCode;

    /**
     * 施工版本信息
     */
    @Excel(name = "ui.data.column.productStatus.bomDataVersion",sort = 3)
    @ImportValidated(required = true,maxLength = 30)
    private  String bomDataVersion;


    /** 成型胎胚代码 */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.embryoCode",sort = 3)
    @ApiModelProperty(value = "成型胎胚代码")
    private String embryoCode;

    /** 质量等级 */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.qualityGrade",sort = 4)
    @ApiModelProperty(value = "质量等级")
    private String qualityGrade;

    /** 成型机台编号 */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.cxMachineCode",sort = 6)
    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;


    /** 硫化机台编号 */
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;


    /** 库存地点编码 */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.stockLocationSort.stockLocation",sort = 10, dictType = "STORAGE_LOCATION")
    @ApiModelProperty(value = "库存地点")
    private String storageLocation;

    /** 理论生产计划 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.theoryProductionPlan",sort = 20)
    @ApiModelProperty(value = "理论生产计划")
    private Long theoryProductionPlan;

    /** 实际安排 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.actualArrangement",sort = 25)
    @ApiModelProperty(value = "实际安排")
    private Long actualArrangement;

    /** 预计超欠产 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.expectedExcessArrears",sort = 30)
    @ApiModelProperty(value = "预计超欠产")
    private Long expectedExcessArrears;

    /** 实际超欠产 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.actualOverProduction",sort = 35)
    @ApiModelProperty(value = "实际超欠产")
    private Long actualOverProduction;

    /** 计划修正量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.planModifyQty",sort = 40)
    @ApiModelProperty(value = "计划修正量")
    private Long planModifyQty;

    /** 平衡 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.balance",sort = 45)
    @ApiModelProperty(value = "平衡")
    private Long balance;

    /** 开始时间 */
    @ImportValidated(date = true)
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.mdmMonthProdPlan.beginDate", width = 30, dateFormat = "yyyy-MM-dd",sort = 50)
    @ApiModelProperty(value = "开始时间")
    private Date beginDate;

    /** 结束时间 */
    @ImportValidated(date = true)
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.mdmMonthProdPlan.endDate", width = 30, dateFormat = "yyyy-MM-dd",sort = 55)
    @ApiModelProperty(value = "结束时间")
    private Date endDate;

    /** 特殊要求 */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.specialRequirements",sort = 60)
    @ApiModelProperty(value = "特殊要求")
    private String specialRequirements;


    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.remark",sort = 65)
    private String remark;

    /** 1号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty1")
    @ApiModelProperty(value = "1号生产数量")
    private Long productQty1;

    /** 2号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty2")
    @ApiModelProperty(value = "2号生产数量")
    private Long productQty2;

    /** 3号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty3")
    @ApiModelProperty(value = "3号生产数量")
    private Long productQty3;

    /** 4号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty4")
    @ApiModelProperty(value = "4号生产数量")
    private Long productQty4;

    /** 5号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty5")
    @ApiModelProperty(value = "5号生产数量")
    private Long productQty5;

    /** 6号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty6")
    @ApiModelProperty(value = "6号生产数量")
    private Long productQty6;

    /** 7号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty7")
    @ApiModelProperty(value = "7号生产数量")
    private Long productQty7;

    /** 8号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty8")
    @ApiModelProperty(value = "8号生产数量")
    private Long productQty8;

    /** 9号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty9")
    @ApiModelProperty(value = "9号生产数量")
    private Long productQty9;

    /** 10号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty10")
    @ApiModelProperty(value = "10号生产数量")
    private Long productQty10;

    /** 11号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty11")
    @ApiModelProperty(value = "11号生产数量")
    private Long productQty11;

    /** 12号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty12")
    @ApiModelProperty(value = "12号生产数量")
    private Long productQty12;

    /** 13号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty13")
    @ApiModelProperty(value = "13号生产数量")
    private Long productQty13;

    /** 14号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty14")
    @ApiModelProperty(value = "14号生产数量")
    private Long productQty14;

    /** 15号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty15")
    @ApiModelProperty(value = "15号生产数量")
    private Long productQty15;

    /** 16号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty16")
    @ApiModelProperty(value = "16号生产数量")
    private Long productQty16;

    /** 17号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty17")
    @ApiModelProperty(value = "17号生产数量")
    private Long productQty17;

    /** 18号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty18")
    @ApiModelProperty(value = "18号生产数量")
    private Long productQty18;

    /** 19号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty19")
    @ApiModelProperty(value = "19号生产数量")
    private Long productQty19;

    /** 20号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty20")
    @ApiModelProperty(value = "20号生产数量")
    private Long productQty20;

    /** 21号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty21")
    @ApiModelProperty(value = "21号生产数量")
    private Long productQty21;

    /** 22号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty22")
    @ApiModelProperty(value = "22号生产数量")
    private Long productQty22;

    /** 23号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty23")
    @ApiModelProperty(value = "23号生产数量")
    private Long productQty23;

    /** 24号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty24")
    @ApiModelProperty(value = "24号生产数量")
    private Long productQty24;

    /** 25号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty25")
    @ApiModelProperty(value = "25号生产数量")
    private Long productQty25;

    /** 26号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty26")
    @ApiModelProperty(value = "26号生产数量")
    private Long productQty26;

    /** 27号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty27")
    @ApiModelProperty(value = "27号生产数量")
    private Long productQty27;

    /** 28号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty28")
    @ApiModelProperty(value = "28号生产数量")
    private Long productQty28;

    /** 29号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty29")
    @ApiModelProperty(value = "29号生产数量")
    private Long productQty29;

    /** 30号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty30")
    @ApiModelProperty(value = "30号生产数量")
    private Long productQty30;

    /** 31号生产数量 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty31")
    @ApiModelProperty(value = "31号生产数量")
    private Long productQty31;



}
