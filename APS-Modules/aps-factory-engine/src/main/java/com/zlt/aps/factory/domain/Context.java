package com.zlt.aps.factory.domain;

import com.ruoyi.common.core.utils.DateUtils;
import com.tlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 工厂月度生产计划排产
 * 通用上下文
 *
 * @author ZLT
 * 20251205
 */
@Data
public class Context {
    /**
     * 工厂编号
     */
    private String factoryCode;

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 产品品类
     */
    private ProductTypeEnum productType;

    /**
     * 月度销售生产需求计划版本
     */
    private String monthPlanVersion;

    /**
     * 排产版本
     */
    private String productionVersion;

    /**
     * 是否生成
     */
    private Boolean general = false;

    /**
     * 版本前缀
     */
    private String prefixVersion;

    /**
     * 日志存储器
     */
    private StringBuilder logBuilder;

    /**
     * 操作批次号
     */
    private String operationWorkNo;

    /**
     * 月份周期排产起始天 第几天
     */
    private Integer startDay;
    /**
     * 排产周期--排产开始日
     */
    private Date productionStartDate;

    /**
     * 排产周期--排产结束日
     */
    private Date productionEndDate;

    /**
     * 停产日信息
     */
    private Set<Integer> stopDays;
    /**
     * 排产产能比例 1~100的值，需除以100
     */
    private Map<Integer, Integer> capacityRatioMap;

    /**
     * 判断排产日是否为排产周期的第一天
     *
     * @param productionDay
     * @return
     */
    public boolean isCycleFirstProductionDay(Integer productionDay) {
        if (null == productionDay) {
            return false;
        }
        Integer monthDays = getMonthDays();
        if (productionDay < ProductionConstant.MONTH_START_DAY || productionDay > monthDays) {
            return false;
        }
        if (stopDays.contains(productionDay)) {
            return false;
        }
        Set<Integer> productionDaySet = getProductionDay();
        if (CollectionUtils.isEmpty(productionDaySet)) {
            return false;
        }
        List<Integer> productionDayList = new ArrayList<>(productionDaySet);
        productionDayList.sort(Comparator.comparing(Integer::intValue));
        Integer firstProductionDay = productionDayList.get(BigDecimal.ZERO.intValue());
        return firstProductionDay.equals(productionDay);
    }

    /**
     * 是否采用自然月进行排产
     *
     * @return
     */
    public boolean isNaturalMonth() {
        if (null == startDay) {
            return true;
        }
        if (startDay > ProductionConstant.NO_NATURAL_MONTH_MAX_VALUE) {
            return true;
        }
        if (startDay <= ProductionConstant.MONTH_START_DAY) {
            return true;
        }
        return false;
    }

    /**
     * 获取前一天
     *
     * @param currentDay
     * @return
     */
    public Integer getPreviousDay(Integer currentDay) {
        if (ProductionConstant.MONTH_START_DAY.equals(currentDay)) {
            return null;
        }
        Integer previousDay = currentDay - BigDecimal.ONE.intValue();
        if (stopDays.contains(previousDay)) {
            return getPreviousDay(previousDay);
        }
        return previousDay;
    }

    /**
     * 创建新的版本号，如果排产版本号已经有值，则不进行创建
     * 否则创建新的排产版本号：规则 前缀 + yyyyMMddHHMMSS
     *
     * @return
     */
    public String createNewProductionVersion() {
        if (StringUtils.isNotBlank(productionVersion)) {
            return productionVersion;
        }
        String prefix = prefixVersion;
        if (StringUtils.isBlank(prefix)) {
            prefix = "";
        }
        productionVersion = prefix + DateUtils.dateTimeNow();
        return productionVersion;
    }

    /**
     * 当前排产月
     *
     * @return
     */
    public LocalDate getCurrentMonth() {
        return LocalDate.of(getYear(), getMonth(), ProductionConstant.MONTH_START_DAY);
    }

    /**
     * 当前排产月前一个月时间
     *
     * @return
     */
    public LocalDate getPreviousMonth() {
        LocalDate currentProductionMonth = LocalDate.of(getYear(), getMonth(), ProductionConstant.MONTH_START_DAY);
        LocalDate previousMonth = currentProductionMonth.minusMonths(BigDecimal.ONE.intValue());
        return previousMonth;
    }

    /**
     * 获取排产周期的天数
     *
     * @return
     */
    public Integer getMonthDays() {
        return com.zlt.aps.factory.utils.DateUtils.getIntervalDays(productionStartDate, productionEndDate);
    }

    /**
     * 获取最大可排产的天数信息
     * 需要剔除停产日
     *
     * @return
     */
    public Integer getMaxProductionDays() {
        Integer monthDays = com.zlt.aps.factory.utils.DateUtils.getIntervalDays(productionStartDate, productionEndDate);
        if (CollectionUtils.isEmpty(stopDays)) {
            return monthDays;
        }
        return monthDays - stopDays.size();
    }

    /**
     * 获取排产周期内可排产天的集合
     * 根据排产周期及停产日，得到排产天集合
     *
     * @return
     */
    public Set<Integer> getProductionDay() {
        Integer monthDays = com.zlt.aps.factory.utils.DateUtils.getIntervalDays(productionStartDate, productionEndDate);
        if (monthDays < BigDecimal.ONE.intValue()) {
            return Collections.emptySet();
        }
        Set<Integer> productionDaySet = new HashSet<>(monthDays);
        for (int day = ProductionConstant.MONTH_START_DAY; day <= monthDays; day++) {
            if (null != stopDays && stopDays.contains(day)) {
                continue;
            }
            productionDaySet.add(day);
        }
        return productionDaySet;
    }
}
