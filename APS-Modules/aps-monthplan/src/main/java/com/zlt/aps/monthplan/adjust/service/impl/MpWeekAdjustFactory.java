package com.zlt.aps.monthplan.adjust.service.impl;

import com.zlt.aps.monthplan.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.monthplan.api.annotation.WeekAdjustType;
import com.zlt.aps.monthplan.api.enums.WeekAdjustTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 周程滚动调整策略工厂
 * @author wengpc
 */
@Component
@RequiredArgsConstructor
public class MpWeekAdjustFactory {

    private final List<IMpWeekAdjustService> strategyList;
    private static Map<WeekAdjustTypeEnum, IMpWeekAdjustService> strategyMap = new ConcurrentHashMap<>();

    public IMpWeekAdjustService getStrategy(String code) {
        WeekAdjustTypeEnum weekAdjustTypeEnum = WeekAdjustTypeEnum.getByCode(code);
        if (weekAdjustTypeEnum == null) {
            return null;
        }
        return strategyMap.get(weekAdjustTypeEnum);
    }

    @PostConstruct
    public void init() {
        strategyList.forEach(item -> {
            WeekAdjustType annotation = AnnotationUtils.findAnnotation(item.getClass(), WeekAdjustType.class);
            strategyMap.put(annotation.adjustType(),item);
        });
    }

}
