package com.zlt.aps.factory.scheduling.moulding.group;

import com.tlt.aps.enums.ProductionFirstSortOptionsEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.ProductionPlanGroupVo;
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
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 分组计划-模具排产
 *
 * @author ZLT
 */
@Slf4j
@Service(value = "groupPlanMouldingProductionService")
public class GroupPlanMouldingProductionService extends AbstractProductionBusinessService {

    private final IProductionBusinessService singlePlanProductionService;

    private final IProductionBusinessService sameMouldDeliverDateProductionService;

    private final IProductionBusinessService sameMouldGeneralProductionService;

    private final IProductionBusinessService sameProductProductionService;

    private final IProductionBusinessService sameProSizeProductionService;

    private final IProductionBusinessService sameConstructionProductionService;

    private final IProductionBusinessService crossGroupSameProductProductionService;

    public GroupPlanMouldingProductionService(ProductionSchedulingDataService dataService,
                                              @Qualifier("singlePlanProductionService") IProductionBusinessService singlePlanProductionService,
                                              @Qualifier("sameMouldDeliverDate") IProductionBusinessService sameMouldDeliverDateProductionService,
                                              @Qualifier("sameMouldGeneral") IProductionBusinessService sameMouldGeneralProductionService,
                                              @Qualifier("sameProductGeneral") IProductionBusinessService sameProductProductionService,
                                              @Qualifier("sameProSizeGeneral") IProductionBusinessService sameProSizeProductionService,
                                              @Qualifier("sameConstructionProductionService") IProductionBusinessService sameConstructionProductionService,
                                              @Qualifier("crossGroupSameProductProductionService") IProductionBusinessService crossGroupSameProductProductionService) {
        super(dataService);
        this.singlePlanProductionService = singlePlanProductionService;
        this.sameMouldDeliverDateProductionService = sameMouldDeliverDateProductionService;
        this.sameMouldGeneralProductionService = sameMouldGeneralProductionService;
        this.sameProductProductionService = sameProductProductionService;
        this.sameProSizeProductionService = sameProSizeProductionService;
        this.sameConstructionProductionService = sameConstructionProductionService;
        this.crossGroupSameProductProductionService = crossGroupSameProductProductionService;
    }

