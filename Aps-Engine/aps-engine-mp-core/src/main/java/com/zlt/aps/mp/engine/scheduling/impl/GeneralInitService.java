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
 * 工厂排产-分阶段-初始化，
 * 排分组：TBR 为结构 PCR 为英寸、寸别、寸口
 *
 * @author
 */
@Slf4j
@Service(value = "generalInitService")
public class GeneralInitService extends AbstractBaseProductionService {

    private final IProductionBusinessService tbrProductionInitService;

    public GeneralInitService(ProductionMdmDataService dataService,
                              MonthProductionDataService monthProductionDataService,
                              @Qualifier("tbrProductionInitService") IProductionBusinessService tbrProductionInitService) {
        super(dataService, monthProductionDataService);
        this.tbrProductionInitService = tbrProductionInitService;
    }

    @Override
    public void run(Context context, Object userObj) {
        //根据类别进行
        ProductTypeEnum productType = context.getProductType();
        if (ProductTypeEnum.WHOLE_STEEL == productType) {
            try {
                context.setProductionProcessStage(ProductionProcessStage.STAGE_INIT);
                tbrProductionInitService.run(context, userObj);
            }finally {
                //保存日志
                saveProductionProcessLog(context, ProductionProcessStage.STAGE_INIT);
            }
            return;
        }
    }
}
