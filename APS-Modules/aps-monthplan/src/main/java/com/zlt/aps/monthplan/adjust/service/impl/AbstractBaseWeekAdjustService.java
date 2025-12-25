package com.zlt.aps.monthplan.adjust.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
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
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.maindata.mapper.MdmMonthSurplusEntityMapper;
import com.zlt.aps.maindata.mapper.MpMonthPlanMonitorEntityMapper;
import com.zlt.aps.maindata.mapper.MpTrialPlanEntityMapper;
import com.zlt.aps.monthplan.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.common.utils.DistributedVersionGenerator;
import com.zlt.aps.monthplan.demand.mapper.SalesOrderPoolEntityMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadPoolExecutor;

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
        // 初始化排产版本
        initVersion(contextDTO);
        // 初始化月度生产计划
        initMonthPlan(contextDTO);
        // 获取线程池执行器
        ThreadPoolExecutor executor = ThreadPoolUtil.getThreadPool();

        // 创建初始化方法的异步任务
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

        // 等待所有异步任务完成
        try {
            // 等待所有子任务执行完成
            CompletableFuture.allOf(
                    saleOrderFuture, trialPlanFuture, monthSurplusFuture,
                    productStockFuture, planMonitorFuture
            ).join();

            log.info("初始化任务执行完成 ==> 耗时:{} ms",watch.getLastTaskTimeMillis());

        } catch (CompletionException e) {
            // 异常处理
            Throwable throwable = e.getCause();
            log.error("初始化任务执行失败!", throwable);
            throw new BusinessException(StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.initDataFailure")
                    , throwable.getMessage()), throwable);
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


}
