package com.zlt.aps.factory.scheduling.groupproduction;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.service.DpRequireDataService;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 工厂TBR业务轮胎结构排产业务
 * 主要进行结构成型产能分配功能
 *
 * @author
 */
@Slf4j
@Service(value = "tbrCapacityAllocationService")
public class TbrStructureNameCapacityProductionService extends AbstractProductionBusinessService {

    public TbrStructureNameCapacityProductionService(ProductionSchedulingDataService dataService,
                                                     DpRequireDataService dpRequireDataService) {
        super(dataService, dpRequireDataService);
    }

    @Override
    public void run(Context context, Object userObj) {

    }
}
