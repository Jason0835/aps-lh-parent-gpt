package com.zlt.aps.factory.scheduling.moulding.single;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.scheduling.IProductionBusinessService;
import com.zlt.aps.factory.scheduling.moulding.SinglePlanProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 单计划模具排产
 * 根据计划的物料配置的可用模具逐一进行排产
 * 对可用模具进行按先续作，再次已排，再次可硫化时间，最后模具编号方式
 * 先用两副，再用两副逐一增模方式按日排产
 * 如果是有交期，则按交期日优先使用模具，否则先将两副模具完全消耗完，
 * 再增加模排产
 *
 * @author
 */
@Slf4j
@Service(value = "singlePlanProductionService")
public class SinglePlanProductionService extends AbstractSinglePlanProductionService {

    private final IProductionBusinessService sameConstructionProductionService;

    private final IProductionBusinessService crossGroupSameProductProductionService;


    public SinglePlanProductionService(ProductionSchedulingDataService dataService,
                                       @Qualifier("assemblingMouldProductionService") IProductionBusinessService assemblingMouldProductionService,
                                       @Qualifier("generalSinglePlanProductionService") IProductionBusinessService generalSinglePlanProductionService,
                                       @Qualifier("deliveryDateSinglePlanProductionService") IProductionBusinessService deliveryDateSinglePlanProductionService,
                                       @Qualifier("sameConstructionProductionService") IProductionBusinessService sameConstructionProductionService,
                                       @Qualifier("crossGroupSameProductProductionService") IProductionBusinessService crossGroupSameProductProductionService) {
        super(dataService, assemblingMouldProductionService, generalSinglePlanProductionService, deliveryDateSinglePlanProductionService);
        this.sameConstructionProductionService = sameConstructionProductionService;
        this.crossGroupSameProductProductionService = crossGroupSameProductProductionService;
    }

    @Override
    public void run(Context context, Object userObj) {
        SinglePlanProductionContext singlePlanProductionContext = (SinglePlanProductionContext) context;
        //单计划排产
        productionPlan(singlePlanProductionContext, userObj);
        //20250424 跨组同规格查找排产处理-计划量<SYS020
//        crossGroupSameProductProductionService.run(singlePlanProductionContext, userObj);
        //20250430 共用生胎查找排产处理 SYS024开关开启
//        sameConstructionProductionService.run(singlePlanProductionContext, userObj);
    }

}
