package com.zlt.aps.factory.scheduling.moulding.single;

import com.tlt.aps.enums.ProductionFirstSortOptionsEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 跨组同规格
 * 单计划执行完，执行跨组同规格排产
 * 根据SYS020 参数判断
 * 如果没配置或是<=0。则不执行
 * 否则，查找同productCode的其它计划(全计划查找，不局限于同一组)且排产量<SYS020值的计划
 * 提示起与第一个productCode计划排产
 *
 * @author
 */
@Slf4j
@Service(value = "crossGroupSameProductProductionService")
public class CrossGroupSameProductProductionService extends AbstractSinglePlanProductionService {

    public CrossGroupSameProductProductionService(ProductionSchedulingDataService dataService,
                                                  @Qualifier("assemblingMouldProductionService") IProductionBusinessService assemblingMouldProductionService,
                                                  @Qualifier("generalSinglePlanProductionService") IProductionBusinessService generalSinglePlanProductionService,
                                                  @Qualifier("deliveryDateSinglePlanProductionService") IProductionBusinessService deliveryDateSinglePlanProductionService) {
        super(dataService, assemblingMouldProductionService, generalSinglePlanProductionService, deliveryDateSinglePlanProductionService);
    }

    /**
     * @param context 排产上下文
     * @param userObj 用户数据
     */
    @Override
    public void run(Context context, Object userObj) {
        SinglePlanProductionContext singlePlanProductionContext = (SinglePlanProductionContext) context;
        //获取当前排产计划
        MonthPlanManufacturingRequirementVo previous = singlePlanProductionContext.getProductionPlan();
        String productCode = previous.getProductCode();
        if (StringUtils.isBlank(productCode)) {
            return;
        }
        GroupPlanProductionContext groupPlanProductionContext = singlePlanProductionContext.getGroupContext();
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        Integer improveLevel = productionContext.getProductionParam().getSameProductProductionQty();
        if (null == improveLevel || improveLevel <= BigDecimal.ZERO.intValue()) {
            return;
        }
        //跨组同规格排产
        List<MonthPlanManufacturingRequirementVo> sameProductProductionList = ProductionPlanUtils.getSameProductProductionLimitList(groupPlanProductionContext, previous.getMonthPlanId(), productCode, improveLevel);
        if (CollectionUtils.isEmpty(sameProductProductionList)) {
            String noPlanLogContent = "====跨组同规格排产：没有同规格的物料排产计划";
            log.info(noPlanLogContent);
            //排产流程日志记录
            MouldProductionLog noPlanLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_PRODUCT_LOG, noPlanLogContent);
            saveProductionLog(productionContext, noPlanLog);
            return;
        }
        ProductionFirstSortOptionsEnum groupType = groupPlanProductionContext.getProductionPlanGroup().getGroup();
        if (ProductionFirstSortOptionsEnum.DELIVERY_DATE == groupType) {
            //有交期先按交期再按排产顺序排序
            Comparator comparator = Comparator.comparing(MonthPlanManufacturingRequirementVo::getDeliveryDateDue, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(MonthPlanManufacturingRequirementVo::getProductionSequence);
            sameProductProductionList.sort(comparator);
        } else {
            //无交期，直接按排产顺序排序
            sameProductProductionList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        }
        sameProductProductionList.stream().forEach(sameProductPlan -> {
            //排产流程日志记录 ====跨组同规格排产：前一条排产计划ID：%s，排产计划ID：%s，计划量：%d, 单条硫化秒：%d
            ProductionLogUtils.addBeforeCrossGroupSameProductProductionLog(productionContext, previous, sameProductPlan);
            //构建排产信息
            SinglePlanProductionContext sameProductionSingleContext = ProductionPlanUtils.buildSinglePlanProductionContext(sameProductPlan, productionContext);
            GroupPlanProductionContext newGroupContext = buildEmptyGroupContext(context, productionContext, sameProductPlan.getGroupType());
            sameProductionSingleContext.setGroupContext(newGroupContext);
            productionPlan(sameProductionSingleContext, userObj);
            //排产流程日志记录 ====跨组同规格排产结束：排产计划ID：%s，计划量：%d
            ProductionLogUtils.addAfterCrossGroupSameProductProductionLog(productionContext, sameProductPlan);
        });
    }
}
