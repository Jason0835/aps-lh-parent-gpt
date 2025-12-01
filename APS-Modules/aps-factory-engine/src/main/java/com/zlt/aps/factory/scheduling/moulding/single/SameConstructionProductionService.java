package com.zlt.aps.factory.scheduling.moulding.single;

import com.tlt.aps.enums.ProductionFirstSortOptionsEnum;
import com.tlt.aps.enums.YesOrNoEnum;
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

import java.util.Comparator;
import java.util.List;

/**
 * 共用生胎排产
 * 单计划执行完，执行共用生胎排产
 * 根据SYS024 参数判断
 * 如果配置的值不是Y。则不执行
 * 否则，查找同embryoCode的其它计划(全计划查找，不局限于同一组)
 * 提示其与第一个embryoCode计划排产
 *
 * @author
 */
@Slf4j
@Service(value = "sameConstructionProductionService")
public class SameConstructionProductionService extends AbstractSinglePlanProductionService {

    public SameConstructionProductionService(ProductionSchedulingDataService dataService,
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
        String embryoCode = previous.getEmbryoCode();
        if (StringUtils.isBlank(embryoCode)) {
            return;
        }
        //获取共用生胎排产开关
        GroupPlanProductionContext groupPlanProductionContext = singlePlanProductionContext.getGroupContext();
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        String isSameConstructionProduction = productionContext.getProductionParam().getIsSameConstructionProduction();
        if (!YesOrNoEnum.YES.getCode().equalsIgnoreCase(isSameConstructionProduction)) {
            return;
        }
        //开启共用生胎排产
        List<MonthPlanManufacturingRequirementVo> sameEmbryoCodeProductionPlanList = ProductionPlanUtils.getSameEmbryoCodeProductionPlanList(groupPlanProductionContext, previous.getMonthPlanId(), embryoCode);
        if (CollectionUtils.isEmpty(sameEmbryoCodeProductionPlanList)) {
            //排产流程日志记录
            String noPlanLogContent = "====共用生胎排产：没有共用生胎的物料排产计划";
            log.info(noPlanLogContent);
            MouldProductionLog noPlanLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_CONSTRUCTION_LOG, noPlanLogContent);
            saveProductionLog(productionContext, noPlanLog);
            return;
        }
        ProductionFirstSortOptionsEnum groupType = groupPlanProductionContext.getProductionPlanGroup().getGroup();
        if (ProductionFirstSortOptionsEnum.DELIVERY_DATE == groupType) {
            //有交期先按交期再按排产顺序最后量小优先排序
            Comparator comparator = Comparator.comparing(MonthPlanManufacturingRequirementVo::getDeliveryDateDue, Comparator.nullsLast(Comparator.naturalOrder())).
                    thenComparing(MonthPlanManufacturingRequirementVo::getProductionSequence).thenComparing(MonthPlanManufacturingRequirementVo::getProductionQty);
            sameEmbryoCodeProductionPlanList.sort(comparator);
        } else {
            //无交期，直接按排产顺序、量小优先 排序
            sameEmbryoCodeProductionPlanList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence)
                    .thenComparing(MonthPlanManufacturingRequirementVo::getProductionQty));
        }
        sameEmbryoCodeProductionPlanList.stream().forEach(sameEmbryoCodePlan -> {
            //排产流程日志记录
            ProductionLogUtils.addBeforeSameConstructionProductionLog(productionContext, previous, sameEmbryoCodePlan);
            //构建排产信息
            SinglePlanProductionContext sameConstructionSingleContext = ProductionPlanUtils.buildSinglePlanProductionContext(sameEmbryoCodePlan, productionContext);
            GroupPlanProductionContext newGroupContext = buildEmptyGroupContext(context, productionContext, sameEmbryoCodePlan.getGroupType());
            sameConstructionSingleContext.setGroupContext(newGroupContext);
            productionPlan(sameConstructionSingleContext, userObj);
            //排产流程日志记录
            ProductionLogUtils.addAfterSameConstructionProductionLog(productionContext, sameEmbryoCodePlan);
        });
    }
}
