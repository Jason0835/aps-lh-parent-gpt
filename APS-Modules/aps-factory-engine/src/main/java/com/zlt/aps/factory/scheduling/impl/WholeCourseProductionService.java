package com.zlt.aps.factory.scheduling.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.ProductionPlanGroupVo;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.IProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.moulding.GroupPlanProductionContext;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 一键生成分厂排产计划，包含
 * 初始化，分厂模具排产
 * 主要针对半钢，全钢业务
 *
 * @author
 */
@Slf4j
@Service(value = "wholeCourseProductionService")
public class WholeCourseProductionService extends AbstractProductionBusinessService {

    private final IProductionBusinessService groupPlanProductionService;

    private final IProductionBusinessService continueProductProductionService;

    private final IProductionBusinessService matchingProductionService;

    public WholeCourseProductionService(ProductionSchedulingDataService dataService,
                                        @Qualifier("groupPlanMouldingProductionService") IProductionBusinessService groupPlanProductionService,
                                        @Qualifier("continueProductMould") IProductionBusinessService continueProductProductionService,
                                        @Qualifier("matchingProduction") IProductionBusinessService matchingProductionService) {
        super(dataService);
        this.groupPlanProductionService = groupPlanProductionService;
        this.continueProductProductionService = continueProductProductionService;
        this.matchingProductionService = matchingProductionService;
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
        log.info(String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划模具排程开始====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion()));
        //20251014 ZLT 汇总物料的备货量，用于备货计划的排序
        summaryProductCodeStockUpQty(monthPlanList);
        //按排产顺序配置分组及排序--按第一排产顺序进行分组，并对每组按第二排产顺序进行排序
        List<PlanOrderSortConfiguration> sortConfigurationList = getDataService().getProductionConfiguration(productionContext);
        List<ProductionPlanGroupVo> groupData = ProductionPlanUtils.getGroupPlan(sortConfigurationList, productionContext.getFactoryCode(), monthPlanList);
        if (CollectionUtils.isEmpty(groupData)) {
            throw new BusinessException("请先配置分厂排产顺序!");
        }
        //20250605 寸口，轮胎类型产能配置初始化
        initCapacityConfiguration(productionContext);
        //20250726 特殊轮胎产能控制
        tireTypeCapacityControl(productionContext, monthPlanList);
        //初始化模具产能
        initMouldCapacity(monthPlanList, productionContext);
        //20250516 ZLT 模具产能预占计算
        calculatedPreemptMouldCapacity(productionContext, monthPlanList);
        //保存模具产能预占信息
        saveMouldPreCapacity(productionContext, monthPlanList);
        //20250605 寸口产能控制
        capacityControl(monthPlanList, productionContext);
        //20250925 一次法将胎体层级统一调整为多层
        ProductionPlanUtils.handlerOneMethodTireFabricNumber(monthPlanList);
        //设置汇总排产量
        summaryProductCodeProductionQty(monthPlanList);
        //需要推迟到模具产能预占后检查
        initNoProductionRecord(productionContext, monthPlanList);

