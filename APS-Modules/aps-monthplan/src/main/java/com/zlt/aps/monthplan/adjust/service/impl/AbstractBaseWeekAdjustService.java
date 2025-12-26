package com.zlt.aps.monthplan.adjust.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Maps;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.ThreadPoolUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.maindata.mapper.MdmMonthSurplusEntityMapper;
import com.zlt.aps.maindata.mapper.MpMonthPlanMonitorEntityMapper;
import com.zlt.aps.maindata.mapper.MpTrialPlanEntityMapper;
import com.zlt.aps.monthplan.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.api.domain.vo.MpAdjustDetailVo;
import com.zlt.aps.monthplan.common.utils.DistributedVersionGenerator;
import com.zlt.aps.monthplan.demand.mapper.SalesOrderPoolEntityMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 周程滚动调整通用抽象类
 * @author wengpc
 */
@Slf4j
public abstract class AbstractBaseWeekAdjustService implements IMpWeekAdjustService {

    @Autowired
    protected FactoryMonthPlanProdFinalMapper factoryMonthPlanProdFinalMapper;

    @Autowired
    protected SalesOrderPoolEntityMapper salesOrderPoolEntityMapper;

    @Autowired
    protected FactoryProductionVersionMapper factoryProductionVersionMapper;

    @Autowired
    protected MpTrialPlanEntityMapper mpTrialPlanEntityMapper;

    @Autowired
    protected MdmMonthSurplusEntityMapper mdmMonthSurplusEntityMapper;

    @Autowired
    protected IMesItfService mesItfService;

    @Autowired
    protected DistributedVersionGenerator versionGenerator;

    @Autowired
    protected MpMonthPlanMonitorEntityMapper mpMonthPlanMonitorEntityMapper;


