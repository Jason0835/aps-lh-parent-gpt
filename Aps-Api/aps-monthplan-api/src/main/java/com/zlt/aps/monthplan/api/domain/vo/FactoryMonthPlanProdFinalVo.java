package com.zlt.aps.monthplan.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

/**
 * 分厂月生产计划排产结果-最终版本(包含调整单调整的结果)
 *
 * @author ZLT
 * @data 20250214
 */
@Data
public class FactoryMonthPlanProdFinalVo extends FactoryMonthPlanProdFinal {

    /**
     * 每天单模最大硫化时间 --单位到秒
     */
    private BigDecimal dayMaxCuringTime;
    /**
     * 月份最大天数
     */
    private Integer maxDays;
    /**
     * 最大模具
     */
    private Set<String> maxMouldSet;
    /**
     * 换规格损耗时间
     */
    private BigDecimal changeProductConsumeTime;
    /**
     * 排产周期开始日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date productionStartDate;

    /**
     * 月份排产最大结束日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "月份排产最大结束日", name = "productionEndDate")
    private Date productionEndDate;
    /**
     * 增加的日期数
     */
    private Integer addDays;
    /**
     * 排产顺序
     */
    @ApiModelProperty(value = "排产顺序", name = "productionSequence")
    private Long productionSequence;

    public Long sumDays() {
        return (getDay1() != null ? getDay1() : 0) +
                (getDay2() != null ? getDay2() : 0) +
                (getDay3() != null ? getDay3() : 0) +
                (getDay4() != null ? getDay4() : 0) +
                (getDay5() != null ? getDay5() : 0) +
                (getDay6() != null ? getDay6() : 0) +
                (getDay7() != null ? getDay7() : 0) +
                (getDay8() != null ? getDay8() : 0) +
                (getDay9() != null ? getDay9() : 0) +
                (getDay10() != null ? getDay10() : 0) +
                (getDay11() != null ? getDay11() : 0) +
                (getDay12() != null ? getDay12() : 0) +
                (getDay13() != null ? getDay13() : 0) +
                (getDay14() != null ? getDay14() : 0) +
                (getDay15() != null ? getDay15() : 0) +
                (getDay16() != null ? getDay16() : 0) +
                (getDay17() != null ? getDay17() : 0) +
                (getDay18() != null ? getDay18() : 0) +
                (getDay19() != null ? getDay19() : 0) +
                (getDay20() != null ? getDay20() : 0) +
                (getDay21() != null ? getDay21() : 0) +
                (getDay22() != null ? getDay22() : 0) +
                (getDay23() != null ? getDay23() : 0) +
                (getDay24() != null ? getDay24() : 0) +
                (getDay25() != null ? getDay25() : 0) +
                (getDay26() != null ? getDay26() : 0) +
                (getDay27() != null ? getDay27() : 0) +
                (getDay28() != null ? getDay28() : 0) +
                (getDay29() != null ? getDay29() : 0) +
                (getDay30() != null ? getDay30() : 0) +
                (getDay31() != null ? getDay31() : 0);
    }

}