package com.zlt.aps.factory.service.impl;

import com.tlt.aps.enums.ProductTypeEnum;
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

    private final IProductionBusinessService wholeCourseProductionService;

    public MonthPlanProductionSchedulingService(@Qualifier("tbrProductionInitService") IProductionBusinessService generalInitService,
                                                @Qualifier("wholeCourseProductionService") IProductionBusinessService wholeCourseProductionService
    ) {
        this.generalInitService = generalInitService;
        this.wholeCourseProductionService = wholeCourseProductionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void init(Context context) {
        //根据类别进行
        ProductTypeEnum productType = context.getProductType();
        if (ProductTypeEnum.SEMI_STEEL == productType) {
            generalInitService.run(context, new Object());
            return;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mouldingScheduling(Context context) {

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void general(Context context) {
        //根据类别进行
        ProductTypeEnum productType = context.getProductType();
        if (ProductTypeEnum.WHOLE_STEEL == productType) {
            wholeCourseProductionService.run(context, new Object());
            return;
        }
    }

    @Override
    public void deleteVersion(Context context) {

    }

    @Override
    public void calculateSizeCapacityRequire(Context context) {

    }
}
