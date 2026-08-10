package com.zlt.aps.mp.engine.handler.appoint;

import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * 在机结构指定最早切换结构信息对象
 * 指定机台时间段业务辅助类
 *
 * @author ZLT
 * @date 20260807
 */
@Getter
public class OnlineGroupAppointCxMachineInfo implements Serializable {
    /**
     * 成型机台信息
     */
    private CxMachineBaseInfoVo cxMachineInfo;
    /**
     * 最早切换结构日：即最早上机的后结构日
     */
    private Integer earliestChangeGroupDay;
    /**
     * 在机分组分配信息
     */
    private CxMachineAllocationPlanHelper onlineGroupAllocationInfo;
    /**
     * 实际最早切换结构日：需要考虑机台停机日
     */
    private Integer realEarliestChangeGroupDay;

    /**
     * 构造函数
     *
     * @param cxMachineInfo
     * @param earliestChangeGroupDay
     * @param onlineGroupAllocationInfo
     */
    public OnlineGroupAppointCxMachineInfo(CxMachineBaseInfoVo cxMachineInfo, Integer earliestChangeGroupDay, CxMachineAllocationPlanHelper onlineGroupAllocationInfo) {
        this.cxMachineInfo = cxMachineInfo;
        this.earliestChangeGroupDay = earliestChangeGroupDay;
        this.onlineGroupAllocationInfo = onlineGroupAllocationInfo;
    }

    /**
     * 判断在机结构是否需要强制下机
     *
     * @param context
     * @return
     */
    public boolean isForceOffLine(Context context) {
        if (null == onlineGroupAllocationInfo) {
            return false;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer realEarliestChangeGroupDay = getEffectiveEarliestChangeGroupDay(productionContext);
        if (null == realEarliestChangeGroupDay) {
            return false;
        }
        Integer conclusionDay = onlineGroupAllocationInfo.getEndDay();
        if (conclusionDay < realEarliestChangeGroupDay) {
            return false;
        }
        Integer acceptRange = productionContext.getBaseDataContainer().getParamConfiguration().getAppointForceOfflineDays();
        if (null == acceptRange) {
            acceptRange = BigDecimal.ZERO.intValue();
        }
        acceptRange = Math.abs(acceptRange);
        Integer latestDay = realEarliestChangeGroupDay + acceptRange;
        return conclusionDay < latestDay;
    }

    /**
     * 设置真实的最早切换结构日
     *
     * @param context
     */
    public void setRealEarliestChangeGroupDay(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        this.realEarliestChangeGroupDay = getEffectiveEarliestChangeGroupDay(productionContext);
    }

    /**
     * 获取有效的最早切换结构时间点
     *
     * @return
     */
    private Integer getEffectiveEarliestChangeGroupDay(TbrProductionContext productionContext) {
        if (null == earliestChangeGroupDay) {
            return null;
        }
        Integer monthEndDay = productionContext.getMonthDays();
        if (earliestChangeGroupDay >= monthEndDay) {
            return monthEndDay;
        }
        Set<Integer> stopDays = Optional.ofNullable(cxMachineInfo.getStopDayInfo()).orElse(Collections.emptySet());
        Integer startDay = earliestChangeGroupDay;
        if (startDay < FactoryConstant.MONTH_START_DAY) {
            startDay = FactoryConstant.MONTH_START_DAY;
        }
        for (; startDay <= monthEndDay; ) {
            if (!stopDays.contains(startDay)) {
                break;
            }
            startDay = startDay + BigDecimal.ONE.intValue();
        }
        return startDay;
    }
}
