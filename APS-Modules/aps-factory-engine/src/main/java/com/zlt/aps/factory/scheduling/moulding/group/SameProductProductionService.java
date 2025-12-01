package com.zlt.aps.factory.scheduling.moulding.group;

import com.tlt.aps.enums.ProductionFirstSortOptionsEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.MouldInfoVO;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.IProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.moulding.GroupPlanProductionContext;
import com.zlt.aps.factory.scheduling.moulding.SinglePlanProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.ProductionLogUtils;
import com.zlt.aps.factory.utils.ProductionPlanUtils;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;

/**
 * 同规格排产
 *
 * @author
 */
@Slf4j
@Service(value = "sameProductGeneral")
public class SameProductProductionService extends AbstractProductionBusinessService {

    private final IProductionBusinessService singlePlanProductionService;

    public SameProductProductionService(ProductionSchedulingDataService dataService,
                                        @Qualifier("singlePlanProductionService") IProductionBusinessService singlePlanProductionService) {
        super(dataService);
        this.singlePlanProductionService = singlePlanProductionService;
    }

    @Override
    public void run(Context context, Object userObj) {
        SinglePlanProductionContext singleContext = (SinglePlanProductionContext) context;
        GroupPlanProductionContext groupPlanProductionContext = singleContext.getGroupContext();
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        String startSameProductionLogContent = String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划同规格排程开始====", productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), productionContext.getMonthPlanVersion());
        log.info(startSameProductionLogContent);
        //排产流程日志记录
        MouldProductionLog startSameProductionLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_PRODUCT_LOG, startSameProductionLogContent);
        saveProductionLog(productionContext, startSameProductionLog);
        //获取对应的可用模具
        List<MouldInfoVO> enableMouldList = singleContext.getEnableMouldList();
        if (CollectionUtils.isEmpty(enableMouldList)) {
            String noEnableMouldLogContent = "====同规格排产：没有可用模具";
            log.info(noEnableMouldLogContent);
            //排产流程日志记录
            MouldProductionLog noEnableMouldLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_PRODUCT_LOG, noEnableMouldLogContent);
            saveProductionLog(productionContext, noEnableMouldLog);
            return;
        }
        //获取上一个排产计划
        MonthPlanManufacturingRequirementVo previous = singleContext.getProductionPlan();
        String productCode = previous.getProductCode();
        List<MonthPlanManufacturingRequirementVo> sameProductProductionList = ProductionPlanUtils.getSameProductProductionList(groupPlanProductionContext, previous.getMonthPlanId(), productCode);
        if (CollectionUtils.isEmpty(sameProductProductionList)) {
            String noPlanLogContent = "====同规格排产：没有同规格的物料排产计划";
            log.info(noPlanLogContent);
            //排产流程日志记录
            MouldProductionLog noPlanLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_PRODUCT_LOG, noPlanLogContent);
            saveProductionLog(productionContext, noPlanLog);
            return;
        }
        if (ProductionFirstSortOptionsEnum.DELIVERY_DATE == groupPlanProductionContext.getProductionPlanGroup().getGroup()) {
            //有交期先按交期再按排产顺序排序
            sameProductProductionList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getDeliveryDateDue)
                    .thenComparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        } else {
            //无交期，直接按排产顺序排序
            sameProductProductionList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        }
        sameProductProductionList.stream().forEach(sameProductPlan -> {
            String productionLogContent = String.format("====同规格排产：前一条排产计划ID：%s，排产计划ID：%s，计划量：%d, 单条硫化秒：%d", previous.getMonthPlanId(), sameProductPlan.getMonthPlanId(), sameProductPlan.getProductionQty(), sameProductPlan.getCuringTime().longValue());
            log.info(productionLogContent);
            //排产流程日志记录
            MouldProductionLog productionLog = ProductionLogUtils.buildProductionLog(productionContext, sameProductPlan, MouldProductionLogType.SAME_PRODUCT_LOG, productionLogContent);
            saveProductionLog(productionContext, productionLog);
            SinglePlanProductionContext sameDeliverySingleContext = ProductionPlanUtils.buildSinglePlanProductionContext(sameProductPlan, groupPlanProductionContext.getProductionContext());
            sameDeliverySingleContext.setGroupContext(groupPlanProductionContext);
            singlePlanProductionService.run(sameDeliverySingleContext, userObj);
            String sameProductionEndLogContent = String.format("====同规格排产结束：排产计划ID：%s，计划量：%d", sameProductPlan.getMonthPlanId(), sameProductPlan.getProductionQty());
            log.info(sameProductionEndLogContent);
            //排产流程日志记录
            MouldProductionLog sameProductionEndLog = ProductionLogUtils.buildProductionLog(productionContext, sameProductPlan, MouldProductionLogType.SAME_PRODUCT_LOG, sameProductionEndLogContent);
            saveProductionLog(productionContext, sameProductionEndLog);
        });
    }
}