    /**
     * 按第一排产顺序进行分组后的排产逻辑
     * 如果是有交期分组排产，则排产需要根据交期来进行排模具。
     * 交期分组的排产逻辑：初始计划排产，然后根据排产计划排的模具，按同模具同交期优先排产、其次同模具其它交期排产，最后才是同规格排产
     * 无交期分组的排产逻辑：初始计划排产，其次根据排产计划排的模具，同规格排产。最后才是同模具排产
     *
     * @param context 排产上下文
     * @param userObj 用户数据
     */
    @Override
    public void run(Context context, Object userObj) {
        String startGroupContent = String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划按第一排产顺序分组-模具排程开始====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        log.info(startGroupContent);
        GroupPlanProductionContext groupPlanProductionContext = (GroupPlanProductionContext) context;
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        //排产流程日志记录
        MouldProductionLog startGroupContentLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.GROUP_PRODUCTION_LOG, startGroupContent);
        saveProductionLog(productionContext, startGroupContentLog);
        ProductionPlanGroupVo productionPlanGroup = groupPlanProductionContext.getProductionPlanGroup();
        if (null == productionPlanGroup) {
            String noGroupLogContent = "按第一排产分组排产错误，没有分组信息";
            log.warn(noGroupLogContent);
            //排产流程日志记录
            MouldProductionLog noGroupLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.GROUP_PRODUCTION_LOG, noGroupLogContent);
            saveProductionLog(productionContext, noGroupLog);
            return;
        }
        ProductionFirstSortOptionsEnum group = productionPlanGroup.getGroup();
        if (null == group) {
            String noGroupTypeLogContent = "分组排产的分组信息不可empty...";
            log.warn(noGroupTypeLogContent);
            //排产流程日志记录
            MouldProductionLog noGroupTypeLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.GROUP_PRODUCTION_LOG, noGroupTypeLogContent);
            saveProductionLog(productionContext, noGroupTypeLog);
            return;
        }
        List<MonthPlanManufacturingRequirementVo> groupPlanList = productionPlanGroup.getGroupPlanList();
        if (CollectionUtils.isEmpty(groupPlanList)) {
            String noGroupPlanLogContent = String.format("======%s分组排产没有分组数据 The Data empty", group.getRemark());
            log.warn(noGroupPlanLogContent);
            //排产流程日志记录
            MouldProductionLog noGroupPlanLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.GROUP_PRODUCTION_LOG, noGroupPlanLogContent);
            saveProductionLog(productionContext, noGroupPlanLog);
            return;
        }
        String realStartGroupLogContent = String.format("开始对分组：%s 进行计划排产,计划数%d", group.getRemark(), groupPlanList.size());
        log.info(realStartGroupLogContent);
        //排产流程日志记录
        MouldProductionLog realStartGroupLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.GROUP_PRODUCTION_LOG, realStartGroupLogContent);
        saveProductionLog(productionContext, realStartGroupLog);
        if (ProductionFirstSortOptionsEnum.DELIVERY_DATE == group) {
            //先按交期再按排产顺序排序
            groupPlanList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getDeliveryDateDue).thenComparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        } else {
            //无交期，直接按排产顺序排序
            groupPlanList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        }
        groupPlanList.forEach(productionPlan -> {
            Long monthPlanId = productionPlan.getMonthPlanId();
            MonthPlanManufacturingRequirementVo manufacturingRequirement = productionContext.getMonthPlanInitMap().get(monthPlanId);
            if (!isNeedProduction(monthPlanId, manufacturingRequirement, productionContext)) {
                //排产流程日志记录
                MouldProductionLog noProductionPlanLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.GROUP_PRODUCTION_LOG, "计划无需排产");
                saveProductionLog(productionContext, noProductionPlanLog);
                return;
            }
            //单计划排产
            SinglePlanProductionContext singleContext = ProductionPlanUtils.buildSinglePlanProductionContext(manufacturingRequirement, productionContext);
            singleContext.setGroupContext(groupPlanProductionContext);
            singlePlanProductionService.run(singleContext, userObj);
            //交期则同模具交期排产
            if (ProductionFirstSortOptionsEnum.DELIVERY_DATE == group) {
                sameMouldDeliverDateProductionService.run(singleContext, userObj);
            }
            //同规格排产
            sameProductProductionService.run(singleContext, userObj);
            //20250424 跨组同规格查找排产处理-计划量<SYS020
            crossGroupSameProductProductionService.run(singleContext, userObj);
            //20250430 共用生胎查找排产处理 SYS024开关开启
            sameConstructionProductionService.run(singleContext, userObj);
            //同模具无交期排产
            if (ProductionFirstSortOptionsEnum.DELIVERY_DATE != group) {
                sameMouldGeneralProductionService.run(singleContext, userObj);
            }
            //20250604 同寸口排产处理
            sameProSizeProductionService.run(singleContext, userObj);
        });
    }

    /**
     * 计划是否需要排产
     * 计划是否存在，是否不排产，是否已经没有可排产量，是否有可用模具
     * true 表示需要排产 false表示不需要排产
     *
     * @param monthPlanId              计划ID
     * @param manufacturingRequirement 计划信息
     * @param productionContext        排产上下文
     */
    private boolean isNeedProduction(Long monthPlanId, MonthPlanManufacturingRequirementVo manufacturingRequirement, ProductionContext productionContext) {
        if (null == manufacturingRequirement) {
            log.warn(String.format("分组排产出错，%d :计划不存在", monthPlanId));
            return false;
        }
        if (!PubUtil.isTrue(manufacturingRequirement.getIsProduction())) {
            log.warn(String.format("%d :计划标记不排产，无需排产", monthPlanId));
            return false;
        }
        if (manufacturingRequirement.getProductionQty() <= 0) {
            manufacturingRequirement.setNoProductionQty(manufacturingRequirement.getProductionQty());
            log.warn(String.format("%d :计划无需排产，没有可排产量", monthPlanId));
            return false;
        }
        //物料编码
        String productCode = manufacturingRequirement.getProductCode();
        Set<String> enableMouldSet = productionContext.getProductRelationMouldMap().get(productCode);
        if (CollectionUtils.isEmpty(enableMouldSet)) {
            manufacturingRequirement.setNoProductionQty(manufacturingRequirement.getProductionQty());
            log.warn(String.format("%d :计划没有可用模具，不可排产", monthPlanId));
            return false;
        }
        return true;
    }
}
