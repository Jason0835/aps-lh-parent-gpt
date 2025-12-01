package com.zlt.aps.factory.scheduling.moulding;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.ConstructionStageEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.ProductionPlanGroupVo;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.IProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.ProductionLogUtils;
import com.zlt.aps.factory.utils.ProductionPlanUtils;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionRecord;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.domain.entity.PlanOrderSortConfiguration;
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
 * 通用的分厂生产计划模具排产实现
 * 主要针对半钢，全钢业务
 *
 * @author
 */
@Slf4j
@Service(value = "generalMouldingService")
public class GeneralMouldingService extends AbstractProductionBusinessService {

    private final IProductionBusinessService groupPlanProductionService;

    private final IProductionBusinessService continueProductProductionService;

    public GeneralMouldingService(ProductionSchedulingDataService dataService,
                                  @Qualifier("groupPlanMouldingProductionService") IProductionBusinessService groupPlanProductionService,
                                  @Qualifier("continueProductMould") IProductionBusinessService continueProductProductionService) {
        super(dataService);
        this.groupPlanProductionService = groupPlanProductionService;
        this.continueProductProductionService = continueProductProductionService;
    }

    @Override
    public void run(Context context, Object userObj) {
        String logContent = String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划模具排程开始====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        log.info(logContent);
        String productionVersion = context.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            String productionVersionEmpty = I18nUtil.getMessage("alg.data.alter.message.productionVersionEmpty");
            throw new BusinessException(String.format(productionVersionEmpty, context.getYear(), context.getMonth(), context.getMonthPlanVersion()));
        }
        //构建排产基础初始化对象
        ProductionContext productionContext = buildProductionContext(context);
        //排产流程日志记录
        MouldProductionLog generalInitLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.MOULD_INIT, logContent);
        saveProductionLog(productionContext, generalInitLog);
        List<MonthPlanManufacturingRequirementVo> monthPlanInitList = getDataService().getFactoryMonthPlanManufacturing(productionContext);
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            String initEmpty = I18nUtil.getMessage("alg.data.alter.message.initEmpty");
            throw new BusinessException(String.format(initEmpty, context.getYear(), context.getMonth(), context.getMonthPlanVersion()));
        }
        initCache(productionContext);
        //20250519 ZLT 更新排产版本信息--排产模式及开始、结束排产日
        updateProductionVersionInfo(productionContext);
        //更新计划信息
        Map<Long, MonthPlanManufacturingRequirementVo> productionPlanMap = new HashMap<>();
        monthPlanInitList.stream().forEach(productionPlan -> {
            productionPlan.setConstructionStageType(ConstructionStageEnum.getInstance(productionPlan.getConstructionStage()));
            productionPlanMap.put(productionPlan.getMonthPlanId(), productionPlan);
        });
        productionContext.setMonthPlanInitMap(productionPlanMap);
        //20250430 设置规格代号，胚胎代码及成型法
        setProductionPlanInfo(productionContext);
        //排产流程日志记录
        MouldProductionLog startProductionSortGroupLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.GROUP_SORT_LOG, "开始进行排产顺序分组及设置排产顺序值");
        saveProductionLog(productionContext, startProductionSortGroupLog);
        //20251014 ZLT 汇总物料的备货量，用于备货计划的排序
        summaryProductCodeStockUpQty(monthPlanInitList);
        //按排产顺序配置分组及排序--按第一排产顺序进行分组，并对每组按第二排产顺序进行排序
        List<PlanOrderSortConfiguration> sortConfigurationList = getDataService().getProductionConfiguration(productionContext);
        List<ProductionPlanGroupVo> groupData = ProductionPlanUtils.getGroupPlan(sortConfigurationList, productionContext.getFactoryCode(), monthPlanInitList);
        if (CollectionUtils.isEmpty(groupData)) {
            throw new BusinessException("请先配置分厂排产顺序!");
        }
        //20250605 寸口，轮胎类型产能配置初始化
        initCapacityConfiguration(productionContext);
        //20250726 特殊轮胎产能控制
        tireTypeCapacityControl(productionContext, monthPlanInitList);
        //初始化模具产能
        initMouldCapacity(monthPlanInitList, productionContext);
        //20250516 ZLT 共用模具产能预占计算
        calculatedPreemptMouldCapacity(productionContext, monthPlanInitList);
        //20250605 寸口、轮胎类型产能控制
        capacityControl(monthPlanInitList, productionContext);
        //需要推迟到模具产能预占后检查
        regenerateNoProductionRecord(productionContext, monthPlanInitList);
        //20250925 一次法将胎体层级统一调整为多层
        ProductionPlanUtils.handlerOneMethodTireFabricNumber(monthPlanInitList);
        //设置汇总排产量
        summaryProductCodeProductionQty(monthPlanInitList);
        //排产流程日志记录
        MouldProductionLog endProductionSortGroupLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.GROUP_SORT_LOG, "排产顺序分组及设置排产顺序值结束");
        saveProductionLog(productionContext, endProductionSortGroupLog);
        //删除旧有数据
        getDataService().deletedMouldProductionData(productionContext);
        //更新排产顺序
        updateProductionSequence(groupData);
        //20250428 ZLT 续作规格用续作模具排产
        continueProductProductionService.run(productionContext, userObj);
        //开始分组排产
        startGroupProduction(context, userObj, productionContext, groupData, monthPlanInitList);
        log.info("排产结束，进行数据保存.......");
        //排产流程日志记录
        MouldProductionLog endProductionLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.MOULD_INIT, "排产结束，进行结果保存");
        saveProductionLog(productionContext, endProductionLog);
        //保存排产结果信息
        Map<Long, Long> sumProductionMap = saveProductionResult(productionContext);
        //保存未排计划明细
        saveNoProductionPlanResult(productionContext, sumProductionMap);
        //排产流程日志记录
        MouldProductionLog finalLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.MOULD_INIT, "全部结果保存完毕");
        saveProductionLog(productionContext, finalLog);
        log.info("模具重新排产完毕");
        saveLastLogs(productionContext, MouldProductionLogType.MOULD_INIT);
    }

    /**
     * 初始化 物料信息、系统控制参数、模具信息
     * 物料与模具关系信息
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
        //模具信息列表
        initMouldBaseInfo(context);
        //续作规格及模具
        initContinueInfo(context);
        //续作满月排产模式规格
        initContinueFullMonthProductInfo(context);
    }

    /**
     * 初始化检查，并生产不排产记录
     *
     * @param productionContext 排产上下文
     * @param monthPlanInitList 计划
     */
    private void regenerateNoProductionRecord(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> monthPlanInitList) {
        ProductionPlanUtils.initCheck(monthPlanInitList);
        String text = "重新";
        //初始化检查流程日志 重新排产版本：%s，检查完成
        ProductionLogUtils.addInitCheckFinishLog(productionContext, text);
        //先删除不排产记录
        getDataService().deletedNoProductionRecord(productionContext);
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
        //保存不排产记录--清除临时变量
        getDataService().saveNoProductionPlanRecord(productionContext.getNoProductionRecordList());
        productionContext.setNoProductionRecordList(Collections.emptyList());
        //不排产记录结果日志 重新排产版本：%s，共提取不排产计划条数：%d
        ProductionLogUtils.addNoProductionRecordResultLog(productionContext, text, count);
        //初始化完成日志 ===分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划重新排产初始化及检查结束==========
        ProductionLogUtils.addInitFinishLog(productionContext, text);
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
        //开启，按寸口进行提取，由大到小
        String prefixText = "";
        ProductionLogUtils.addProSizeModelStartProductionLog(productionContext, prefixText);
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
        String prefixText = "";
        //按照分组顺序，顺序排产
        groupData.stream().forEach(groupDataList -> {
            //分组排产开始-流程日志记录 开始进行[%s]分组排产
            ProductionLogUtils.addGroupStartProductionLog(productionContext, groupDataList, prefixText);
            //构建分组排产参数信息并排产
            GroupPlanProductionContext groupContext = new GroupPlanProductionContext();
            BeanUtils.copyProperties(context, groupContext);
            groupContext.setProductionContext(productionContext);
            groupContext.setProductionPlanGroup(groupDataList);
            groupPlanProductionService.run(groupContext, userObj);
            //分组排产结束-流程日志记录 [%s]分组排产结束
            ProductionLogUtils.addGroupEndProductionLog(productionContext, groupDataList, prefixText);
        });
    }
}
