package com.zlt.aps.factory.scheduling.impl;

import com.tlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.IProductionBusinessService;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.monthplan.api.enums.ProductionProcessStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 一键生成工厂排产计划，包含
 * 初始化，
 * 排分组：TBR 为结构 PCR 为英寸、寸别、寸口
 * 按分组排模具
 * 主要针对半钢，全钢业务
 *
 * @author
 */
@Slf4j
@Service(value = "wholeCourseProductionService")
public class WholeCourseProductionService extends AbstractProductionBusinessService {

    private final IProductionBusinessService tbrProductionInitService;

    private final IProductionBusinessService tbrCxCapacityAllocationService;

    public WholeCourseProductionService(ProductionSchedulingDataService dataService,
                                        @Qualifier("tbrProductionInitService") IProductionBusinessService tbrProductionInitService,
                                        @Qualifier("tbrCxCapacityAllocationService") IProductionBusinessService tbrCxCapacityAllocationService) {
        super(dataService);
        this.tbrProductionInitService = tbrProductionInitService;
        this.tbrCxCapacityAllocationService = tbrCxCapacityAllocationService;
    }

    /**
     * 一键排产
     *
     * @param context 排产上下文
     * @param userObj 用户数据
     */
    @Override
    public void run(Context context, Object userObj) {
        //根据类别进行
        ProductTypeEnum productType = context.getProductType();
        if (ProductTypeEnum.WHOLE_STEEL == productType) {
            context.setInsertNewProductionVersion(Boolean.TRUE);
            //初始化
            tbrProductionInitService.run(context, userObj);
            context.setInsertNewProductionVersion(Boolean.FALSE);
            //排结构、排模具
            tbrCxCapacityAllocationService.run(context, userObj);
            //保存日志
            saveProductionProcessLog(context, ProductionProcessStage.ONE_CLICK_SCHEDULING);
        }
    }


}
