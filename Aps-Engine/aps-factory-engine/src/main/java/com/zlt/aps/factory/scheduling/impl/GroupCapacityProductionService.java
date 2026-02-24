package com.zlt.aps.factory.scheduling.impl;

import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.scheduling.AbstractBaseProductionService;
import com.zlt.aps.factory.scheduling.IProductionBusinessService;
import com.zlt.aps.factory.service.MonthProductionDataService;
import com.zlt.aps.factory.service.ProductionMdmDataService;
import com.zlt.aps.monthplan.api.enums.ProductionProcessStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 排结构：
 * 对分组计划进行成型产能分配
 * TBR 结构
 * PCR 英寸
 *
 * @author
 */
@Slf4j
@Service(value = "groupCapacityProductionService")
public class GroupCapacityProductionService extends AbstractBaseProductionService {

    private final IProductionBusinessService tbrStructureNameCapacityProductionService;

    public GroupCapacityProductionService(ProductionMdmDataService dataService,
                                          MonthProductionDataService monthProductionDataService,
                                          @Qualifier("tbrCapacityAllocationService") IProductionBusinessService tbrStructureNameCapacityProductionService) {
        super(dataService, monthProductionDataService);
        this.tbrStructureNameCapacityProductionService = tbrStructureNameCapacityProductionService;
    }

    @Override
    public void run(Context context, Object userObj) {
        //根据类别进行
        ProductTypeEnum productType = context.getProductType();
        if (ProductTypeEnum.WHOLE_STEEL == productType) {
            try {
                context.setProductionProcessStage(ProductionProcessStage.STAGE_GROUP);
                tbrStructureNameCapacityProductionService.run(context, userObj);
            } finally {
                //保存日志
                saveProductionProcessLog(context, ProductionProcessStage.STAGE_GROUP);
            }
            return;
        }
    }
}
