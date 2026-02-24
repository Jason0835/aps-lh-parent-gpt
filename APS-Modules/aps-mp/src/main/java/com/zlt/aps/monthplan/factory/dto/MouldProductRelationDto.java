package com.zlt.aps.monthplan.factory.dto;

import com.zlt.aps.factory.utils.DateUtils;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 模具物料关系对象
 *
 * @author ZLT
 * @date 20250331
 */
@Data
public class MouldProductRelationDto implements Serializable {

    /**
     * 模具编号
     */
    private String mouldCode;
    /**
     * 模具
     */
    private String mouldNo;
    /**
     * 分厂编号
     */
    private String factoryCode;
    /**
     * 规格代号
     */
    private String specCode;
    /**
     * 物料编码-SAP代号
     */
    private String productCode;

    /**
     * 维修开始时间 yyyy-MM-DD
     */
    private Date beginDate;

    /**
     * 结束日期:yyyy-MM-DD
     */
    private Date endDay;
    /**
     * 不可排产日列表
     */
    private Set<Integer> noProductionList;

    /**
     * 获取不排产天数
     *
     * @return
     */
    public Set<Integer> getNoProductionDay() {
        Set<Integer> noProductionSet = new HashSet<>();
        Integer begin = DateUtils.getDaysByMonth(beginDate);
        Integer end = DateUtils.getDaysByMonth(endDay);
        for (Integer index = begin; index <= end; index++) {
            noProductionSet.add(index);
        }
        return noProductionSet;
    }

    /**
     * 获取不排产天数，根据周期起始日
     *
     * @param productionStartDate
     * @return
     */
    public Set<Integer> getNoProductionDayByCycle(Date productionStartDate) {
        Set<Integer> noProductionSet = new HashSet<>();
        if (null == beginDate || null == endDay) {
            return noProductionSet;
        }
        Integer begin = Long.valueOf(Math.abs(Duration.between(productionStartDate.toInstant(), beginDate.toInstant()).toDays())).intValue() + BigDecimal.ONE.intValue();
        Integer end = Long.valueOf(Math.abs(Duration.between(productionStartDate.toInstant(), endDay.toInstant()).toDays())).intValue() + BigDecimal.ONE.intValue();
        for (Integer index = begin; index <= end; index++) {
            noProductionSet.add(index);
        }
        return noProductionSet;
    }
}
