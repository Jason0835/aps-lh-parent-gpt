package com.zlt.aps.factory.scheduling.init;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.ProductionPlanGroupVo;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.ProductionLogUtils;
import com.zlt.aps.factory.utils.ProductionPlanUtils;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionRecord;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.domain.entity.PlanOrderSortConfiguration;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用的分厂生产计划初始化及检查业务
 * 主要针对半钢，全钢业务
 *
 * @author
 */
@Slf4j
@Service(value = "generalInitService")
public class GeneralInitService extends AbstractProductionBusinessService {

    public GeneralInitService(ProductionSchedulingDataService dataService) {
        super(dataService);
    }

    @Override
    public void run(Context context, Object userObj) {
        String logContent = String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划初始化及检查开始====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        log.info(logContent);
        //构建排产基础初始化对象
        ProductionContext productionContext = buildProductionContext(context);
        MouldProductionLog generalInit = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.INIT_LOG, logContent);
        saveProductionLog(productionContext, generalInit);
        //得到制造需求计划
        List<SaleMonthPlanRequire> monthPlanRequireList = getDataService().getFactoryMonthPlan(productionContext);
        if (CollectionUtils.isEmpty(monthPlanRequireList)) {
            String planListIsNull = I18nUtil.getMessage("alg.data.alter.message.planListIsNull");
            throw new BusinessException(String.format(planListIsNull, context.getYear(), context.getMonth(), context.getMonthPlanVersion()));
        }
        //初始化缓存-物料信息、排产参数、拆A率、利润等级值、模具列表
        initCache(productionContext);
        //删除旧数据
        deleteOldData(productionContext);
        //构造月生产计划初始化信息
        List<MonthPlanManufacturingRequirementVo> monthPlanInitList = new ArrayList<>();
        for (SaleMonthPlanRequire monthPlanRequire : monthPlanRequireList) {
            MonthPlanManufacturingRequirementVo monthPlanInit = ProductionPlanUtils.buildMonthPlanInit(productionContext, monthPlanRequire);
            monthPlanInitList.add(monthPlanInit);
        }
        //计划标记是否继作
        markIsContinue(monthPlanInitList, productionContext);
        //标记不排产
        markFactoryNoProduction(monthPlanInitList, productionContext);
        //20250430 设置规格代号，胚胎代码及成型法
        setProductionPlanInfo(productionContext);
        //20251014 ZLT 汇总物料的备货量，用于备货计划的排序
        summaryProductCodeStockUpQty(monthPlanInitList);
        //按排产顺序配置分组及排序--按第一排产顺序进行分组，并对每组按第二排产顺序进行排序
        List<PlanOrderSortConfiguration> sortConfigurationList = getDataService().getProductionConfiguration(productionContext);
        List<ProductionPlanGroupVo> groupData = ProductionPlanUtils.getGroupPlan(sortConfigurationList, productionContext.getFactoryCode(), monthPlanInitList);
        if (CollectionUtils.isEmpty(groupData)) {
            throw new BusinessException("请先配置分厂排产顺序!");
        }
        //初始化模具产能
        initMouldCapacity(monthPlanInitList, productionContext);
        //检查
        ProductionPlanUtils.initCheck(monthPlanInitList);
        String checkFinish = String.format("排产版本：%s，检查完成", productionContext.getProductionVersion());
        MouldProductionLog initCheckFinish = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.INIT_LOG, checkFinish);
        saveProductionLog(productionContext, initCheckFinish);
        //生成不排产数据
        List<MonthPlanNoProductionRecord> factoryNoProductionPlanList = ProductionPlanUtils.createNoProductionRecordData(monthPlanInitList);
        int count = 0;
        if (!CollectionUtils.isEmpty(factoryNoProductionPlanList)) {
            count = factoryNoProductionPlanList.size();
        }
        String initNoProductionPlan = String.format("排产版本：%s，共提取不排产计划条数：%d", productionContext.getProductionVersion(), count);
        MouldProductionLog initNoProductionPlanLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.INIT_LOG, initNoProductionPlan);
        saveProductionLog(productionContext, initNoProductionPlanLog);
        //保存不排产记录
        getDataService().saveNoProductionPlanRecord(factoryNoProductionPlanList);
        //保存分厂计划初始数据
        getDataService().saveMonthPlanInit(monthPlanInitList);
        String initComplete = String.format("===分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划初始化及检查结束===========================", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        log.info(initComplete);
        MouldProductionLog initCompleteLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.INIT_LOG, initComplete);
        saveProductionLog(productionContext, initCompleteLog);
        saveLastLogs(productionContext, MouldProductionLogType.INIT_LOG);
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
        //物料及施工阶段信息
        initProductInfo(context);
        //折损率
        initProductALevel(context);
        //利润优先值
        initProfitInfo(context);
        //模具信息列表
        initMouldBaseInfo(context);
        //续作规格及模具
        initContinueInfo(context);
    }

    /**
     * 设置生产版本号，如果已经有生产版本号，则不进行设置
     * 否则根据当前时间戳及版本号前缀设置
     * 已有生产版本号，则根据生产版本号删除旧有数据
     *
     * @param productionContext
     */
    private void deleteOldData(ProductionContext productionContext) {
        String productionVersion = productionContext.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            throw new BusinessException(I18nUtil.getMessage("alg.data.alter.message.productionVersionNoEmpty"));
        }
        //删除版本已有数据
        getDataService().deletedInitData(productionContext);
        getDataService().deletedMouldProductionData(productionContext);
    }

}
