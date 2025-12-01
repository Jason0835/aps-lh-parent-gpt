package com.zlt.aps.factory.scheduling.moulding.group;

import com.tlt.aps.enums.ProductionFirstSortOptionsEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.IProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.moulding.GroupPlanProductionContext;
import com.zlt.aps.factory.scheduling.moulding.SinglePlanProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.ProductionLogUtils;
import com.zlt.aps.factory.utils.ProductionPlanUtils;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 跨组同寸口排产--已经在同寸口中兼容了跨组同寸口-作废
 *
 * @author
 */
@Slf4j
@Deprecated
@Service(value = "crossGroupSameProSizeGeneral")
public class CrossGroupSameProSizeProductionService extends AbstractProductionBusinessService {

    private final IProductionBusinessService singlePlanProductionService;

    public CrossGroupSameProSizeProductionService(ProductionSchedulingDataService dataService, @Qualifier("singlePlanProductionService") IProductionBusinessService singlePlanProductionService) {
        super(dataService);
        this.singlePlanProductionService = singlePlanProductionService;
    }

    @Override
    public void run(Context context, Object userObj) {
        SinglePlanProductionContext singleContext = (SinglePlanProductionContext) context;
        GroupPlanProductionContext groupPlanProductionContext = singleContext.getGroupContext();
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        //获取上一个排产计划
        MonthPlanManufacturingRequirementVo previous = singleContext.getProductionPlan();
        BigDecimal proSize = previous.getProSize();
        if (null == proSize) {
            return;
        }
        Integer improveLevel = productionContext.getProductionParam().getSameProSizeProductionQty();
        if (null == improveLevel || improveLevel <= BigDecimal.ZERO.intValue()) {
            return;
        }
        String businessTypeText = "跨组同寸口";
        //排产流程日志记录 =====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划跨组同寸口排程开始====
        ProductionLogUtils.addStartTypeProductionLog(productionContext, businessTypeText, MouldProductionLogType.CROSS_SAME_PRO_SIZE_LOG);
        List<MonthPlanManufacturingRequirementVo> crossSameProSizeProductionList = ProductionPlanUtils.getCrossSameProSizeProductionList(groupPlanProductionContext, previous.getMonthPlanId(), proSize, improveLevel);
        if (CollectionUtils.isEmpty(crossSameProSizeProductionList)) {
            //排产流程日志记录 ===跨组同寸口排产：没有跨组同寸口的物料排产计划
            ProductionLogUtils.addNoTypeProductionPlanLog(productionContext, businessTypeText, MouldProductionLogType.CROSS_SAME_PRO_SIZE_LOG);
            return;
        }
        if (ProductionFirstSortOptionsEnum.DELIVERY_DATE == groupPlanProductionContext.getProductionPlanGroup().getGroup()) {
            //有交期先按交期再按排产顺序排序
            crossSameProSizeProductionList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getDeliveryDateDue).thenComparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        } else {
            //无交期，直接按排产顺序排序
            crossSameProSizeProductionList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        }
        crossSameProSizeProductionList.stream().forEach(crossSameProSizePlan -> {
            //排产流程日志记录 ====跨组同寸口排产：前一条排产计划ID：%s，排产计划ID：%s，计划量：%d, 单条硫化秒：%d
            ProductionLogUtils.addBeforeBusinessProductionLog(productionContext, businessTypeText, MouldProductionLogType.CROSS_SAME_PRO_SIZE_LOG, previous, crossSameProSizePlan);
            //构建排产信息
            SinglePlanProductionContext sameDeliverySingleContext = ProductionPlanUtils.buildSinglePlanProductionContext(crossSameProSizePlan, groupPlanProductionContext.getProductionContext());
            sameDeliverySingleContext.setGroupContext(groupPlanProductionContext);
            singlePlanProductionService.run(sameDeliverySingleContext, userObj);
            //排产流程日志记录 ====跨组同寸口排产结束：排产计划ID：%s，计划量：%d
            ProductionLogUtils.addAfterBusinessProductionLog(productionContext, businessTypeText, MouldProductionLogType.CROSS_SAME_PRO_SIZE_LOG, crossSameProSizePlan);
        });
    }
}
