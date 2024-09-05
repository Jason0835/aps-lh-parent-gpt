package com.zlt.aps.common.engine.domain;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 *  主计划月度生产计划明细
  * @ClassName MdmMonthProdPlan
  * @Description TODO
  * @Author Joran.Zhang
  * @Date  
  * @Version 1.0
**/
@Data
@ApiModel(value = "主计划月度生产计划对象", description = "主计划月度生产计划对象 ")
public class MdmMonthProdPlan extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键")
    private Long id;

    /**
     * 计划序号
     * 主计划沟通后每次版本变更会带序号，如果之前的计划发过重新发布定稿给过来的序号是一样的，根据这个来确定唯一
     */
    private Long planSeq;

    /** 生产排程记录主计划版本号,年+月+日+01，02 */
    @ApiModelProperty(value = "生产排程记录主计划版本号,年+月+日+01，02")
    private String monthPlanApsVersion;

    /** 物料编号 */
    @ApiModelProperty(value = "物料编号")
    private String materialCode;

    /** 规格描述信息 */
    @ApiModelProperty(value = "规格描述信息")
    private String specDesc;

    /** 成型胎胚代码 */
    @ApiModelProperty(value = "成型胎胚代码")
    private String embryoCode;

    /** 质量等级 */
    @ApiModelProperty(value = "质量等级")
    private String qualityGrade;

    /** 外胎寸口信息 */
    @ApiModelProperty(value = "寸口")
    private Double specDimension = 0d;

    /** 库存地点 */
    @ApiModelProperty(value = "库存地点")
    private String storageLocation;

    /** 特殊要求 */
    @ApiModelProperty(value = "特殊要求")
    private String specialRequirements;

    /** 预计超欠产 */
    @ApiModelProperty(value = "预计超欠产")
    private Long expectedExcessArrears = 0L;

    /** 理论生产计划 */
    @ApiModelProperty(value = "理论生产计划")
    private Long theoryProductionPlan = 0L;

    /** 实际安排 */
    @ApiModelProperty(value = "实际安排")
    private Long actualArrangement = 0L;

    /** 计划修正量 */
    @ApiModelProperty(value = "计划修正量")
    private Long planModifyQty = 0L;

    /** 平衡 */
    @ApiModelProperty(value = "平衡")
    private Long balance = 0L;

    /** 硫化机台编号 */
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    /** 硫化机台名称 */
    @ApiModelProperty(value = "硫化机台名称")
    private String lhMachineName;

    /** 成型机台编号 */
    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;

    /** 成型机台名称 */
    @ApiModelProperty(value = "成型机台名称")
    private String cxMachineName;

    /** 1号生产数量 */
    @ApiModelProperty(value = "1号生产数量")
    private Integer productQty1 = 0;

    /** 1号完成数量 */
    @ApiModelProperty(value = "1号完成数量")
    private Integer finishQty1 = 0;

    /** 2号生产数量 */
    @ApiModelProperty(value = "2号生产数量")
    private Integer productQty2 = 0;

    /** 2号完成数量 */
    @ApiModelProperty(value = "2号完成数量")
    private Integer finishQty2 = 0;

    /** 3号生产数量 */
    @ApiModelProperty(value = "3号生产数量")
    private Integer productQty3 = 0;

    /** 3号完成数量 */
    @ApiModelProperty(value = "3号完成数量")
    private Integer finishQty3 = 0;

    /** 4号生产数量 */
    @ApiModelProperty(value = "4号生产数量")
    private Integer productQty4 = 0;

    /** 4号完成数量 */
    @ApiModelProperty(value = "4号完成数量")
    private Integer finishQty4 = 0;

    /** 5号生产数量 */
    @ApiModelProperty(value = "5号生产数量")
    private Integer productQty5 = 0;

    /** 5号完成数量 */
    @ApiModelProperty(value = "5号完成数量")
    private Integer finishQty5 = 0;

    /** 6号生产数量 */
    @ApiModelProperty(value = "6号生产数量")
    private Integer productQty6 = 0;

    /** 6号完成数量 */
    @ApiModelProperty(value = "6号完成数量")
    private Integer finishQty6 = 0;

    /** 7号生产数量 */
    @ApiModelProperty(value = "7号生产数量")
    private Integer productQty7 = 0;

    /** 7号完成数量 */
    @ApiModelProperty(value = "7号完成数量")
    private Integer finishQty7 = 0;

    /** 8号生产数量 */
    @ApiModelProperty(value = "8号生产数量")
    private Integer productQty8 = 0;

    /** 8号完成数量 */
    @ApiModelProperty(value = "8号完成数量")
    private Integer finishQty8 = 0;

    /** 9号生产数量 */
    @ApiModelProperty(value = "9号生产数量")
    private Integer productQty9 = 0;

    /** 9号完成数量 */
    @ApiModelProperty(value = "9号完成数量")
    private Integer finishQty9 = 0;

    /** 10号生产数量 */
    @ApiModelProperty(value = "10号生产数量")
    private Integer productQty10 = 0;

    /** 10号完成数量 */
    @ApiModelProperty(value = "10号完成数量")
    private Integer finishQty10 = 0;

    /** 11号生产数量 */
    @ApiModelProperty(value = "11号生产数量")
    private Integer productQty11 = 0;

    /** 11号完成数量 */
    @ApiModelProperty(value = "11号完成数量")
    private Integer finishQty11 = 0;

    /** 12号生产数量 */
    @ApiModelProperty(value = "12号生产数量")
    private Integer productQty12 = 0;

    /** 12号完成数量 */
    @ApiModelProperty(value = "12号完成数量")
    private Integer finishQty12 = 0;

    /** 13号生产数量 */
    @ApiModelProperty(value = "13号生产数量")
    private Integer productQty13 = 0;

    /** 13号完成数量 */
    @ApiModelProperty(value = "13号完成数量")
    private Integer finishQty13 = 0;

    /** 14号生产数量 */
    @ApiModelProperty(value = "14号生产数量")
    private Integer productQty14 = 0;

    /** 14号完成数量 */
    @ApiModelProperty(value = "14号完成数量")
    private Integer finishQty14 = 0;

    /** 15号生产数量 */
    @ApiModelProperty(value = "15号生产数量")
    private Integer productQty15 = 0;

    /** 15号完成数量 */
    @ApiModelProperty(value = "15号完成数量")
    private Integer finishQty15 = 0;

    /** 16号生产数量 */
    @ApiModelProperty(value = "16号生产数量")
    private Integer productQty16 = 0;

    /** 16号完成数量 */
    @ApiModelProperty(value = "16号完成数量")
    private Integer finishQty16 = 0;

    /** 17号生产数量 */
    @ApiModelProperty(value = "17号生产数量")
    private Integer productQty17 = 0;

    /** 17号完成数量 */
    @ApiModelProperty(value = "17号完成数量")
    private Integer finishQty17 = 0;

    /** 18号生产数量 */
    @ApiModelProperty(value = "18号生产数量")
    private Integer productQty18 = 0;

    /** 18号完成数量 */
    @ApiModelProperty(value = "18号完成数量")
    private Integer finishQty18 = 0;

    /** 19号生产数量 */
    @ApiModelProperty(value = "19号生产数量")
    private Integer productQty19 = 0;

    /** 19号完成数量 */
    @ApiModelProperty(value = "19号完成数量")
    private Integer finishQty19 = 0;

    /** 20号生产数量 */
    @ApiModelProperty(value = "20号生产数量")
    private Integer productQty20 = 0;

    /** 20号完成数量 */
    @ApiModelProperty(value = "20号完成数量")
    private Integer finishQty20 = 0;

    /** 21号生产数量 */
    @ApiModelProperty(value = "21号生产数量")
    private Integer productQty21 = 0;

    /** 21号完成数量 */
    @ApiModelProperty(value = "21号完成数量")
    private Integer finishQty21 = 0;

    /** 22号生产数量 */
    @ApiModelProperty(value = "22号生产数量")
    private Integer productQty22 = 0;

    /** 22号完成数量 */
    @ApiModelProperty(value = "22号完成数量")
    private Integer finishQty22 = 0;

    /** 23号生产数量 */
    @ApiModelProperty(value = "23号生产数量")
    private Integer productQty23 = 0;

    /** 23号完成数量 */
    @ApiModelProperty(value = "23号完成数量")
    private Integer finishQty23 = 0;

    /** 24号生产数量 */
    @ApiModelProperty(value = "24号生产数量")
    private Integer productQty24 = 0;

    /** 24号完成数量 */
    @ApiModelProperty(value = "24号完成数量")
    private Integer finishQty24 = 0;

    /** 25号生产数量 */
    @ApiModelProperty(value = "25号生产数量")
    private Integer productQty25 = 0;

    /** 25号完成数量 */
    @ApiModelProperty(value = "25号完成数量")
    private Integer finishQty25 = 0;

    /** 26号生产数量 */
    @ApiModelProperty(value = "26号生产数量")
    private Integer productQty26 = 0;

    /** 26号完成数量 */
    @ApiModelProperty(value = "26号完成数量")
    private Integer finishQty26 = 0;

    /** 27号生产数量 */
    @ApiModelProperty(value = "27号生产数量")
    private Integer productQty27 = 0;

    /** 27号完成数量 */
    @ApiModelProperty(value = "27号完成数量")
    private Integer finishQty27 = 0;

    /** 28号生产数量 */
    @ApiModelProperty(value = "28号生产数量")
    private Integer productQty28 = 0;

    /** 28号完成数量 */
    @ApiModelProperty(value = "28号完成数量")
    private Integer finishQty28 = 0;

    /** 29号生产数量 */
    @ApiModelProperty(value = "29号生产数量")
    private Integer productQty29 = 0;

    /** 29号完成数量 */
    @ApiModelProperty(value = "29号完成数量")
    private Integer finishQty29 = 0;

    /** 30号生产数量 */
    @ApiModelProperty(value = "30号生产数量")
    private Integer productQty30 = 0;

    /** 30号完成数量 */
    @ApiModelProperty(value = "30号完成数量")
    private Integer finishQty30 = 0;

    /** 31号生产数量 */
    @ApiModelProperty(value = "31号生产数量")
    private Integer productQty31 = 0;

    /** 31号完成数量 */
    @ApiModelProperty(value = "31号完成数量")
    private Integer finishQty31 = 0;

    /** 汇总同SAP+胎胚+库存地点 */
    private Integer monthTotalPlanQty;

    /**
     * 库区生产顺序
     */
    private Integer productSort;

    /**
     * 开始时间
     */
    private Date beginDate;

    /**
     * 结束时间
     */
    private Date endDate;

    /**
     * 库存地点中文描述
     */
    private String storageLocationDesc;
    /**
     * 实际超欠产
     */
    private Integer actualOverProduction = 0;
    /**
     * 数据来源：0：主计划；1：APS新增;
     */
    private String dataSource;

    /**
     * 施工版本信息
     */
    private String  bomDataVersion;

    @Override
    public String toString() {
        return "MdmMonthProdPlan{" +
                "materialCode='" + materialCode + '\'' +
                ", embryoCode='" + embryoCode + '\'' +
                ", bomDataVersion='" + bomDataVersion + '\'' +
                '}';
    }
}
