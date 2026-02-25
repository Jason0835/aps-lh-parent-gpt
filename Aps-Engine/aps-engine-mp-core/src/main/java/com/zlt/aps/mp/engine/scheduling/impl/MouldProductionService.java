package com.zlt.aps.mp.engine.scheduling.impl;

import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.service.MonthProductionDataService;
import com.zlt.aps.mp.engine.scheduling.AbstractBaseProductionService;
import com.zlt.aps.mp.engine.scheduling.IProductionBusinessService;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import com.zlt.aps.mp.api.enums.ProductionProcessStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 排模具：
 * 根据结构排产信息对分组计划进行模具排产
 * TBR 结构
 * PCR 英寸
 *
 * @author
 */
@Slf4j
@Service(value = "mouldProductionService")
public class MouldProductionService extends AbstractBaseProductionService {

    private final IProductionBusinessService tbrMouldProductionService;

    public MouldProductionService(ProductionMdmDataService dataService,
                                  MonthProductionDataService monthProductionDataService,
                                  @Qualifier("tbrMouldProductionService") IProductionBusinessService tbrMouldProductionService) {
        super(dataService, monthProductionDataService);
        this.tbrMouldProductionService = tbrMouldProductionService;
    }

    @Override
    public void run(Context context, Object userObj) {
        //根据类别进行
        ProductTypeEnum productType = context.getProductType();
        if (ProductTypeEnum.WHOLE_STEEL == productType) {
            try {
                context.setProductionProcessStage(ProductionProcessStage.STAGE_GROUP);
                tbrMouldProductionService.run(context, userObj);
            } finally {
                //保存日志
                saveProductionProcessLog(context, ProductionProcessStage.STAGE_GROUP);
            }
            return;
        }
    }
}
