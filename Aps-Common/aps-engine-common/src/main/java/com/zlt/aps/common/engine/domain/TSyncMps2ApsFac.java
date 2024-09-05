package com.zlt.aps.common.engine.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import lombok.Data;

/**
 * 分厂和精益，都可能下发这张表数据，给分厂APS
 * @TableName T_SYNC_MPS_2_APS_FAC
 */
@Data
public class TSyncMps2ApsFac implements Serializable{
    /**
     * 
     */
    private Long id;

    /**
     * 计划序号
     * 主计划沟通后每次版本变更会带序号，如果之前的计划发过重新发布定稿给过来的序号是一样的，根据这个来确定唯一
     */
    private Long planSeq;

    /**
     * 分公司编号
     */
    private String companyCode;

    /**
     * 分厂编号
     */
    private String factoryCode;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 分厂生产版本号
     */
    private String productionVersion;

    /**
     * 需求计划

     */
    private Long monthPlanId;

    /**
     * 物料编号
     */
    private String productCode;

    /**
     * 产品描述
     */
    private String productDescription;

    /**
     * 等级码
     */
    private String levelCode;

    /**
     * 等级名称
     */
    private String levelName;

    /**
     * 库位类别名称
     */
    private String storType;

    /**
     * 寸口（保留2位小数）
     */
    private BigDecimal proSize = BigDecimal.ZERO;

    /**
     * 品名
     */
    private String productName;

    /**
     * 施工代号
     */
    private String processCode;

    /**
     * 理论生产需求计划
     */
    private Integer theoryProdReqQty = 0;

    /**
     * 生产实际安排
     */
    private Integer totalQty = 0;

    /**
     * 平衡超欠数
     */
    private Integer estimateShortQty = 0;

    /**
     * 备注
     */
    private String remark;

    /**
     * 成型机编号
     */
    private String moldingMachineCode;

    /**
     * 差异量(未排产数量)
     */
    private Integer differenceQty = 0;

    /**
     * 未排产原因
     */
    private String reason;

    /**
     * 开始时间
     */
    private Date beginDate;

    /**
     * 结束时间
     */
    private Date endDate;

    /**
     * DAY_1
     */
    private Integer day1 = 0;

    /**
     * DAY_2
     */
    private Integer day2 = 0;

    /**
     * DAY_3
     */
    private Integer day3 = 0;

    /**
     * DAY_4
     */
    private Integer day4 = 0;

    /**
     * DAY_5
     */
    private Integer day5 = 0;

    /**
     * DAY_6
     */
    private Integer day6 = 0;

    /**
     * DAY_7
     */
    private Integer day7 = 0;

    /**
     * DAY_8
     */
    private Integer day8 = 0;

    /**
     * DAY_9
     */
    private Integer day9 = 0;

    /**
     * DAY_10
     */
    private Integer day10 = 0;

    /**
     * DAY_11
     */
    private Integer day11 = 0;

    /**
     * DAY_12
     */
    private Integer day12 = 0;

    /**
     * DAY_13
     */
    private Integer day13 = 0;

    /**
     * DAY_14
     */
    private Integer day14 = 0;

    /**
     * DAY_15
     */
    private Integer day15 = 0;

    /**
     * DAY_16
     */
    private Integer day16 = 0;

    /**
     * DAY_17
     */
    private Integer day17 = 0;

    /**
     * DAY_18
     */
    private Integer day18 = 0;

    /**
     * DAY_19
     */
    private Integer day19 = 0;

    /**
     * DAY_20
     */
    private Integer day20 = 0;

    /**
     * DAY_21
     */
    private Integer day21 = 0;

    /**
     * DAY_22
     */
    private Integer day22 = 0;

    /**
     * DAY_23
     */
    private Integer day23 = 0;

    /**
     * DAY_24
     */
    private Integer day24 = 0;

    /**
     * DAY_25
     */
    private Integer day25 = 0;

    /**
     * DAY_26
     */
    private Integer day26 = 0;

    /**
     * DAY_27
     */
    private Integer day27 = 0;

    /**
     * DAY_28
     */
    private Integer day28 = 0;

    /**
     * DAY_29
     */
    private Integer day29 = 0;

    /**
     * DAY_30
     */
    private Integer day30 = 0;

    /**
     * DAY_31
     */
    private Integer day31 = 0;

    /**
     * 是否删除（0：默认未删除 1：已删除）
     */
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    private static final long serialVersionUID = 1L;

    /**
     * 根据id是否为空给创建时间，创建人，更新时间，更新人赋值
     */
    public void setBaseVale(Long id) {
        if(id == null) {
            //id为空，表示为新增操作
            this.setIsDelete(0);
            this.setCreateBy(SecurityUtils.getUsername());
            this.setCreateTime(new Date());
        } else {
            //更新操作
            this.setUpdateBy(SecurityUtils.getUsername());
            this.setUpdateTime(new Date());
        }
    }
}