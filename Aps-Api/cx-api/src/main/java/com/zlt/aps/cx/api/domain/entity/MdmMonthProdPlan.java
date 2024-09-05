package com.zlt.aps.cx.api.domain.entity;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.annotation.ImportValidated;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 主计划月度生产计划对象 t_mdm_month_prod_plan
 * 
 * @author zlt
 * @date 2021-09-15
 */
@Data
@ApiModel(value = "主计划月度生产计划对象", description = "主计划月度生产计划对象 ")
public class MdmMonthProdPlan extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 序号 */
    @ApiModelProperty(value = "序号")
    private Long planSeq;

    @ApiModelProperty(value = "年")
    private String year;

    @ApiModelProperty(value = "月")
    private String month;

    /** 主计划月度 */
    @JsonFormat(pattern = "yyyy-MM")
    @ApiModelProperty(value = "主计划月度")
    @Excel(name = "ui.data.column.mdmMonthProdPlan.mainPlanMonth", dateFormat = "yyyy-MM",sort = 1)
    private Date mainPlanMonth;

    /** 是否定稿 */
    @ApiModelProperty(value = "是否定稿")
    @Excel(name = "ui.data.column.mdmMonthProdPlan.isFinamized",sort = 2)
    private String isFinamized;

    /** 生产排程记录主计划版本号 */
    @ApiModelProperty(value = "生产排程记录主计划版本号")
    private String monthPlanApsVersion;

    /** 物料编号 */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.materialCode",sort = 3)
    @ApiModelProperty(value = "物料编号")
    private String materialCode;

    /**
     * 施工版本信息
     */
    @Excel(name = "ui.data.column.productStatus.bomDataVersion",sort = 4)
    private  String bomDataVersion;

    /** 成型胎胚代码 */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.embryoCode",sort = 4)
    @ApiModelProperty(value = "成型胎胚代码")
    private String embryoCode;

    /** 质量等级 */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.qualityGrade",sort = 5)
    @ApiModelProperty(value = "质量等级")
    private String qualityGrade;

    /** 成型机台编号 */
    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;

    /** 成型机台名称 */
    @ImportValidated(maxLength = 20)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.cxMachineName",sort = 7,type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "成型机台名称")
    private String cxMachineName;

    /** 硫化机台编号 */
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    /** 硫化机台名称 */
    @ApiModelProperty(value = "硫化机台名称")
    private String lhMachineName;

    /** 库存地点编码 */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.stockLocationSort.stockLocation",sort = 10, dictType = "STORAGE_LOCATION")
    @ApiModelProperty(value = "库存地点")
    private String storageLocation;

    /** 库存地点中文描述 */
    @ApiModelProperty(value = "库存地点中文描述")
    private String storageLocationDesc;

    /** 规格描述信息 */
    @ImportValidated(required = true,maxLength = 300)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.specDesc",sort = 12)
    @ApiModelProperty(value = "规格描述信息")
    private String specDesc;

    /** 外胎规格尺寸 */
    @ImportValidated(number = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.specDimension",sort = 15)
    @ApiModelProperty(value = "外胎规格尺寸")
    private BigDecimal specDimension;

    /** 理论生产计划 */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.theoryProductionPlan",sort = 20)
    @ApiModelProperty(value = "理论生产计划")
    private Long theoryProductionPlan;

    /** 实际安排 */
    @ImportValidated(required = true,digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.plan",sort = 25)
    @ApiModelProperty(value = "实际安排")
    private Long actualArrangement;

    /** 预计超欠产 */
    @ImportValidated(digits = true,min = -9999999,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.expectedExcessArrears",sort = 30)
    @ApiModelProperty(value = "预计超欠产")
    private Long expectedExcessArrears;

    /** 实际超欠产 */
    @ImportValidated(digits = true,min = -9999999,max = 9999999)
    @Excel(name = "ui.data.column.mdmMonthProdPlan.actualOverProduction",sort = 35)
    @ApiModelProperty(value = "实际超欠产")
    private Long actualOverProduction;

    /** 计划修正量 */
    @ImportValidated(digits = true,min = -9999999,max = 9999999)
    @ApiModelProperty(value = "计划修正量")
    private Long planModifyQty;

    /** 平衡 */
    @ImportValidated(digits = true,min = -9999999,max = 9999999)
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


    /** 数据来源 */
    @ApiModelProperty(value = "数据来源")
    private String dataSource;

    /** 1号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty1")
    @ApiModelProperty(value = "1号生产数量")
    private Long productQty1;

    /** 1号完成数量 */
    @ApiModelProperty(value = "1号完成数量")
    private Long finishQty1;

    /** 2号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty2")
    @ApiModelProperty(value = "2号生产数量")
    private Long productQty2;

    /** 2号完成数量 */
    @ApiModelProperty(value = "2号完成数量")
    private Long finishQty2;

    /** 3号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty3")
    @ApiModelProperty(value = "3号生产数量")
    private Long productQty3;

    /** 3号完成数量 */
    @ApiModelProperty(value = "3号完成数量")
    private Long finishQty3;

    /** 4号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty4")
    @ApiModelProperty(value = "4号生产数量")
    private Long productQty4;

    /** 4号完成数量 */
    @ApiModelProperty(value = "4号完成数量")
    private Long finishQty4;

    /** 5号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty5")
    @ApiModelProperty(value = "5号生产数量")
    private Long productQty5;

    /** 5号完成数量 */
    @ApiModelProperty(value = "5号完成数量")
    private Long finishQty5;

    /** 6号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty6")
    @ApiModelProperty(value = "6号生产数量")
    private Long productQty6;

    /** 6号完成数量 */
    @ApiModelProperty(value = "6号完成数量")
    private Long finishQty6;

    /** 7号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty7")
    @ApiModelProperty(value = "7号生产数量")
    private Long productQty7;

    /** 7号完成数量 */
    @ApiModelProperty(value = "7号完成数量")
    private Long finishQty7;

    /** 8号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty8")
    @ApiModelProperty(value = "8号生产数量")
    private Long productQty8;

    /** 8号完成数量 */
    @ApiModelProperty(value = "8号完成数量")
    private Long finishQty8;

    /** 9号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty9")
    @ApiModelProperty(value = "9号生产数量")
    private Long productQty9;

    /** 9号完成数量 */
    @ApiModelProperty(value = "9号完成数量")
    private Long finishQty9;

    /** 10号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty10")
    @ApiModelProperty(value = "10号生产数量")
    private Long productQty10;

    /** 10号完成数量 */
    @ApiModelProperty(value = "10号完成数量")
    private Long finishQty10;

    /** 11号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty11")
    @ApiModelProperty(value = "11号生产数量")
    private Long productQty11;

    /** 11号完成数量 */
    @ApiModelProperty(value = "11号完成数量")
    private Long finishQty11;

    /** 12号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty12")
    @ApiModelProperty(value = "12号生产数量")
    private Long productQty12;

    /** 12号完成数量 */
    @ApiModelProperty(value = "12号完成数量")
    private Long finishQty12;

    /** 13号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty13")
    @ApiModelProperty(value = "13号生产数量")
    private Long productQty13;

    /** 13号完成数量 */
    @ApiModelProperty(value = "13号完成数量")
    private Long finishQty13;

    /** 14号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty14")
    @ApiModelProperty(value = "14号生产数量")
    private Long productQty14;

    /** 14号完成数量 */
    @ApiModelProperty(value = "14号完成数量")
    private Long finishQty14;

    /** 15号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty15")
    @ApiModelProperty(value = "15号生产数量")
    private Long productQty15;

    /** 15号完成数量 */
    @ApiModelProperty(value = "15号完成数量")
    private Long finishQty15;

    /** 16号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty16")
    @ApiModelProperty(value = "16号生产数量")
    private Long productQty16;

    /** 16号完成数量 */
    @ApiModelProperty(value = "16号完成数量")
    private Long finishQty16;

    /** 17号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty17")
    @ApiModelProperty(value = "17号生产数量")
    private Long productQty17;

    /** 17号完成数量 */
    @ApiModelProperty(value = "17号完成数量")
    private Long finishQty17;

    /** 18号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty18")
    @ApiModelProperty(value = "18号生产数量")
    private Long productQty18;

    /** 18号完成数量 */
    @ApiModelProperty(value = "18号完成数量")
    private Long finishQty18;

    /** 19号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty19")
    @ApiModelProperty(value = "19号生产数量")
    private Long productQty19;

    /** 19号完成数量 */
    @ApiModelProperty(value = "19号完成数量")
    private Long finishQty19;

    /** 20号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty20")
    @ApiModelProperty(value = "20号生产数量")
    private Long productQty20;

    /** 20号完成数量 */
    @ApiModelProperty(value = "20号完成数量")
    private Long finishQty20;

    /** 21号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty21")
    @ApiModelProperty(value = "21号生产数量")
    private Long productQty21;

    /** 21号完成数量 */
    @ApiModelProperty(value = "21号完成数量")
    private Long finishQty21;

    /** 22号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty22")
    @ApiModelProperty(value = "22号生产数量")
    private Long productQty22;

    /** 22号完成数量 */
    @ApiModelProperty(value = "22号完成数量")
    private Long finishQty22;

    /** 23号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty23")
    @ApiModelProperty(value = "23号生产数量")
    private Long productQty23;

    /** 23号完成数量 */
    @ApiModelProperty(value = "23号完成数量")
    private Long finishQty23;

    /** 24号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty24")
    @ApiModelProperty(value = "24号生产数量")
    private Long productQty24;

    /** 24号完成数量 */
    @ApiModelProperty(value = "24号完成数量")
    private Long finishQty24;

    /** 25号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty25")
    @ApiModelProperty(value = "25号生产数量")
    private Long productQty25;

    /** 25号完成数量 */
    @ApiModelProperty(value = "25号完成数量")
    private Long finishQty25;

    /** 26号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty26")
    @ApiModelProperty(value = "26号生产数量")
    private Long productQty26;

    /** 26号完成数量 */
    @ApiModelProperty(value = "26号完成数量")
    private Long finishQty26;

    /** 27号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty27")
    @ApiModelProperty(value = "27号生产数量")
    private Long productQty27;

    /** 27号完成数量 */
    @ApiModelProperty(value = "27号完成数量")
    private Long finishQty27;

    /** 28号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty28")
    @ApiModelProperty(value = "28号生产数量")
    private Long productQty28;

    /** 28号完成数量 */
    @ApiModelProperty(value = "28号完成数量")
    private Long finishQty28;

    /** 29号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty29")
    @ApiModelProperty(value = "29号生产数量")
    private Long productQty29;

    /** 29号完成数量 */
    @ApiModelProperty(value = "29号完成数量")
    private Long finishQty29;

    /** 30号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty30")
    @ApiModelProperty(value = "30号生产数量")
    private Long productQty30;

    /** 30号完成数量 */
    @ApiModelProperty(value = "30号完成数量")
    private Long finishQty30;

    /** 31号生产数量 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.productQty31")
    @ApiModelProperty(value = "31号生产数量")
    private Long productQty31;

    /** 31号完成数量 */
    @ApiModelProperty(value = "31号完成数量")
    private Long finishQty31;

    /** 删除标识 */
    @ApiModelProperty(value = "31号完成数量")
    private String delFlag;

    @ApiModelProperty(value = "是否存在版本")
    private Integer hasVersion;

}