    @Override
    public void generateAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        // 前置处理
        preProcess(contextDTO);
        // 生成调整明细
        doGenerateAdjust(contextDTO);
        // 后置处理
        postProcess(contextDTO);
    }

    /**
     * 前置处理
     */
    private void preProcess(MpRollAdjustContextDTO contextDTO) {
        // 校验
        check(contextDTO);
        // 并行初始化
        initParallel(contextDTO);
    }

    /**
     * 后置处理
     */
    private void postProcess(MpRollAdjustContextDTO contextDTO) {
    }

    @Override
    public void autoAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {

    }

    @Override
    public void confirmAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {

    }

    /**
     * 生成调整明细(业务逻辑处理)
     */
    public abstract void doGenerateAdjust(MpRollAdjustContextDTO contextDTO);

    /**
     * 自动调整(业务逻辑处理)
     */
    public abstract void doAutoAdjust(MpRollAdjustContextDTO contextDTO);

    /**
     * 调整确认(业务逻辑处理)
     */
    public abstract void doConfirmAdjust(MpRollAdjustContextDTO contextDTO);


    /**
     * 生成分布式唯一版本号
     * @param prefix
     * @return
     */
    protected String generateVersion(String prefix) {
        return versionGenerator.generateVersion(prefix);
    }


    /**
     * 并行初始化
     */
    private void initParallel(MpRollAdjustContextDTO contextDTO) {
        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();

        // 初始化通用
        initCommon(contextDTO);

        // 获取线程池执行器
        ThreadPoolExecutor executor = ThreadPoolUtil.getThreadPool();

        // 创建初始化方法的异步任务
        // 初始化排产版本、初始化月度生产计划 (有依赖关系：先执行initVersion，再执行initMonthPlan)
        CompletableFuture<Void> versionAndMonthPlanFuture = CompletableFuture
                .runAsync(() -> initVersion(contextDTO),executor)
                .thenRunAsync(() -> initMonthPlan(contextDTO),executor);
        // 初始化销售订单池
        CompletableFuture<Void> saleOrderFuture = CompletableFuture.runAsync(() -> initSaleOrderPool(contextDTO),executor);
        // 初始化试制量试计划
        CompletableFuture<Void> trialPlanFuture = CompletableFuture.runAsync(() -> initTrialPlan(contextDTO),executor);
        // 初始化月底计划余量
        CompletableFuture<Void> monthSurplusFuture = CompletableFuture.runAsync(() -> initMonthSurplus(contextDTO),executor);
        // 初始化成品实时库存
        CompletableFuture<Void> productStockFuture = CompletableFuture.runAsync(() -> initProductStock(contextDTO),executor);
        // 初始化月度硫化监控
        CompletableFuture<Void> planMonitorFuture = CompletableFuture.runAsync(() -> initPlanMonitor(contextDTO),executor);

        try {
            // 等待所有异步任务执行完成
            CompletableFuture.allOf(
                    versionAndMonthPlanFuture, saleOrderFuture, trialPlanFuture,
                    monthSurplusFuture,productStockFuture, planMonitorFuture
            ).join();

            log.info("初始化任务执行完成 ==> 耗时:{} ms",watch.getLastTaskTimeMillis());

        } catch (CompletionException e) {
            // 异常处理
            Throwable throwable = e.getCause();
            log.error("初始化任务执行失败! 失败原因:{}", throwable.getMessage(), throwable);
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.initDataFailure"), throwable);
        } finally {
            ThreadPoolUtil.shutdown();
        }
    }


    /**
     * 初始化通用
     * @param contextDTO
     */
    private void initCommon(MpRollAdjustContextDTO contextDTO) {
        // 工厂编码
        contextDTO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        if (contextDTO.getMpYear() != null && contextDTO.getMpMonth() != null) {
            // 年月
            contextDTO.setYearMonth(Integer.valueOf(contextDTO.getMpYear() + "" + contextDTO.getMpMonth()));
        }
    }

    /**
     * 初始化月度硫化监控
     * @param contextDTO
     */
    private void initPlanMonitor(MpRollAdjustContextDTO contextDTO) {
        MpMonthPlanMonitor queryVO = MpMonthPlanMonitor.builder()
                .factoryCode(contextDTO.getFactoryCode())
                .year(contextDTO.getMpYear())
                .month(contextDTO.getMpMonth())
                .build();

        LambdaQueryWrapper<MpMonthPlanMonitor> queryWrapper = new LambdaQueryWrapper<>();
        buildPlanMonitorCondition(queryWrapper,queryVO);
        List<MpMonthPlanMonitor> planMonitorList = mpMonthPlanMonitorEntityMapper.selectList(queryWrapper);
        contextDTO.setMpMonthPlanMonitorList(planMonitorList);
    }

    /**
     * 构建月度硫化监控条件
     * @param queryWrapper
     * @param queryVO
     */
    private void buildPlanMonitorCondition(LambdaQueryWrapper<MpMonthPlanMonitor> queryWrapper, MpMonthPlanMonitor queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()),MpMonthPlanMonitor::getFactoryCode,queryVO.getFactoryCode());
        queryWrapper.eq(MpMonthPlanMonitor::getYear, queryVO.getYear());
        queryWrapper.eq(MpMonthPlanMonitor::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpMonthPlanMonitor::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 初始化成品实时库存
     * @param contextDTO
     */
    private void initProductStock(MpRollAdjustContextDTO contextDTO) {
        MdmProductStock queryVO = new MdmProductStock();
        Map<String,Object> param = Maps.newHashMap();
        param.put("factoryCode",contextDTO.getFactoryCode());
        param.put("stockDate", new Date());
        // 传空查询所有
        param.put("materialCodeList",null);
        queryVO.setParams(param);

        // 调用接口查询实时成品库存
        List<MdmProductStock> mdmProductStockList = mesItfService.getProductStock(queryVO);
        contextDTO.setMdmProductStockList(mdmProductStockList);
    }


    /**
     * 初始化月底计划余量
     * @param contextDTO
     */
    private void initMonthSurplus(MpRollAdjustContextDTO contextDTO) {
        MdmMonthSurplus queryVO = new MdmMonthSurplus();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYear(contextDTO.getMpYear());
        queryVO.setMonth(contextDTO.getMpMonth());

        LambdaQueryWrapper<MdmMonthSurplus> queryWrapper = new LambdaQueryWrapper<>();
        buildMonthSurplusCondition(queryWrapper,queryVO);
        List<MdmMonthSurplus> mdmMonthSurplusesList = mdmMonthSurplusEntityMapper.selectList(queryWrapper);
        contextDTO.setMdmMonthSurplusesList(mdmMonthSurplusesList);
    }

    /**
     * 构建月底计划余量条件
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMonthSurplusCondition(LambdaQueryWrapper<MdmMonthSurplus> queryWrapper, MdmMonthSurplus queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()),MdmMonthSurplus::getFactoryCode,queryVO.getFactoryCode());
        queryWrapper.eq(MdmMonthSurplus::getYear, queryVO.getYear());
        queryWrapper.eq(MdmMonthSurplus::getMonth, queryVO.getMonth());
        queryWrapper.eq(MdmMonthSurplus::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 初始化试制量试计划
     * @param contextDTO
     */
    private void initTrialPlan(MpRollAdjustContextDTO contextDTO) {
        MpTrialPlan queryVO = new MpTrialPlan();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYear(contextDTO.getMpYear());
        queryVO.setMonth(contextDTO.getMpMonth());

        LambdaQueryWrapper<MpTrialPlan> queryWrapper = new LambdaQueryWrapper<>();
        buildTrialPlanCondition(queryWrapper,queryVO);
        List<MpTrialPlan> mpTrialPlanList = mpTrialPlanEntityMapper.selectList(queryWrapper);
        contextDTO.setMpTrialPlanList(mpTrialPlanList);
    }

    /**
     * 构建试制量试计划条件
     * @param queryWrapper
     * @param queryVO
     */
    private void buildTrialPlanCondition(LambdaQueryWrapper<MpTrialPlan> queryWrapper, MpTrialPlan queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()),MpTrialPlan::getFactoryCode,queryVO.getFactoryCode());
        queryWrapper.eq(MpTrialPlan::getYear, queryVO.getYear());
        queryWrapper.eq(MpTrialPlan::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpTrialPlan::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 初始化销售订单池
     * @param contextDTO
     */
    private void initSaleOrderPool(MpRollAdjustContextDTO contextDTO) {
        SalesOrderPool queryVO = new SalesOrderPool();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        // 订单状态，0-关单，1-正常
        queryVO.setOrderStatus(ApsConstant.TRUE);

        LambdaQueryWrapper<SalesOrderPool> queryWrapper = new LambdaQueryWrapper<>();
        buildSaleOrderPoolCondition(queryWrapper,queryVO);
        List<SalesOrderPool> salesOrderPoolList = salesOrderPoolEntityMapper.selectList(queryWrapper);
        // 排除暂缓订单
        CollUtil.filter(salesOrderPoolList, pool -> !"5".equals(pool.getOrderPriority()));
        contextDTO.setSalesOrderPoolList(salesOrderPoolList);
    }


    /**
     * 构建销售订单池条件
     * @param queryWrapper
     * @param queryVO
     */
    private void buildSaleOrderPoolCondition(LambdaQueryWrapper<SalesOrderPool> queryWrapper, SalesOrderPool queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()),SalesOrderPool::getFactoryCode,queryVO.getFactoryCode());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getOrderStatus()),SalesOrderPool::getOrderStatus,queryVO.getOrderStatus());
        queryWrapper.eq(SalesOrderPool::getIsDelete, YesOrNoEnum.NO.getValue());
    }



    /**
     * 初始化排产版本
     * @param contextDTO
     */
    private void initVersion(MpRollAdjustContextDTO contextDTO) {
        // 查询排产版本
        FactoryProductionVersion version = new FactoryProductionVersion();
        version.setFactoryCode(contextDTO.getFactoryCode());
        // todo 暂时写死 01 正常
        version.setPlanType("01");
        version.setYear(contextDTO.getMpYear());
        version.setMonth(contextDTO.getMpMonth());

        LambdaQueryWrapper<FactoryProductionVersion> wrapper = new LambdaQueryWrapper<>();
        buildVersionCondition(wrapper, version);
        List<FactoryProductionVersion> versionList = factoryProductionVersionMapper.selectList(wrapper);
        contextDTO.setFactoryProductionVersionList(versionList);
    }

    /**
     * 初始化月度生产计划
     * @param contextDTO
     */
    private void initMonthPlan(MpRollAdjustContextDTO contextDTO) {
        // 查询月度生产计划
        FactoryProductionVersion factoryProductionVersion = getIsFinalVersion(contextDTO);
        FactoryMonthPlanProdFinal queryVO = new FactoryMonthPlanProdFinal();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYearMonth(contextDTO.getYearMonth());
        queryVO.setMonthPlanVersion(factoryProductionVersion == null ? null : factoryProductionVersion.getMonthPlanVersion());

        LambdaQueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new LambdaQueryWrapper<>();
        buildMonthPlanCondition(queryWrapper,queryVO);
        List<FactoryMonthPlanProdFinal> factoryMonthPlanProdFinalList = factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
        List<FactoryMonthPlanFinalAdjustVo> resultList = BeanUtil.copyToList(factoryMonthPlanProdFinalList,FactoryMonthPlanFinalAdjustVo.class);
        contextDTO.setFactoryMonthPlanProdFinalList(resultList);
    }


    /**
     * 构建排产版本条件
     * @param queryWrapper
     * @param queryVO
     */
    private void buildVersionCondition(LambdaQueryWrapper<FactoryProductionVersion> queryWrapper, FactoryProductionVersion queryVO) {
        queryWrapper.eq(FactoryProductionVersion::getFactoryCode,queryVO.getFactoryCode());
        queryWrapper.eq(FactoryProductionVersion::getYear,queryVO.getYear());
        queryWrapper.eq(FactoryProductionVersion::getMonth,queryVO.getMonth());
        queryWrapper.eq(FactoryProductionVersion::getPlanType,queryVO.getPlanType());
        queryWrapper.eq(queryVO.getIsFinal() != null,FactoryProductionVersion::getIsFinal,queryVO.getIsFinal());
    }


    /**
     * 构建月度生产计划条件
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMonthPlanCondition(LambdaQueryWrapper<FactoryMonthPlanProdFinal> queryWrapper, FactoryMonthPlanProdFinal queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()),FactoryMonthPlanProdFinal::getFactoryCode,queryVO.getFactoryCode());
        queryWrapper.eq(queryVO.getYearMonth() != null,FactoryMonthPlanProdFinal::getYearMonth,queryVO.getYearMonth());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getMonthPlanVersion()),FactoryMonthPlanProdFinal::getMonthPlanVersion,queryVO.getMonthPlanVersion());
        queryWrapper.eq(FactoryMonthPlanProdFinal::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 校验
     * @param contextDTO
     */
    private void check(MpRollAdjustContextDTO contextDTO) {
        // 初始化通用
        initCommon(contextDTO);
        // 校验年月是否为空
        Assert.isFalse(contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null,I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.yearMonthEmpty"));
        // 获取定稿的排产版本
        FactoryProductionVersion factoryProductionVersion = getIsFinalVersion(contextDTO);
        // 月度生产计划还未定稿，抛出异常
        Assert.isFalse(factoryProductionVersion == null, () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFinalMonthPlan"),
                    contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
    }

    /**
     * 获取定稿的排产版本
     * @param contextDTO
     * @return
     */
    private FactoryProductionVersion getIsFinalVersion(MpRollAdjustContextDTO contextDTO) {
        // 初始化排产版本
        initVersion(contextDTO);

        List<FactoryProductionVersion> sourceVersionList = contextDTO.getFactoryProductionVersionList();
        if (PubUtil.isEmpty(sourceVersionList)) {
            return null;
        }
        // 筛选：定稿的排产版本
        FactoryProductionVersion factoryProductionVersion = sourceVersionList.stream()
                .filter(item -> Constant.TRUE.equals(item.getIsFinal()))
                .findFirst()
                .orElse(null);
        return factoryProductionVersion;
    }


    /**
     * 构建调整明细
     * @param contextDTO
     * @return
     */
    protected List<MpAdjustDetailVo> buildAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        // 销售订单池列表
        List<SalesOrderPool> salesOrderPoolList = contextDTO.getSalesOrderPoolList();
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 结果集初始化
        List<MpAdjustDetailVo> resultList = new ArrayList<>();
        // 任一列表为空则直接返回空结果
        if (PubUtil.isEmpty(salesOrderPoolList) || PubUtil.isEmpty(monthPlanProdList)) {
            return resultList;
        }
        // 获取版本号
        String version = generateVersion(BusiConstant.WeekRollAdjust.VERSION_PREFIX);
        // 按物料编码分组，合并同分组下的成型机编码（逗号分隔）
        List<FactoryMonthPlanFinalAdjustVo> mergeMonthPlanProdList = mergeMonthPlanProdList(monthPlanProdList);
        // 生产计划列表按照物料编码进行分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap = mergeMonthPlanProdList.stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
        // 遍历销售订单列表，匹配生产计划
        for (SalesOrderPool salesOrder : salesOrderPoolList) {
            String materialCode = salesOrder.getOriMaterialCode();
            // 物料编码为空则跳过
            if (com.ruoyi.common.utils.StringUtils.isEmpty(materialCode)) {
                continue;
            }
            // 根据物料编码获取对应的生产计划列表
            List<FactoryMonthPlanFinalAdjustVo> matchMonthPlanProdList = monthPlanMap.get(materialCode);
            if (PubUtil.isEmpty(matchMonthPlanProdList)) {
                // 匹配不到时跳过
                continue;
            }
            // 组装结果集
            for (FactoryMonthPlanFinalAdjustVo monthPlan : matchMonthPlanProdList) {
                MpAdjustDetailVo adjustStructureIn = new MpAdjustDetailVo();
                adjustStructureIn.setMaterialCode(materialCode);
                adjustStructureIn.setScheduledMachines(monthPlan.getCxMachineCode());
                // todo 暂时写死，后续获取
                adjustStructureIn.setHasSpecialMaterial("0");
                adjustStructureIn.setYear(contextDTO.getMpYear());
                adjustStructureIn.setMonth(contextDTO.getMpMonth());
                adjustStructureIn.setVersion(version);
                adjustStructureIn.setStructureName(monthPlan.getStructureName());
                adjustStructureIn.setMaterialDesc(monthPlan.getMaterialDesc());
                // todo 暂时写死，后续获取
                adjustStructureIn.setPreviousNetQty(0);
                // 添加到结果集
                resultList.add(adjustStructureIn);
            }
        }
        return resultList;
    }

    /**
     * 按物料编码分组，合并同分组下的成型机编码（逗号分隔）
     * @param originalList
     * @return 合并后结果集
     */
    private List<FactoryMonthPlanFinalAdjustVo> mergeMonthPlanProdList(List<FactoryMonthPlanFinalAdjustVo> originalList) {
        // 结果集初始化
        List<FactoryMonthPlanFinalAdjustVo> mergedList = new ArrayList<>();
        // 原始列表为空直接返回空结果
        if (PubUtil.isEmpty(originalList)) {
            return mergedList;
        }
        // 按物料编码分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanGroupMap = originalList.stream()
                .filter(vo -> com.ruoyi.common.utils.StringUtils.isNotBlank(vo.getMaterialCode()))
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
        // 遍历分组，合并成型机编码
        monthPlanGroupMap.forEach((materialCode, list) -> {
            // 收集并合并成型机编码（多个逗号分隔）
            String mergedCxMachineCode = list.stream()
                    .map(FactoryMonthPlanFinalAdjustVo::getCxMachineCode)
                    .filter(com.ruoyi.common.utils.StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining(","));
            // 构建合并后的月度生产计划
            FactoryMonthPlanFinalAdjustVo mergedVo = new FactoryMonthPlanFinalAdjustVo();
            FactoryMonthPlanFinalAdjustVo firstVo = list.get(0);
            BeanUtil.copyProperties(firstVo,mergedVo,false);
            mergedVo.setMaterialCode(materialCode);
            mergedVo.setCxMachineCode(mergedCxMachineCode);
            // 添加到结果集
            mergedList.add(mergedVo);
        });
        return mergedList;
    }


    /**
     * 设置净需求
     * 净需求 = 销售订单池.当前订单量 - 实时库存 - 月底计划余量
     * @param contextDTO
     */
    protected void setCurrentNetQty(MpRollAdjustContextDTO contextDTO) {
        // 月底计划余量列表
        List<MdmMonthSurplus> surplusList = contextDTO.getMdmMonthSurplusesList();
        // 实时成品库存列表
        List<MdmProductStock> stockList = contextDTO.getMdmProductStockList();
        // 结构内调整记录
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 将列表转为Map
        Map<String, MdmMonthSurplus> surplusMap = convertToSurplusMap(surplusList);
        Map<String, MdmProductStock> stockMap = convertToStockMap(stockList);

        // 遍历计算
        for (MpAdjustDetailVo adjust : adjustList) {
            if (com.ruoyi.common.utils.StringUtils.isEmpty(adjust.getMaterialCode())) {
                continue;
            }
            String materialCode = adjust.getMaterialCode();
            Integer ordQty = Convert.toInt(adjust.getOrdQty(),0);
            Integer planSurplusQty = MapUtil.getInt(surplusMap,materialCode,0);
            Integer stockQty = MapUtil.getInt(stockMap,materialCode,0);
            // 计算赋值 净需求 = 销售订单池.当前订单量 - 实时库存 - 月底计划余量
            Integer currentNetQty = ordQty - planSurplusQty - stockQty;
            adjust.setCurrentNetQty(currentNetQty);
        }

    }


    /**
     * 将MdmMonthSurplus转Map
     */
    private Map<String, MdmMonthSurplus> convertToSurplusMap(List<MdmMonthSurplus> surplusList) {
        if (PubUtil.isEmpty(surplusList)) {
            return Collections.emptyMap();
        }
        return surplusList.stream()
                .filter(surplus -> com.ruoyi.common.utils.StringUtils.isNotEmpty(surplus.getMaterialCode()))
                .collect(Collectors.toMap(
                        MdmMonthSurplus::getMaterialCode,
                        surplus -> surplus,
                        (existingVal, newVal) -> newVal
                ));
    }


    /**
     * 将MdmProductStock转Map
     */
    private Map<String, MdmProductStock> convertToStockMap(List<MdmProductStock> stockList) {
        if (stockList == null || stockList.isEmpty()) {
            return Collections.emptyMap();
        }
        return stockList.stream()
                .filter(stock -> stock != null && stock.getMaterialCode() != null)
                .collect(Collectors.toMap(
                        MdmProductStock::getMaterialCode,
                        stock -> stock,
                        (existingVal, newVal) -> newVal
                ));
    }

    /**
     * 设置计划剩余排产量
     * 计划剩余排产量 =【 1日 至 （调整日+锁定3天）】.计划量 - 已生产量，出现负数，默认等于0
     * @param contextDTO
     */
    protected void setMonthUnScheduledQty(MpRollAdjustContextDTO contextDTO) {
        // 月度硫化监控列表
        List<MpMonthPlanMonitor> monitorList = contextDTO.getMpMonthPlanMonitorList();
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> planList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 结构内调整记录
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 转分组Map
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> planGroupMap = convertToPlanGroupMap(planList);
        Map<String, List<MpMonthPlanMonitor>> monitorGroupMap = convertToMonitorGroupMap(monitorList);
        // 获取当前日期 + 锁定3天的日期，计算目标天数（如：5号+3天=7号，包含当天）
        LocalDate currentDate = LocalDate.now();
        LocalDate targetDate = currentDate.plus(BusiConstant.WeekRollAdjust.LOCK_DAYS, ChronoUnit.DAYS).minusDays(1);
        int targetDay = targetDate.getDayOfMonth();
        // 目标天数不超过当月最大天数
        int maxDayOfMonth = currentDate.lengthOfMonth();
        targetDay = Math.min(targetDay, maxDayOfMonth);
        // 遍历目标列表，计算赋值
        for (MpAdjustDetailVo adjust : adjustList) {
            if (com.ruoyi.common.utils.StringUtils.isEmpty(adjust.getMaterialCode())) {
                adjust.setMonthUnScheduledQty(0);
                continue;
            }
            String materialCode = adjust.getMaterialCode();
            // 计算：day1~targetDay的累计值
            Integer totalScheduledQty = calculateQty(planGroupMap, materialCode, targetDay);
            // 获取已生产量（空值按0处理）
            Integer productionQty = MapUtil.getInt(monitorGroupMap,materialCode,0);
            // 计划已排产量
            adjust.setMonthScheduledQty(totalScheduledQty);
            // 计划剩余排产量 = 累计已排产量 - 已生产量
            Integer monthUnScheduledQty = totalScheduledQty - productionQty;
            // 计划剩余排产量为负数时，默认为0
            if (monthUnScheduledQty < 0) {
                monthUnScheduledQty = 0;
            }
            adjust.setMonthUnScheduledQty(monthUnScheduledQty);
        }

    }

    /**
     * 计算day1~targetDay的累计已排产量
     */
    private Integer calculateQty(Map<String, List<FactoryMonthPlanFinalAdjustVo>> planGroupMap, String materialCode, int targetDay) {
        // 从分组Map中获取当前物料的计划列表（空则返回0）
        List<FactoryMonthPlanFinalAdjustVo> planList = Optional.ofNullable(planGroupMap.get(materialCode))
                .filter(list -> PubUtil.isNotEmpty(list))
                .orElse(Collections.emptyList());
        if (PubUtil.isEmpty(planList)) {
            return 0;
        }
        // 取第一个计划对象
        FactoryMonthPlanFinalAdjustVo plan = planList.get(0);
        int total = 0;
        // 遍历day1~targetDay字段，累加值
        for (int day = 1; day <= targetDay; day++) {
            try {
                // 拼接字段名
                String fieldName = "day" + day;
                // 获取字段值，空值按0处理
                Integer dayValue = (Integer) plan.getFieldValueByFieldName(fieldName);
                total += Convert.toInt(dayValue);
            } catch (Exception e) {
                // 异常时跳过
                continue;
            }
        }
        return total;
    }

    /**
     * 设置其他字段
     * @param contextDTO
     */
    protected void setOtherField(MpRollAdjustContextDTO contextDTO) {
        // 调整明细
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 循环设置
        adjustList.stream().forEach(vo -> {
            // 计算: 调整量 = 净需求 - 计划剩余排产量
            Integer pendingQty = vo.getCurrentNetQty() - vo.getMonthUnScheduledQty();
            vo.setPendingQty(Convert.toInt(pendingQty,0));
            // 计算：净需求变动 = 净需求 - 调整前净需求量
            Integer netQtyChange = vo.getCurrentNetQty() - vo.getPreviousNetQty();
            vo.setNetQtyChange(Convert.toInt(netQtyChange,0));
        });
    }


    /**
     * 转FactoryMonthPlanFinalAdjustVo分组Map
     */
    private Map<String, List<FactoryMonthPlanFinalAdjustVo>> convertToPlanGroupMap(List<FactoryMonthPlanFinalAdjustVo> planList) {
        if (PubUtil.isEmpty(planList)) {
            return Collections.emptyMap();
        }
        return planList.stream()
                .filter(plan -> plan != null && plan.getMaterialCode() != null)
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
    }

    /**
     * 转MpMonthPlanMonitor分组Map
     */
    private Map<String, List<MpMonthPlanMonitor>> convertToMonitorGroupMap(List<MpMonthPlanMonitor> monitorList) {
        if (PubUtil.isEmpty(monitorList)) {
            return Collections.emptyMap();
        }
        return monitorList.stream()
                .filter(monitor -> monitor != null && monitor.getMaterialCode() != null)
                .collect(Collectors.groupingBy(MpMonthPlanMonitor::getMaterialCode));
    }


}
