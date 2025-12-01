package com.zlt.aps.monthplan.factory.helper;

import com.zlt.aps.monthplan.factory.dto.MouldProductRelationDto;
import lombok.Getter;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * 计划调减后校验通过返回值对象
 *
 * @author ZLT
 * @date 20250609
 */
@Getter
public class AfterSubtractPlanInfoHelper implements Serializable {
    /**
     * 每日剩余产能限制
     */
    private Map<Integer, Long> dayLimitQtyMap;
    /**
     * 最大可排产模具
     */
    private Map<String, MouldProductRelationDto> maxEnableMouldMap;
    /**
     * 停工日
     */
    private Set<Integer> stopDays;

    public AfterSubtractPlanInfoHelper(Map<Integer, Long> dayLimitQtyMap, Map<String, MouldProductRelationDto> maxEnableMouldMap, Set<Integer> stopDays) {
        this.dayLimitQtyMap = dayLimitQtyMap;
        this.maxEnableMouldMap = maxEnableMouldMap;
        this.stopDays = stopDays;
    }
}
