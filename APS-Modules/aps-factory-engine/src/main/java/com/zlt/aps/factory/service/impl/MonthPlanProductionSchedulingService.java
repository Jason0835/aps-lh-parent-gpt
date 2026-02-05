package com.zlt.aps.factory.service.impl;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.scheduling.IProductionBusinessService;
import com.zlt.aps.factory.service.IMonthPlanProductionSchedulingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分厂月生产计划排产Service
 *
 * @author ZLT
 * @date 20250220
 */
@Slf4j
@Service
public class MonthPlanProductionSchedulingService implements IMonthPlanProductionSchedulingService {

    private final IProductionBusinessService generalInitService;

    private final IProductionBusinessService mouldProductionService;

    private final IProductionBusinessService wholeCourseProductionService;

    private final IProductionBusinessService groupCapacityProductionService;

    public MonthPlanProductionSchedulingService(@Qualifier("generalInitService") IProductionBusinessService generalInitService,
                                                @Qualifier("mouldProductionService") IProductionBusinessService mouldProductionService,
                                                @Qualifier("wholeCourseProductionService") IProductionBusinessService wholeCourseProductionService,
                                                @Qualifier("groupCapacityProductionService") IProductionBusinessService groupCapacityProductionService) {
        this.generalInitService = generalInitService;
        this.mouldProductionService = mouldProductionService;
        this.wholeCourseProductionService = wholeCourseProductionService;
        this.groupCapacityProductionService = groupCapacityProductionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void init(Context context) {
        generalInitService.run(context, new Object());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void groupCapacityScheduling(Context context) {
        groupCapacityProductionService.run(context, new Object());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mouldingScheduling(Context context) {
        mouldProductionService.run(context, new Object());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void general(Context context) {
        wholeCourseProductionService.run(context, new Object());
    }

    @Override
    public void deleteVersion(Context context) {

    }

    @Override
    public void calculateSizeCapacityRequire(Context context) {

    }
}
