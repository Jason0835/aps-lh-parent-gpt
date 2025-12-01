package com.zlt.aps.factory.scheduling.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.ProductionPlanGroupVo;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.ProductionPlanUtils;
import com.zlt.aps.monthplan.api.domain.entity.PlanOrderSortConfiguration;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 寸口产能需求计算
 *
 * @author
 */
@Slf4j
@Service(value = "sizeCapacityRequireCalculateService")
public class SizeCapacityRequireCalculateService extends AbstractProductionBusinessService {

    public SizeCapacityRequireCalculateService(ProductionSchedulingDataService dataService) {
        super(dataService);
    }

    @Override
    public void run(Context context, Object userObj) {
        //构建排产初始化数据及构建排产上下文信息
        ProductionContext productionContext = productionInit(context, userObj);
        //进行排产
        List<MonthPlanManufacturingRequirementVo> monthPlanList = productionContext.getMonthPlanInitList();
        if (CollectionUtils.isEmpty(monthPlanList)) {
            String initEmpty = I18nUtil.getMessage("alg.data.alter.message.initEmpty");
            throw new BusinessException(String.format(initEmpty, context.getYear(), context.getMonth(), context.getMonthPlanVersion()));
        }
        productionContext.setMonthPlanInitList(Collections.emptyList());
        //更新计划信息
        Map<Long, MonthPlanManufacturingRequirementVo> productionPlanMap = new HashMap<>();
        monthPlanList.stream().forEach(productionPlan -> productionPlanMap.put(productionPlan.getMonthPlanId(), productionPlan));
        productionContext.setMonthPlanInitMap(productionPlanMap);
        //20250430 设置规格代号，胚胎代码及成型法
        setProductionPlanInfo(productionContext);
        //20251014 ZLT 汇总物料的备货量，用于备货计划的排序
        summaryProductCodeStockUpQty(monthPlanList);
        //按排产顺序配置分组及排序--按第一排产顺序进行分组，并对每组按第二排产顺序进行排序
        List<PlanOrderSortConfiguration> sortConfigurationList = getDataService().getProductionConfiguration(productionContext);
        List<ProductionPlanGroupVo> groupData = ProductionPlanUtils.getGroupPlan(sortConfigurationList, productionContext.getFactoryCode(), monthPlanList);
        if (CollectionUtils.isEmpty(groupData)) {
            throw new BusinessException("请先配置分厂排产顺序!");
        }
        //20250605 轮胎类型产能配置初始化
        initTireCapacityConfiguration(productionContext);
        //20250605 轮胎类型产能控制
        tireCapacityControl(monthPlanList, productionContext);
        //初始化模具产能
        initMouldCapacity(monthPlanList, productionContext);
        //20250516 ZLT 共用模具产能预占计算
        calculatedPreemptMouldCapacity(productionContext, monthPlanList);
        //20250620 不满足最小批量的剔除--设置可排产量为零
        ProductionPlanUtils.rejectFallShortOfMinQty(monthPlanList, productionContext, true);
        //检查不可排产的需求
        ProductionPlanUtils.initCheck(monthPlanList);
        //设置最终需求信息
        context.setSizeCapacityRequireList(monthPlanList);
    }

    /**
     * 执行初始化动作
     *
     * @param context
     * @param userObj
     * @return
     */
    private ProductionContext productionInit(Context context, Object userObj) {
        //构建排产基础初始化对象
        ProductionContext productionContext = buildProductionContext(context);
        //得到制造需求计划
        List<SaleMonthPlanRequire> monthPlanRequireList = getDataService().getFactoryMonthPlan(productionContext);
        if (CollectionUtils.isEmpty(monthPlanRequireList)) {
            String planListIsNull = I18nUtil.getMessage("alg.data.alter.message.planListIsNull");
            throw new BusinessException(String.format(planListIsNull, context.getYear(), context.getMonth(), context.getMonthPlanVersion()));
        }
        //初始化缓存-物料信息、排产参数、拆A率、利润等级值、模具列表
        initCache(productionContext);
        //构造月生产计划初始化信息
        List<MonthPlanManufacturingRequirementVo> monthPlanInitList = new ArrayList<>();
        for (SaleMonthPlanRequire monthPlanRequire : monthPlanRequireList) {
            MonthPlanManufacturingRequirementVo monthPlanInit = ProductionPlanUtils.buildMonthPlanInit(productionContext, monthPlanRequire);
            monthPlanInitList.add(monthPlanInit);
        }
        //标记是否继作
        markIsContinue(monthPlanInitList, productionContext);
        //标记不排产
        markFactoryNoProduction(monthPlanInitList, productionContext);
        //存入缓存
        productionContext.setMonthPlanInitList(monthPlanInitList);
        return productionContext;
    }

    /**
     * 初始化 物料信息、系统控制参数、物料折损率、施工阶段信息、模具信息
     * 物料与模具关系信息、利润优先值
     *
     * @param context
     * @return
     */
    private void initCache(ProductionContext context) {
        //系统参数
        initSysParams(context);
        //排产天数、停工日集合
        initMonthProductionDays(context);
        //最小批量
        initMinimumLotSizeConfiguration(context);
        //物料及施工阶段信息
        initProductInfo(context);
        //折损率
        context.setProductDamageMap(new HashMap<>());
        //利润优先值
        initProfitInfo(context);
        //模具信息列表
        initMouldBaseInfo(context);
        //续作规格及模具
        initContinueInfo(context);
        //续作满月排产模式规格
        initContinueFullMonthProductInfo(context);
    }
}
