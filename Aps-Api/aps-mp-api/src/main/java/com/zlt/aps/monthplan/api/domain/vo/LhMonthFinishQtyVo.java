package com.zlt.aps.monthplan.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 月度外胎汇总
 *
 * @author Liam
 * @since 2025/4/10
 */
@Data
public class LhMonthFinishQtyVo {
    /**
     * 分厂编号
     */
    private String factoryCode;
    /**
     * SAP代码
     */
    private String productCode;
    /**
     * 规格编码
     */
    private String specCode;
    /**
     * 规格描述
     */
    private String specDesc;
    /**
     * 外胎不良数，若不良接口可以提供SAP+胎胚，则接口同步更新该字段,如果给不了由人为输入确认同步更新
     */
    private Integer specPadQty;
    /**
     * 外胎月结库存，月结库存获取时更新到该字段
     */
    private Integer lastMonthStock;
    /**
     * 计划数量
     */
    private Integer monthPlanQty = 0;
    /**
     * 本月完成数量
     */
    private Integer monthFinishQty;
    /**
     * 剩余数量
     */
    private Integer monthRemainQty;

    /**
     * 胎胚代码
     */
    private String embryoCode;
    /**
     * 品牌
     */
    private String brand;

    /**
     * 模具数
     */
    private Integer mouldQty;

    /**
     * 施工号
     */
    private String constructionCode;

    /**
     * 夏季机械硫化时间
     */
    private Integer curingTime;

    /**
     * 冬季机械硫化时间
     */
    private Integer curingTime2;

    /**
     * 期初库存
     */
    private Integer initQty;
    /**
     * 可利用模具
     */
    private Integer usedMouldQty;
    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date planStartDate;
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date planEndDate;
    /**
     * 模具
     */
    private String mouldNo;
    /***
     *  日计划
     */
    private List<LhMonthDayFinishQtyVo> dayFinishQtyList;
}
