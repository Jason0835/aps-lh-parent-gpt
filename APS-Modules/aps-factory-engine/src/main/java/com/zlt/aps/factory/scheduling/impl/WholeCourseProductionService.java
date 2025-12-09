package com.zlt.aps.factory.scheduling.impl;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 一键生成分厂排产计划，包含
 * 初始化，分厂模具排产
 * 主要针对半钢，全钢业务
 *
 * @author
 */
@Slf4j
@Service(value = "wholeCourseProductionService")
public class WholeCourseProductionService extends AbstractProductionBusinessService {


    public WholeCourseProductionService(ProductionSchedulingDataService dataService) {
        super(dataService);
    }

    @Override
    public void run(Context context, Object userObj) {

    }


}
