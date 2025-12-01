package com.zlt.aps.factory.scheduling.moulding.single;

import com.tlt.aps.enums.ProductionFirstSortOptionsEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.MouldInfoVO;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.IProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.moulding.SinglePlanProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.NoProductionReasonUtils;
import com.zlt.aps.factory.utils.ProductUtils;
import com.zlt.aps.factory.utils.ProductionGroupUtils;
import com.zlt.aps.factory.utils.ProductionLogUtils;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 抽象的单计划排产
 * 主要实现单计划排产逻辑处理
 *
 * @author ZLT
 * @date 20250220
 */
@Slf4j
public abstract class AbstractSinglePlanProductionService extends AbstractProductionBusinessService {
    /**
     * 通用单计划排产
     */
    private final IProductionBusinessService generalSinglePlanProductionService;
    /**
     * 有交期单计划排产
     */
    private final IProductionBusinessService deliveryDateSinglePlanProductionService;
    /**
     * 拼模排产
     */
    private final IProductionBusinessService assemblingMouldProductionService;

    public AbstractSinglePlanProductionService(ProductionSchedulingDataService dataService,
                                               IProductionBusinessService assemblingMouldProductionService,
                                               IProductionBusinessService generalSinglePlanProductionService,
                                               IProductionBusinessService deliveryDateSinglePlanProductionService) {
        super(dataService);
        this.assemblingMouldProductionService = assemblingMouldProductionService;
        this.generalSinglePlanProductionService = generalSinglePlanProductionService;
        this.deliveryDateSinglePlanProductionService = deliveryDateSinglePlanProductionService;
    }

    /**
     * 排产计划
     */
    protected void productionPlan(SinglePlanProductionContext singlePlanProductionContext, Object userObj) {
        ProductionContext productionContext = singlePlanProductionContext.getGroupContext().getProductionContext();
        MonthPlanManufacturingRequirementVo productionPlan = singlePlanProductionContext.getProductionPlan();
        Long monthPlanId = productionPlan.getMonthPlanId();
        if (productionContext.isProductionFinishPlan(monthPlanId)) {
            log.warn("排产计划已排产完毕。。。。无需再次排产");
            return;
        }
        if (null == productionPlan || productionPlan.getProductionQty() <= 0) {
            log.warn("排产计划不存在或是可排产量为0");
            return;
        }
        //单计划排产流程开始：排产流程日志打印及保存记录 分厂%s,计划年月：%d-%d,排产版本:%s，单计划[%s]模具排产开始,计划排产量%d
        ProductionLogUtils.addBeforeSinglePlanProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG);

        ProductionFirstSortOptionsEnum group = singlePlanProductionContext.getGroupContext().getProductionPlanGroup().getGroup();

        MonthPlanManufacturingRequirementVo originalPlan = productionContext.getMonthPlanInitMap().get(monthPlanId);
        List<MouldInfoVO> mouldList = singlePlanProductionContext.getEnableMouldList();
        if (CollectionUtils.isEmpty(mouldList)) {
            originalPlan.setIsProduction(YesOrNoEnum.NO.getValue());
            //没有可排模具
            originalPlan.addNoProductionReasonAndQty(NoProductionReasonUtils.getNoProductionMould(), originalPlan.getProductionQty());
            //单计划排产流程结果：排产流程日志打印及保存记录 %d :排产计划没有可排模具
            ProductionLogUtils.addNoEnableMouldProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG);
            //加入已排完计划集合
            productionContext.addProductionFinishPlan(monthPlanId);
            return;
        }
        List<MouldInfoVO> enableMouldList = mouldList.stream().filter(mouldInfo -> !PubUtil.isTrue(mouldInfo.getIsFinish())).collect(Collectors.toList());
        BigDecimal totalCuringTime = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(enableMouldList)) {
            for (MouldInfoVO mouldInfo : enableMouldList) {
                totalCuringTime = totalCuringTime.add(mouldInfo.getLeftOverSeconds());
            }
        }
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(productionPlan, productionContext);
        if (totalCuringTime.compareTo(singleCuringTime) < 0) {
            originalPlan.setIsProduction(YesOrNoEnum.NO.getValue());
            //模具没有产能
            originalPlan.addNoProductionReasonAndQty(NoProductionReasonUtils.getMouldNoCapacity(), originalPlan.getProductionQty());
            //单计划排产结果：模具产能-排产流程日志打印及保存记录 %d :排产计划可用模具没有产能
            ProductionLogUtils.addNoCapacityByMouldLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG);
            //加入已排完计划集合
            productionContext.addProductionFinishPlan(monthPlanId);
            return;
        }
        //20250624 拼模排产判断
        assemblingMouldProductionService.run(singlePlanProductionContext, userObj);

        //有交期计划排产
        if (ProductionFirstSortOptionsEnum.DELIVERY_DATE == group) {
            //排产流程日志记录 ===当前计划%d 为有交期计划排产=====
            ProductionLogUtils.addBeforeGroupSinglePlanProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG, "有交期");
            //有交期排产
            deliveryDateSinglePlanProductionService.run(singlePlanProductionContext, userObj);
            //加入已排完计划集合
            productionContext.addProductionFinishPlan(monthPlanId);
            //标记已经排产完毕的分组
            ProductionGroupUtils.markFinishProductionGroup(productionContext);
            return;
        }
        //无交期通用计划排产开始：排产流程日志打印及保存记录 ===当前计划%d 为通用计划排产=====
        ProductionLogUtils.addBeforeGroupSinglePlanProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG, "通用");
        //无交期通用计划排产
        generalSinglePlanProductionService.run(singlePlanProductionContext, userObj);
        //加入已排完计划集合
        productionContext.addProductionFinishPlan(monthPlanId);
        //标记已经排产完毕的分组
        ProductionGroupUtils.markFinishProductionGroup(productionContext);
    }

}
