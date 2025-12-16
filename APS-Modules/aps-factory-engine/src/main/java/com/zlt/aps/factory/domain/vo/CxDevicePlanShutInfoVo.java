package com.zlt.aps.factory.domain.vo;

import com.zlt.aps.factory.utils.DateUtils;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 月计划-成型维修信息
 *
 * @author ZLT
 * @date 20251215
 */
@Data
public class CxDevicePlanShutInfoVo implements Serializable {

    /**
     * 工厂编号
     */
    private String factoryCode;

    /**
     * 成型机台
     */
    private String cxMachineCode;

    /**
     * 维修开始日期
     */
    private Date beginDate;

    /**
     * 维修结束日期
     */
    private Date endDate;

    /**
     * 排产周期起始日
     */
    private Date productionStartDate;

    /**
     * 排产周期结束日
     */
    private Date productionEndDate;

    /**
     * 根据维护的维修停机日期，转化为排产周期的停产日信息
     * 根据周期得到其周期所处日
     * 时间所处条件：beginDate <= productionStartDate <= endDate <= productionEndDate
     * 如果beginDate > productionEndDate 则表示周期范围内没有停产日
     * 如果endDate < productionStartDate 则表示周期范围内没有停产日
     * 否则从min[beginDate,productionStartDate] ~min[endDate,productionEndDate]在
     * [productionStartDate,productionEndDate]的停产信息
     *
     * @return
     */
    public Set<Integer> getStopDayInfo() {
        if (null == beginDate || null == endDate || null == productionStartDate || null == productionEndDate) {
            return Collections.emptySet();
        }
        Integer stopDays = DateUtils.getIntervalDays(beginDate, endDate);
        //大于1，表示不相等，则beginDate需在endDate前
        if (stopDays > BigDecimal.ONE.intValue() && beginDate.after(endDate)) {
            return Collections.emptySet();
        }
        //大于1，表示不相等，则endDate需在productionStartDate后
        Integer startDiffDays = DateUtils.getIntervalDays(endDate, productionStartDate);
        if (startDiffDays > BigDecimal.ONE.intValue() && endDate.before(productionStartDate)) {
            return Collections.emptySet();
        }
        //大于1，表示不相等，则beginDate需在productionEndDate前
        Integer endDiffDays = DateUtils.getIntervalDays(beginDate, productionEndDate);
        if (endDiffDays > BigDecimal.ONE.intValue() && beginDate.after(productionEndDate)) {
            return Collections.emptySet();
        }
        Set<Integer> stopDayInfoSet = new HashSet<>();
        Integer stopStartDays = DateUtils.getIntervalDays(beginDate, productionStartDate);
        Date stopStartDate;
        //不相等，取最小的日期
        if (stopStartDays != BigDecimal.ONE.intValue()) {
            stopStartDate = beginDate.after(productionStartDate) ? beginDate : productionStartDate;
        } else {
            stopStartDate = beginDate;
        }
        Date stopEndDate;
        Integer stopEndDays = DateUtils.getIntervalDays(endDate, productionEndDate);
        //不相等，取最小的日期
        if (stopEndDays != BigDecimal.ONE.intValue()) {
            stopEndDate = endDate.after(productionEndDate) ? productionEndDate : endDate;
        } else {
            stopEndDate = endDate;
        }
        Integer realStopDays = DateUtils.getIntervalDays(stopStartDate, stopEndDate);
        Integer realStartStopDay = DateUtils.getIntervalDays(stopStartDate, productionStartDate);
        for (int day = BigDecimal.ZERO.intValue(); day < realStopDays; day++) {
            stopDayInfoSet.add(realStartStopDay + day);
        }
        return stopDayInfoSet;
    }

}
