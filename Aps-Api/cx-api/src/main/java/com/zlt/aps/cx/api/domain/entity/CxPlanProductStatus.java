package com.zlt.aps.cx.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 成型计划投产状态对象 t_cx_plan_product_status
 *
 * @author zlt
 * @date 2021-07-21
 */
@ApiModel(value = "成型计划投产状态对象", description = "成型计划投产状态对象 ")
@Data
public class CxPlanProductStatus extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 生产排程记录主计划版本号
     */
    @Excel(name = "ui.data.column.productStatus.monthPlanApsVersion",sort = 1)
    @ApiModelProperty(value = "主计划版本号")
    private String monthPlanApsVersion;

    /**
     * 施工版本信息
     */
    @Excel(name = "ui.data.column.productStatus.bomDataVersion",sort = 1)
    private  String bomDataVersion;

    /**
     * 对应月度计划明细ID串
     */
    @ApiModelProperty(value = "月度计划明细ID串")
    private String monthPlanIds;

    /**
     * SAP品号
     */
    @Excel(name = "ui.data.column.productStatus.sapCode",sort = 2)
    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    /**
     * 胎胚代码
     */
    @Excel(name = "ui.data.column.productStatus.embryoCode",sort = 3)
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    @Excel(name = "ui.data.column.productStatus.embryoVersion",sort = 4)
    @ApiModelProperty(value = "胎胚版本")
    private String embryoVersion;

    /**
     * 月度计划总量
     */
    @Excel(name = "ui.data.column.productStatus.monthPlanTotalQty",sort = 4)
    @ApiModelProperty(value = "月度计划总量")
    private Long monthPlanTotalQty;

    /**
     * 规格寸口
     */
    @Excel(name = "ui.data.column.productStatus.specDimension",sort = 5)
    @ApiModelProperty(value = "规格寸口")
    private Double specDimension;

    /**
     * 开始时间
     */
    @ApiModelProperty(value = "开始时间")
    private String beginDate;

    /**
     * 投产状态
     */
    @Excel(name = "ui.data.column.productStatus.productStatus", dictType = "PRODUCT_STATUS",sort = 6)
    @ApiModelProperty(value = "投产状态")
    private String productStatus;

    /**
     * 投产明细描述
     */
    @ApiModelProperty(value = "明细描述")
    private String productDetail;

    /**
     * 删除标识（0未删除；1已删除）
     */
    private String delFlag;

    /**
     * 结束时间
     */
    @ApiModelProperty(value = "结束时间")
    private String endDate;

    /**
     * 标记不投产
     */
    @Excel(name = "ui.data.column.productStatus.markUnProduct", dictType = "MARK_UN_PRODUCT",sort = 7)
    @ApiModelProperty(value = "标记不投产")
    private String markUnProduct;


    @Excel(name = "ui.data.column.stock.remark",sort = 8)
    private String remark;

    //投产所选成型机台编码
    private String cxMachineCode;

    //投产所选的记录数
    private String ids;

    //投产日期
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date scheduleDate;

    //投产-库存地点
    private String storageLocation;

    //投产-1班计划量
    private Integer class1PlanQty;

    //投产-2班计划量
    private Integer class2PlanQty;

    //投产-3班计划量
    private Integer class3PlanQty;

    //投产-4班计划量
    private Integer class4PlanQty;

    //投产-5班计划量
    private Integer class5PlanQty;

    //月度计划调整量
    private Long monthPlanTotalModifyQty;

    /**
     * 调整源头（0投产列表；1成型排程）
     */
    private String adjustSource;




    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("monthPlanApsVersion", getMonthPlanApsVersion())
                .append("monthPlanIds", getMonthPlanIds())
                .append("sapCode", getSapCode())
                .append("embryoCode", getEmbryoCode())
                .append("monthPlanTotalQty", getMonthPlanTotalQty())
                .append("beginDate", getBeginDate())
                .append("productStatus", getProductStatus())
                .append("productDetail", getProductDetail())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("delFlag", getDelFlag())
                .append("remark", getRemark())
                .append("endDate", getEndDate())
                .append("markUnProduct", getMarkUnProduct())
                .append("specDimension", getSpecDimension())
                .toString();
    }

    /**
     * 初始化月度剩余量
     */
    public  void initPlanQty(){
        if(class1PlanQty==null){
            class1PlanQty=0;
        }
        if(class2PlanQty==null){
            class2PlanQty=0;
        }
        if(class3PlanQty==null){
            class3PlanQty=0;
        }
        if(class4PlanQty==null){
            class4PlanQty=0;
        }
        if(class5PlanQty==null){
            class5PlanQty=0;
        }
    }

    /**
     * 获取计划总量
     * @return
     */
    public Integer getTotalPlanQty(){
        initPlanQty();
        return class1PlanQty+class2PlanQty+class3PlanQty+class4PlanQty+class5PlanQty;
    }

}
