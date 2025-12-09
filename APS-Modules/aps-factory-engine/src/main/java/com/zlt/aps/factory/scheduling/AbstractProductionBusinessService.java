package com.zlt.aps.factory.scheduling;

import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象的排产业务类
 * 主要实现一些公用的业务处理
 *
 * @author ZLT
 * @date 20250220
 */
@Slf4j
public abstract class AbstractProductionBusinessService implements IProductionBusinessService {
    /**
     * 数据提供接口
     */
    private final ProductionSchedulingDataService dataService;

    public AbstractProductionBusinessService(ProductionSchedulingDataService dataService) {
        this.dataService = dataService;
    }

    public ProductionSchedulingDataService getDataService() {
        return dataService;
    }
}
