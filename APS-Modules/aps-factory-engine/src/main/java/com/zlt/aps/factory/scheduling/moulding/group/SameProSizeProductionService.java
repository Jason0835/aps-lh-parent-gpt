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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 同寸口排产
 *
 * @author
 */
@Slf4j
@Service(value = "sameProSizeGeneral")
public class SameProSizeProductionService extends AbstractProductionBusinessService {

    private final IProductionBusinessService singlePlanProductionService;

    public SameProSizeProductionService(ProductionSchedulingDataService dataService,
                                        @Qualifier("singlePlanProductionService") IProductionBusinessService singlePlanProductionService) {
        super(dataService);
        this.singlePlanProductionService = singlePlanProductionService;
    }

    @Override
    public void run(Context context, Object userObj) {
        SinglePlanProductionContext singleContext = (SinglePlanProductionContext) context;
        GroupPlanProductionContext groupPlanProductionContext = singleContext.getGroupContext();
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        ProductionFirstSortOptionsEnum groupType = groupPlanProductionContext.getProductionPlanGroup().getGroup();
        String businessTypeText = "同寸口";
        //排产流程日志记录 =====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划同寸口排程开始====
        ProductionLogUtils.addStartTypeProductionLog(productionContext, businessTypeText, MouldProductionLogType.SAME_PRO_SIZE_LOG);
        //获取上一个排产计划
        MonthPlanManufacturingRequirementVo previous = singleContext.getProductionPlan();
        List<MonthPlanManufacturingRequirementVo> allSameProSizeProductionList = getSameProSizePlan(groupPlanProductionContext, previous);
        if (CollectionUtils.isEmpty(allSameProSizeProductionList)) {
            //排产流程日志记录 ===组内同寸口排产：没有同寸口的物料排产计划
            ProductionLogUtils.addNoTypeProductionPlanLog(productionContext, businessTypeText, MouldProductionLogType.SAME_PRO_SIZE_LOG);
            return;
        }
        if (ProductionFirstSortOptionsEnum.DELIVERY_DATE == groupType) {
            Comparator comparator = Comparator.comparing(MonthPlanManufacturingRequirementVo::getDeliveryDateDue, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(MonthPlanManufacturingRequirementVo::getProductionSequence);
            //有交期先按交期再按排产顺序排序
            allSameProSizeProductionList = ProductionPlanUtils.getListBySortAndProductCode(allSameProSizeProductionList, comparator);
        } else {
            //无交期
            allSameProSizeProductionList = ProductionPlanUtils.getListBySortAndProductCode(allSameProSizeProductionList, Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        }
        allSameProSizeProductionList.stream().forEach(sameProSizePlan -> {
            //排产流程日志记录 ====同寸口排产：前一条排产计划ID：%s，排产计划ID：%s，计划量：%d, 单条硫化秒：%d
            ProductionLogUtils.addBeforeBusinessProductionLog(productionContext, businessTypeText, MouldProductionLogType.SAME_PRO_SIZE_LOG, previous, sameProSizePlan);
            //构建排产信息
            SinglePlanProductionContext sameDeliverySingleContext = ProductionPlanUtils.buildSinglePlanProductionContext(sameProSizePlan, productionContext);
            GroupPlanProductionContext newGroupContext = buildEmptyGroupContext(context, productionContext, sameProSizePlan.getGroupType());
            sameDeliverySingleContext.setGroupContext(newGroupContext);
            singlePlanProductionService.run(sameDeliverySingleContext, userObj);
            //排产流程日志记录 ====同寸口排产结束：排产计划ID：%s，计划量：%d
            ProductionLogUtils.addAfterBusinessProductionLog(productionContext, businessTypeText, MouldProductionLogType.SAME_PRO_SIZE_LOG, sameProSizePlan);
        });
    }

    /**
     * 获取同寸口计划
     * 先
     *
     * @param groupPlanProductionContext
     * @param previous
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getSameProSizePlan(GroupPlanProductionContext groupPlanProductionContext, MonthPlanManufacturingRequirementVo previous) {
        BigDecimal proSize = previous.getProSize();
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        List<MonthPlanManufacturingRequirementVo> allSameProSizeProductionList = new ArrayList<>();
        //组内同寸口
        List<MonthPlanManufacturingRequirementVo> sameProSizeProductionList = ProductionPlanUtils.getSameProSizeProductionList(groupPlanProductionContext, previous.getMonthPlanId(), proSize);
        if (!CollectionUtils.isEmpty(sameProSizeProductionList)) {
            allSameProSizeProductionList.addAll(sameProSizeProductionList);
        }
        //跨组同寸口
        Integer improveLevel = productionContext.getProductionParam().getSameProSizeProductionQty();
        if (null == improveLevel || improveLevel <= BigDecimal.ZERO.intValue()) {
            return allSameProSizeProductionList;
        }
        List<MonthPlanManufacturingRequirementVo> crossSameProSizeProductionList = ProductionPlanUtils.getCrossSameProSizeProductionList(groupPlanProductionContext, previous.getMonthPlanId(), proSize, improveLevel);
        if (CollectionUtils.isEmpty(crossSameProSizeProductionList)) {
            return allSameProSizeProductionList;
        }
        Set<Long> monthPlanIdSet = allSameProSizeProductionList.stream().map(MonthPlanManufacturingRequirementVo::getMonthPlanId).collect(Collectors.toSet());
        crossSameProSizeProductionList.stream().forEach(crossSameProSizePlan -> {
            if (monthPlanIdSet.contains(crossSameProSizePlan.getMonthPlanId())) {
                return;
            }
            allSameProSizeProductionList.add(crossSameProSizePlan);
        });
        return allSameProSizeProductionList;
    }

}