        //保存不排产记录--清除临时变量
        getDataService().saveNoProductionPlanRecord(productionContext.getNoProductionRecordList());
        productionContext.setNoProductionRecordList(Collections.emptyList());
        //保存初始化信息--包含排产顺序信息
        getDataService().saveMonthPlanInit(monthPlanList);
        ProductionLogUtils.addDayMaxMouldQtyResultLog(productionContext, MouldProductionLogType.WHOLE_PRODUCTION);
        //排产流程日志记录
        MouldProductionLog endProductionSortGroupLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.GROUP_SORT_LOG, "一键排产：排产顺序分组及设置排产顺序值结束");
        saveProductionLog(productionContext, endProductionSortGroupLog);
        //20250428 ZLT 续作规格用续作模具排产
        continueProductProductionService.run(productionContext, userObj);
        //开始分组排产
        startGroupProduction(context, userObj, productionContext, groupData, monthPlanList);
        ProductionLogUtils.addDayProductionQtyResultLog(productionContext, MouldProductionLogType.WHOLE_PRODUCTION_END);
        ProductionLogUtils.addDayProductionMouldQtyResultLog(productionContext, MouldProductionLogType.WHOLE_PRODUCTION_END);
        log.info("排产结束，进行数据保存.......");
        //保存排产结果信息
        Map<Long, Long> sumProductionMap = saveProductionResult(productionContext);
        //保存未排计划明细
        saveNoProductionPlanResult(productionContext, sumProductionMap);
        saveLastLogs(productionContext, MouldProductionLogType.WHOLE_PRODUCTION_END);
        log.info(String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划模具排程结束====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion()));
    }

    /**
     * 执行初始化动作
     *
     * @param context
     * @param userObj
     * @return
     */
    private ProductionContext productionInit(Context context, Object userObj) {
        String productionStart = String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，制造需求计划开始排产====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        log.info(productionStart);
        //构建排产基础初始化对象
        ProductionContext productionContext = buildProductionContext(context);
        MouldProductionLog productionStartLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.WHOLE_PRODUCTION, productionStart);
        saveProductionLog(productionContext, productionStartLog);
        //得到制造需求计划
        List<SaleMonthPlanRequire> monthPlanRequireList = getDataService().getFactoryMonthPlan(productionContext);
        if (CollectionUtils.isEmpty(monthPlanRequireList)) {
            String planListIsNull = I18nUtil.getMessage("alg.data.alter.message.planListIsNull");
            throw new BusinessException(String.format(planListIsNull, context.getYear(), context.getMonth(), context.getMonthPlanVersion()));
        }
        String initContent = String.format("分厂%s, 计划年月：%d-%d, 计划版本：%s，制造需求计划一键排产初始化开始", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        MouldProductionLog productionInitLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.INIT_LOG, initContent);
        saveProductionLog(productionContext, productionInitLog);
        //初始化缓存-物料信息、排产参数、拆A率、利润等级值、模具列表
        initCache(productionContext);
        //生成版本号及删除旧数据
        setProductionVersionAndDeleteOldData(productionContext);
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
        initProductALevel(context);
        //利润优先值
        initProfitInfo(context);
        //排产分组信息
        initProductionGroupInfo(context);
        //模具信息列表
        initMouldBaseInfo(context);
        //续作规格及模具
        initContinueInfo(context);
        //续作满月排产模式规格
        initContinueFullMonthProductInfo(context);
    }

    /**
     * 开始进行分组排产
     *
     * @param context           排产上下文
     * @param userObj           参数
     * @param productionContext 模具排产上下文
     * @param groupData         分组数据(全部)
     * @param monthPlanList     所有排产计划信息
     */
    private void startGroupProduction(Context context, Object userObj, ProductionContext productionContext, List<ProductionPlanGroupVo> groupData, List<MonthPlanManufacturingRequirementVo> monthPlanList) {
        //20250909 ZLT 开启按寸口由大到小排产模式
        String openProSizeProductionModel = productionContext.getProductionParam().getOpenProSizeProductionModel();
        //没有开启
        if (!ProductionConstant.YES_VALUE.equals(openProSizeProductionModel)) {
            groupPlanProduction(context, userObj, productionContext, groupData);
            return;
        }
        String prefixText = "一键排产：";
        ProductionLogUtils.addProSizeModelStartProductionLog(productionContext, prefixText);
        //开启，按寸口进行提取，由大到小
        Set<BigDecimal> proSizeSet = monthPlanList.stream().map(MonthPlanManufacturingRequirementVo::getProSize).collect(Collectors.toSet());
        List<BigDecimal> proSizeList = new ArrayList<>(proSizeSet);
        proSizeList.sort(Comparator.comparing(BigDecimal::doubleValue, Comparator.reverseOrder()));
        proSizeList.stream().forEach(proSize -> {
            //按寸口提取各自分组数据
            List<ProductionPlanGroupVo> proSizeGroupData = new ArrayList<>();
            groupData.stream().forEach(groupInfo -> {
                ProductionPlanGroupVo proSizeGroupInfo = new ProductionPlanGroupVo();
                proSizeGroupInfo.setGroup(groupInfo.getGroup());
                proSizeGroupData.add(proSizeGroupInfo);
                List<MonthPlanManufacturingRequirementVo> groupPlanList = groupInfo.getGroupPlanList();
                if (CollectionUtils.isEmpty(groupPlanList)) {
                    proSizeGroupInfo.setGroupPlanList(Collections.emptyList());
                    return;
                }
                List<MonthPlanManufacturingRequirementVo> proSizeDataList = groupPlanList.stream().filter(productionPlan -> proSize.equals(productionPlan.getProSize())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(proSizeDataList)) {
                    proSizeGroupInfo.setGroupPlanList(Collections.emptyList());
                    return;
                }
                proSizeGroupInfo.setGroupPlanList(proSizeDataList);
            });
            //按寸口排产
            ProductionLogUtils.addProSizeStartProductionLog(productionContext, proSize, prefixText);
            groupPlanProduction(context, userObj, productionContext, proSizeGroupData);
            return;
        });
    }

    /**
     * 分组数据排产
     *
     * @param context           排产上下文
     * @param userObj           参数
     * @param productionContext 模具排产上下文
     * @param groupData         分组数据
     */
    private void groupPlanProduction(Context context, Object userObj, ProductionContext productionContext, List<ProductionPlanGroupVo> groupData) {
        String prefixText = "一键排产：";
        //按照分组顺序，顺序排产
        groupData.stream().forEach(groupDataList -> {
            //分组排产开始-流程日志记录 一键排产：开始进行[%s]分组排产
            ProductionLogUtils.addGroupStartProductionLog(productionContext, groupDataList, prefixText);
            //构建分组排产参数信息并排产
            GroupPlanProductionContext groupContext = new GroupPlanProductionContext();
            BeanUtils.copyProperties(context, groupContext);
            groupContext.setProductionContext(productionContext);
            groupContext.setProductionPlanGroup(groupDataList);
            groupPlanProductionService.run(groupContext, userObj);
            //分组排产结束-流程日志记录 一键排产：[%s]分组排产结束
            ProductionLogUtils.addGroupEndProductionLog(productionContext, groupDataList, prefixText);
        });
    }

    /**
     * 初始化检查，并生产不排产记录
     *
     * @param productionContext 排产上下文
     * @param monthPlanInitList 计划
     */
    private void initNoProductionRecord(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> monthPlanInitList) {
        //检查
        String text = "一键";
        ProductionPlanUtils.initCheck(monthPlanInitList);
        //初始化检查流程日志 一键排产版本：%s，检查完成
        ProductionLogUtils.addInitCheckFinishLog(productionContext, text);
        //生成不排产数据
        List<MonthPlanNoProductionRecord> factoryNoProductionPlanList = ProductionPlanUtils.createNoProductionRecordData(monthPlanInitList);
        int count = 0;
        //放入排产上下文，后续使用
        if (!CollectionUtils.isEmpty(factoryNoProductionPlanList)) {
            productionContext.setNoProductionRecordList(factoryNoProductionPlanList);
            Map<Long, MonthPlanNoProductionRecord> noProductionRecordMap = factoryNoProductionPlanList.stream().collect(Collectors.toMap(MonthPlanNoProductionRecord::getMonthPlanId, record -> record));
            productionContext.setNoProductionRecordMap(noProductionRecordMap);
            count = factoryNoProductionPlanList.size();
        } else {
            productionContext.setNoProductionRecordMap(Collections.emptyMap());
        }
        //不排产记录结果日志 一键排产版本：%s，共提取不排产计划条数：%d
        ProductionLogUtils.addNoProductionRecordResultLog(productionContext, text, count);
        //初始化完成日志 ===分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划一键排产初始化及检查结束==========
        ProductionLogUtils.addInitFinishLog(productionContext, text);
    }

    /**
     * 设置生产版本号，如果已经有生产版本号，则不进行设置
     * 否则根据当前时间戳及版本号前缀设置
     * 已有生产版本号，则根据生产版本号删除旧有数据
     *
     * @param productionContext
     */
    private void setProductionVersionAndDeleteOldData(ProductionContext productionContext) {
        String productionVersion = productionContext.getProductionVersion();
        setProductionVersion(productionContext);
        if (StringUtils.isBlank(productionVersion)) {
            return;
        }
        //删除版本已有数据
        getDataService().deletedInitData(productionContext);
        getDataService().deletedMouldProductionData(productionContext);
    }

}
