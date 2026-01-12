package com.zlt.aps.monthplan.adjust.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Maps;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
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
import com.zlt.aps.monthplan.adjust.engine.MpWeekRollAdjustEngine;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustResultEntityMapper;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustStructureInEntityMapper;
import com.zlt.aps.monthplan.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustResult;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.aps.monthplan.api.domain.entity.MpTrialPlan;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.api.domain.vo.MpAdjustDetailVo;
import com.zlt.aps.monthplan.common.utils.DistributedVersionGenerator;
import com.zlt.aps.monthplan.demand.mapper.SalesOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 周程滚动调整通用抽象类
 *
 * @author wengpc
 */
@Slf4j
public abstract class AbstractBaseWeekAdjustService implements IMpWeekAdjustService {

    @Autowired
    protected FactoryMonthPlanProdFinalMapper factoryMonthPlanProdFinalMapper;

    @Autowired
    protected SalesOrderPoolEntityMapper salesOrderPoolEntityMapper;

    @Autowired
    protected MpFactoryProductionVersionMapper factoryProductionVersionMapper;

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

    @Autowired
    protected MpAdjustResultEntityMapper mpAdjustResultEntityMapper;

    @Autowired
    protected MpAdjustStructureInEntityMapper mpAdjustStructureInEntityMapper;

    @Autowired
    protected IDpDemandPlanService dpDemandPlanService;

    @Autowired
    protected BaseDao baseDao;


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
        // 排序调整明细
        sortAdjustDetailList(contextDTO);
        // 保存调整明细
        saveAdjustDetailList(contextDTO);
    }

    /**
     * 保存调整明细
     * @param contextDTO
     */
    public abstract void saveAdjustDetailList(MpRollAdjustContextDTO contextDTO);

    protected void sortAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        if (PubUtil.isEmpty(adjustDetailList)) {
            return;
        }
        Collections.sort(adjustDetailList, getSortComparator());
    }

    protected Comparator<MpAdjustDetailVo> getSortComparator() {
        return Comparator
                .comparing(MpAdjustDetailVo::getStructureName,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MpAdjustDetailVo::getPendingQty,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MpAdjustDetailVo::getMaterialCode,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    @Override
    public void autoAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        MpWeekRollAdjustEngine adjustEngine = new MpWeekRollAdjustEngine();
        adjustEngine.structureInAutoAdjust(contextDTO);
        //保存调整结果
        saveMpAdjustResult(contextDTO);
    }

    /**
     * 保存调整结果
     * @param contextDTO
     */
    private void saveMpAdjustResult(MpRollAdjustContextDTO contextDTO){
        List<FactoryMonthPlanFinalAdjustVo> factoryMonthPlanProdFinalList = contextDTO.getFactoryMonthPlanProdFinalList();
        if (PubUtil.isEmpty(factoryMonthPlanProdFinalList)){
            return;
        }
        //1、根据调整版本 先删除
        LambdaQueryWrapper<MpAdjustResult> adjustResultWrapper = new LambdaQueryWrapper<>();
        adjustResultWrapper.eq(MpAdjustResult::getFactoryCode, contextDTO.getFactoryCode());
        adjustResultWrapper.eq(MpAdjustResult::getYear, contextDTO.getMpYear());
        adjustResultWrapper.eq(MpAdjustResult::getMonth, contextDTO.getMpMonth());
        adjustResultWrapper.eq(MpAdjustResult::getVersion, contextDTO.getVersion());
        mpAdjustResultEntityMapper.delete(adjustResultWrapper);

        //2、保存调整记录
        MpAdjustResult mpAdjustResult;
        List<MpAdjustResult> mpAdjustResultList = new ArrayList<>();
        for (FactoryMonthPlanFinalAdjustVo finalAdjustVo:factoryMonthPlanProdFinalList){
            mpAdjustResult = new MpAdjustResult();
            BeanUtils.copyProperties(finalAdjustVo,mpAdjustResult);
            mpAdjustResult.setAdjustType("01");
            mpAdjustResult.setVersion(contextDTO.getVersion());
            mpAdjustResultList.add(mpAdjustResult);
        }
        //baseDao.insertBatch(mpAdjustResultList);
    }

    @Override
    public void confirmAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        log.info("开始执行周程调整确认流程，年份：{}，月份：{}，版本：{}",
                contextDTO.getMpYear(), contextDTO.getMpMonth(), contextDTO.getVersion());
        try {
            // 1、保存周程调整结果
            saveAdjustResult(contextDTO);
            // 2、查询月度生产计划
            queryMonthPlanList(contextDTO);
            // 3、更新月度生产计划
            updateMonthPlanList(contextDTO);
            // 4、查询调整明细
            queryAdjustDetailList(contextDTO);
            // 5、更新调整明细
            updateAdjustDetailList(contextDTO);
            // 6、记录调整操作日志 TODO
            log.info("周程调整确认流程执行完成");
        } catch (Exception e) {
            log.error("周程调整确认流程执行异常", e);
            throw new BusinessException("周程调整确认失败：" + e.getMessage());
        }
    }


    /**
     * 更新调整明细
     * 将本次调整的量，回填到"调整明细".实际调整；置换过程回填到“调整明细".调整原因
     *
     * @param contextDTO
     */
    private void updateAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        if (PubUtil.isEmpty(adjustResultList) || PubUtil.isEmpty(adjustDetailList)) {
            log.warn("更新调整明细：调整结果列表或调整明细列表为空，直接返回");
            return;
        }
        // 调整结果按照物料编号分组
        Map<String, List<MpAdjustResult>> adjustDetailMap = buildMaterialCodeAdjustMap(adjustResultList);
        // 遍历调整明细列表匹配调整结果(更新实际调整、调整原因)
        for (MpAdjustDetailVo adjustDetailVo : adjustDetailList) {
            String materialCode = adjustDetailVo.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            MpAdjustResult adjustResult = getFirstAdjustResult(adjustDetailMap, materialCode);
            if (adjustResult == null) {
                log.warn("更新调整明细：物料编号:{}未查询到对应调整结果，跳过", materialCode);
                continue;
            }
            // 实际调整
            adjustDetailVo.setActualAdjustQty(adjustResult.getTotalPlanQty());
            // 调整原因 TODO
            adjustDetailVo.setAdjustReason("");
        }
        // 更新调整明细
        try {
            baseDao.updateBatch(adjustDetailList);
            log.info("更新调整明细成功，共更新:{}条记录", adjustDetailList.size());
        } catch (Exception e) {
            log.error("更新调整明细批量操作异常", e);
            throw new RuntimeException("更新调整明细失败", e);
        }
    }


    /**
     * 查询调整明细
     *
     * @param contextDTO
     */
    private void queryAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        if (contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null) {
            log.warn("查询调整明细：年份或者月份为空，直接返回");
            return;
        }
        // 年份
        Integer year = contextDTO.getMpYear();
        // 月份
        Integer month = contextDTO.getMpMonth();
        // 调整版本号
        String version = contextDTO.getVersion();

        MpAdjustDetailVo queryVO = new MpAdjustDetailVo();
        queryVO.setYear(year);
        queryVO.setMonth(month);
        queryVO.setVersion(version);

        LambdaQueryWrapper<MpAdjustStructureIn> queryWrapper = new LambdaQueryWrapper<>();
        buildAdjustDetailCondition(queryWrapper, queryVO);

        try {
            List<MpAdjustStructureIn> adjustStructureInList = mpAdjustStructureInEntityMapper.selectList(queryWrapper);
            List<MpAdjustDetailVo> adjustDetailList = BeanUtil.copyToList(adjustStructureInList, MpAdjustDetailVo.class);
            contextDTO.setAdjustDetailList(adjustDetailList);
            log.info("查询调整明细成功，年份：{}，月份：{}，版本：{}，共查询:{}条记录",
                    year, month, version, adjustDetailList.size());
        } catch (Exception e) {
            log.error("查询调整明细异常，年份：{}，月份：{}，版本：{}", year, month, version, e);
            throw new RuntimeException("查询调整明细失败", e);
        }
    }


    /**
     * 更新月度生产计划
     * 更新月度生产计划.1日至31日计划量，并重算开始日期和结束日期
     * 根据周次，将本次调整量合并到对应周次的月度生产计划.调整量
     *
     * @param contextDTO
     */
    private void updateMonthPlanList(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        List<FactoryMonthPlanFinalAdjustVo> factoryMonthPlanProdFinalList = contextDTO.getFactoryMonthPlanProdFinalList();
        if (PubUtil.isEmpty(adjustResultList) || PubUtil.isEmpty(factoryMonthPlanProdFinalList)) {
            log.warn("更新月度生产计划：调整结果列表或月度计划列表为空，直接返回");
            return;
        }
        // 调整结果按照物料编号分组
        Map<String, List<MpAdjustResult>> adjustDetailMap = buildMaterialCodeAdjustMap(adjustResultList);
        // 遍历生产计划列表匹配调整结果（更新计划量、开始日期、结束日期、调整量)
        for (FactoryMonthPlanFinalAdjustVo monthPlanVo : factoryMonthPlanProdFinalList) {
            String materialCode = monthPlanVo.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            MpAdjustResult adjustResult = getFirstAdjustResult(adjustDetailMap, materialCode);
            if (adjustResult == null) {
                log.warn("更新月度生产计划：物料编号:{}未查询到对应调整结果，跳过", materialCode);
                continue;
            }
            // 更新1日至31日计划量
            for (int i = 1; i <= BusiConstant.WeekRollAdjust.MAX_DAY_OF_MONTH; i++) {
                String dayFieldName = BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + i;
                monthPlanVo.setFieldValueByFieldName(dayFieldName, adjustResult.getFieldValueByFieldName(dayFieldName));
            }
            // 重算开始日期和结束日期
            if (adjustResult.getStartDate() != null) {
                try {
                    String startDateStr = DateUtil.format(adjustResult.getStartDate(), BusiConstant.WeekRollAdjust.DATE_FORMAT_YYYYMMDD);
                    monthPlanVo.setBeginDay(Integer.valueOf(startDateStr));
                } catch (Exception e) {
                    log.error("更新月度生产计划：物料:{}的开始日期转换失败，跳过", materialCode, e);
                }
            }
            if (adjustResult.getEndDate() != null) {
                try {
                    String endDateStr = DateUtil.format(adjustResult.getEndDate(), BusiConstant.WeekRollAdjust.DATE_FORMAT_YYYYMMDD);
                    monthPlanVo.setEndDay(Integer.valueOf(endDateStr));
                } catch (Exception e) {
                    log.error("更新月度生产计划：物料:{}的结束日期转换失败，跳过", materialCode, e);
                }
            }
            // 获取业务数据对应的周数
            int week = 0;
            if (adjustResult.getStartDate() != null) {
                week = DateUtil.weekOfMonth(adjustResult.getStartDate());
            } else {
                week = DateUtil.weekOfMonth(new Date());
            }
            monthPlanVo.setFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_ADJUST_QTY + week, adjustResult.getTotalPlanQty());
        }
        // 更新月度生产计划
        try {
            baseDao.updateBatch(factoryMonthPlanProdFinalList);
            log.info("更新月度生产计划成功，共更新:{}条记录", factoryMonthPlanProdFinalList.size());
        } catch (Exception e) {
            log.error("更新月度生产计划批量操作异常", e);
            throw new RuntimeException("更新月度生产计划失败", e);
        }

    }

    /**
     * 保存周程调整结果
     *
     * @param contextDTO
     */
    private void saveAdjustResult(MpRollAdjustContextDTO contextDTO) {
        // 调整结果
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        if (PubUtil.isEmpty(adjustResultList)) {
            log.warn("保存周程调整结果：调整结果列表为空，直接返回");
            return;
        }
        // 保存调整结果
        try {
            baseDao.saveBatch(adjustResultList);
            log.info("保存周程调整结果成功，共保存:{}条记录", adjustResultList.size());
        } catch (Exception e) {
            log.error("保存周程调整结果批量操作异常", e);
            throw new RuntimeException("保存周程调整结果失败", e);
        }
    }

    /**
     * 查询月度生产计划
     *
     * @param contextDTO
     */
    private void queryMonthPlanList(MpRollAdjustContextDTO contextDTO) {
        if (contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null) {
            log.warn("查询月度生产计划：年份或者月份为空，直接返回");
            return;
        }
        // 年份
        Integer year = contextDTO.getMpYear();
        // 月份
        Integer month = contextDTO.getMpMonth();
        // 月度计划版本
        String monthPlanVersion = contextDTO.getProductionVersion();

        FactoryMonthPlanProdFinal queryVO = new FactoryMonthPlanProdFinal();
        queryVO.setYear(year);
        queryVO.setMonth(month);
        queryVO.setMonthPlanVersion(monthPlanVersion);

        LambdaQueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new LambdaQueryWrapper<>();
        buildMonthPlanCondition(queryWrapper, queryVO);

        try {
            List<FactoryMonthPlanProdFinal> factoryMonthPlanProdFinalList = factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
            List<FactoryMonthPlanFinalAdjustVo> resultList = BeanUtil.copyToList(factoryMonthPlanProdFinalList, FactoryMonthPlanFinalAdjustVo.class);
            contextDTO.setFactoryMonthPlanProdFinalList(resultList);
        } catch (Exception e) {
            log.error("查询月度生产计划异常，年份：{}，月份：{}，版本：{}", year, month, monthPlanVersion, e);
            throw new RuntimeException("查询月度生产计划失败", e);
        }
    }

    /**
     * 构建调整结果分组Map
     *
     * @param adjustResultList
     * @return
     */
    private Map<String, List<MpAdjustResult>> buildMaterialCodeAdjustMap(List<MpAdjustResult> adjustResultList) {
        if (PubUtil.isEmpty(adjustResultList)) {
            return Collections.emptyMap();
        }
        return adjustResultList.stream()
                .filter(result -> StringUtils.isNotEmpty(result.getMaterialCode()))
                .collect(Collectors.groupingBy(MpAdjustResult::getMaterialCode));
    }

    /**
     * 获取第一个调整结果
     *
     * @param materialCodeAdjustMap
     * @param materialCode
     * @return
     */
    private MpAdjustResult getFirstAdjustResult(Map<String, List<MpAdjustResult>> materialCodeAdjustMap, String materialCode) {
        if (materialCodeAdjustMap == null || StringUtils.isEmpty(materialCode)) {
            return null;
        }
        List<MpAdjustResult> resultList = materialCodeAdjustMap.get(materialCode);
        if (PubUtil.isEmpty(resultList)) {
            return null;
        }
        return resultList.get(0);
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
     *
     * @param prefix
     * @return
     */
    protected String generateVersion(String prefix) {
        return versionGenerator.generateVersion(prefix);
    }

    /**
     * 设置分布式唯一版本号
     *
     * @param contextDTO
     * @param prefix
     * @return
     */
    protected void setVersion(MpRollAdjustContextDTO contextDTO, String prefix) {
        contextDTO.setVersion(generateVersion(prefix));
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
                .runAsync(() -> initVersion(contextDTO), executor)
                .thenRunAsync(() -> initMonthPlan(contextDTO), executor);
        // 初始化销售订单池
        CompletableFuture<Void> saleOrderFuture = CompletableFuture.runAsync(() -> initSaleOrderPool(contextDTO), executor);
        // 初始化试制量试计划
        CompletableFuture<Void> trialPlanFuture = CompletableFuture.runAsync(() -> initTrialPlan(contextDTO), executor);
//        // 初始化月底计划余量
//        CompletableFuture<Void> monthSurplusFuture = CompletableFuture.runAsync(() -> initMonthSurplus(contextDTO), executor);
        // 初始化月度硫化监控
        CompletableFuture<Void> planMonitorFuture = CompletableFuture.runAsync(() -> initPlanMonitor(contextDTO), executor);
//        // 初始化成品实时库存
//        CompletableFuture<Void> productStockFuture = CompletableFuture.runAsync(
//                // 解决父子上下文传递问题
//                SpringContextSupplierUtil.wrap(() -> initProductStock(contextDTO)),
//                executor
//        );


        try {
            // 等待所有异步任务执行完成
            CompletableFuture.allOf(
                    versionAndMonthPlanFuture,
                    saleOrderFuture,
                    trialPlanFuture,
//                    monthSurplusFuture,
//                    productStockFuture,
                    planMonitorFuture
            ).join();

            log.info("并行初始化任务执行完成");

        } catch (CompletionException e) {
            // 异常处理
            Throwable throwable = e.getCause();
            log.error("初始化任务执行失败! 失败原因:{}", throwable.getMessage(), throwable);
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.initDataFailure"), throwable);
        } finally {
            watch.stop();
            ThreadPoolUtil.shutdown();
        }
        log.info("初始化任务执行完成 ==> 耗时:{} ms", watch.getLastTaskTimeMillis());
    }


    /**
     * 初始化通用
     *
     * @param contextDTO
     */
    private void initCommon(MpRollAdjustContextDTO contextDTO) {
        // 工厂编码
        contextDTO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        if (contextDTO.getMpYear() != null && contextDTO.getMpMonth() != null) {
            // 年月
            contextDTO.setYearMonth(Integer.valueOf(contextDTO.getMpYear() + "" + String.format("%02d",contextDTO.getMpMonth())));
        }
    }

    /**
     * 初始化月度硫化监控
     *
     * @param contextDTO
     */
    private void initPlanMonitor(MpRollAdjustContextDTO contextDTO) {
        MpMonthPlanMonitor queryVO = MpMonthPlanMonitor.builder()
                .factoryCode(contextDTO.getFactoryCode())
                .year(contextDTO.getMpYear())
                .month(contextDTO.getMpMonth())
                .monthPlanVersion(contextDTO.getProductionVersion())
                .build();

        LambdaQueryWrapper<MpMonthPlanMonitor> queryWrapper = new LambdaQueryWrapper<>();
        buildPlanMonitorCondition(queryWrapper, queryVO);
        List<MpMonthPlanMonitor> planMonitorList = mpMonthPlanMonitorEntityMapper.selectList(queryWrapper);
        contextDTO.setMpMonthPlanMonitorList(planMonitorList);
    }

    /**
     * 构建月度硫化监控条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildPlanMonitorCondition(LambdaQueryWrapper<MpMonthPlanMonitor> queryWrapper, MpMonthPlanMonitor queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), MpMonthPlanMonitor::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MpMonthPlanMonitor::getYear, queryVO.getYear());
        queryWrapper.eq(MpMonthPlanMonitor::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpMonthPlanMonitor::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getMonthPlanVersion()), MpMonthPlanMonitor::getMonthPlanVersion, queryVO.getMonthPlanVersion());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getProductionVersion()), MpMonthPlanMonitor::getProductionVersion, queryVO.getProductionVersion());
    }

    /**
     * 初始化成品实时库存
     *
     * @param contextDTO
     */
    private void initProductStock(MpRollAdjustContextDTO contextDTO) {
        MdmProductStock queryVO = new MdmProductStock();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setStockDate(DateUtil.parse(DateUtil.today()));
        Map<String, Object> param = Maps.newHashMap();
        // 传空查询所有
        param.put("materialCodeList", null);
        queryVO.setParams(param);

        // 调用接口查询实时成品库存
        List<MdmProductStock> mdmProductStockList = mesItfService.getProductStock(queryVO);
        contextDTO.setMdmProductStockList(mdmProductStockList);
    }


    /**
     * 初始化月底计划余量
     *
     * @param contextDTO
     */
    private void initMonthSurplus(MpRollAdjustContextDTO contextDTO) {
        MdmMonthSurplus queryVO = new MdmMonthSurplus();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYear(contextDTO.getMpYear());
        queryVO.setMonth(contextDTO.getMpMonth());

        LambdaQueryWrapper<MdmMonthSurplus> queryWrapper = new LambdaQueryWrapper<>();
        buildMonthSurplusCondition(queryWrapper, queryVO);
        List<MdmMonthSurplus> mdmMonthSurplusesList = mdmMonthSurplusEntityMapper.selectList(queryWrapper);
        contextDTO.setMdmMonthSurplusesList(mdmMonthSurplusesList);
    }

    /**
     * 构建月底计划余量条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMonthSurplusCondition(LambdaQueryWrapper<MdmMonthSurplus> queryWrapper, MdmMonthSurplus queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), MdmMonthSurplus::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmMonthSurplus::getYear, queryVO.getYear());
        queryWrapper.eq(MdmMonthSurplus::getMonth, queryVO.getMonth());
        queryWrapper.eq(MdmMonthSurplus::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 初始化试制量试计划
     *
     * @param contextDTO
     */
    private void initTrialPlan(MpRollAdjustContextDTO contextDTO) {
        MpTrialPlan queryVO = new MpTrialPlan();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYear(contextDTO.getMpYear());
        queryVO.setMonth(contextDTO.getMpMonth());

        LambdaQueryWrapper<MpTrialPlan> queryWrapper = new LambdaQueryWrapper<>();
        buildTrialPlanCondition(queryWrapper, queryVO);
        List<MpTrialPlan> mpTrialPlanList = mpTrialPlanEntityMapper.selectList(queryWrapper);
        contextDTO.setMpTrialPlanList(mpTrialPlanList);
    }

    /**
     * 构建试制量试计划条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildTrialPlanCondition(LambdaQueryWrapper<MpTrialPlan> queryWrapper, MpTrialPlan queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), MpTrialPlan::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MpTrialPlan::getYear, queryVO.getYear());
        queryWrapper.eq(MpTrialPlan::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpTrialPlan::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 初始化销售订单池
     *
     * @param contextDTO
     */
    private void initSaleOrderPool(MpRollAdjustContextDTO contextDTO) {
        SalesOrderPool queryVO = new SalesOrderPool();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        // 订单状态，0-关单，1-正常
//        queryVO.setOrderStatus(ApsConstant.TRUE);

        LambdaQueryWrapper<SalesOrderPool> queryWrapper = new LambdaQueryWrapper<>();
        buildSaleOrderPoolCondition(queryWrapper, queryVO);
        List<SalesOrderPool> salesOrderPoolList = salesOrderPoolEntityMapper.selectList(queryWrapper);
        // 排除暂缓订单
        CollUtil.filter(salesOrderPoolList, pool -> !"5".equals(pool.getScmPriority()));
        contextDTO.setSalesOrderPoolList(salesOrderPoolList);
    }


    /**
     * 构建销售订单池条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildSaleOrderPoolCondition(LambdaQueryWrapper<SalesOrderPool> queryWrapper, SalesOrderPool queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), SalesOrderPool::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getOrderStatus()), SalesOrderPool::getOrderStatus, queryVO.getOrderStatus());
        queryWrapper.eq(SalesOrderPool::getIsDelete, YesOrNoEnum.NO.getValue());
    }


    /**
     * 初始化排产版本
     *
     * @param contextDTO
     */
    private void initVersion(MpRollAdjustContextDTO contextDTO) {
        // 查询排产版本
        MpFactoryProductionVersion version = new MpFactoryProductionVersion();
        version.setFactoryCode(contextDTO.getFactoryCode());
        // todo 暂时写死 01 正常
        version.setPlanType("01");
        version.setYear(contextDTO.getMpYear());
        version.setMonth(contextDTO.getMpMonth());

        LambdaQueryWrapper<MpFactoryProductionVersion> wrapper = new LambdaQueryWrapper<>();
        buildVersionCondition(wrapper, version);
        List<MpFactoryProductionVersion> versionList = factoryProductionVersionMapper.selectList(wrapper);
        contextDTO.setFactoryProductionVersionList(versionList);
    }

    /**
     * 初始化月度生产计划
     *
     * @param contextDTO
     */
    private void initMonthPlan(MpRollAdjustContextDTO contextDTO) {
        // 查询月度生产计划
        MpFactoryProductionVersion factoryProductionVersion = getIsFinalVersion(contextDTO);
        FactoryMonthPlanProdFinal queryVO = new FactoryMonthPlanProdFinal();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYearMonth(contextDTO.getYearMonth());
        queryVO.setMonthPlanVersion(factoryProductionVersion == null ? null : factoryProductionVersion.getMonthPlanVersion());

        LambdaQueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new LambdaQueryWrapper<>();
        buildMonthPlanCondition(queryWrapper, queryVO);
        List<FactoryMonthPlanProdFinal> factoryMonthPlanProdFinalList = factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
        List<FactoryMonthPlanFinalAdjustVo> resultList = BeanUtil.copyToList(factoryMonthPlanProdFinalList, FactoryMonthPlanFinalAdjustVo.class);
        contextDTO.setFactoryMonthPlanProdFinalList(resultList);
        contextDTO.setProductionVersion(queryVO.getMonthPlanVersion());
    }


    /**
     * 构建排产版本条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildVersionCondition(LambdaQueryWrapper<MpFactoryProductionVersion> queryWrapper, MpFactoryProductionVersion queryVO) {
        queryWrapper.eq(MpFactoryProductionVersion::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MpFactoryProductionVersion::getYear, queryVO.getYear());
        queryWrapper.eq(MpFactoryProductionVersion::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpFactoryProductionVersion::getPlanType, queryVO.getPlanType());
        queryWrapper.eq(queryVO.getIsFinal() != null, MpFactoryProductionVersion::getIsFinal, queryVO.getIsFinal());
        queryWrapper.eq(MpFactoryProductionVersion::getIsDelete, YesOrNoEnum.NO.getValue());
    }


    /**
     * 构建月度生产计划条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMonthPlanCondition(LambdaQueryWrapper<FactoryMonthPlanProdFinal> queryWrapper, FactoryMonthPlanProdFinal queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), FactoryMonthPlanProdFinal::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(queryVO.getYearMonth() != null, FactoryMonthPlanProdFinal::getYearMonth, queryVO.getYearMonth());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getMonthPlanVersion()), FactoryMonthPlanProdFinal::getMonthPlanVersion, queryVO.getMonthPlanVersion());
        queryWrapper.eq(FactoryMonthPlanProdFinal::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 构建调整明细条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildAdjustDetailCondition(LambdaQueryWrapper<MpAdjustStructureIn> queryWrapper, MpAdjustStructureIn queryVO) {
        queryWrapper.eq(queryVO.getYear() != null, MpAdjustStructureIn::getYear, queryVO.getYear());
        queryWrapper.eq(queryVO.getMonth() != null, MpAdjustStructureIn::getMonth, queryVO.getMonth());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getVersion()), MpAdjustStructureIn::getVersion, queryVO.getVersion());
        queryWrapper.eq(MpAdjustStructureIn::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 校验
     *
     * @param contextDTO
     */
    private void check(MpRollAdjustContextDTO contextDTO) {
        // 初始化通用
        initCommon(contextDTO);
        // 校验年月是否为空
        Assert.isFalse(contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null, I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.yearMonthEmpty"));
        // 特殊规则检查
        specialCheck(contextDTO);
        // 获取定稿的排产版本
        MpFactoryProductionVersion factoryProductionVersion = getIsFinalVersion(contextDTO);
        // 月度生产计划还未定稿，抛出异常
        Assert.isFalse(factoryProductionVersion == null, () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFinalMonthPlan"),
                    contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
    }

    /**
     * 特殊规则检查（由子类实现）
     *
     * @param contextDTO
     */
    public abstract void specialCheck(MpRollAdjustContextDTO contextDTO);

    /**
     * 获取定稿的排产版本
     *
     * @param contextDTO
     * @return
     */
    private MpFactoryProductionVersion getIsFinalVersion(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isEmpty(contextDTO.getFactoryProductionVersionList())) {
            // 初始化排产版本
            initVersion(contextDTO);
        }
        List<MpFactoryProductionVersion> sourceVersionList = contextDTO.getFactoryProductionVersionList();
        if (PubUtil.isEmpty(sourceVersionList)) {
            return null;
        }
        // 筛选：定稿的排产版本
        MpFactoryProductionVersion factoryProductionVersion = sourceVersionList.stream()
                .filter(item -> ApsConstant.TRUE.equals(item.getIsFinal()))
                .findFirst()
                .orElse(null);
        return factoryProductionVersion;
    }


    /**
     * 构建调整明细
     *
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
        // 按物料编码分组，合并同分组下的成型机编码（逗号分隔）
        List<FactoryMonthPlanFinalAdjustVo> mergeMonthPlanProdList = mergeMonthPlanProdList(monthPlanProdList);
        // 生产计划列表按照物料编码进行分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap = mergeMonthPlanProdList.stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
        // 遍历销售订单列表，匹配生产计划
        for (SalesOrderPool salesOrder : salesOrderPoolList) {
            String materialCode = salesOrder.getOriMaterialCode();
            // 物料编码为空则跳过
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            matchMonthPlanList(contextDTO, resultList, materialCode, monthPlanMap, Convert.toInt(salesOrder.getOrdQty(),0));
        }
        return resultList;
    }

    protected void matchMonthPlanList(MpRollAdjustContextDTO contextDTO, List<MpAdjustDetailVo> resultList,
                                      String materialCode, Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap,
                                      Integer ordQty) {
        // 根据物料编码获取对应的生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> matchMonthPlanProdList = monthPlanMap.get(materialCode);
        if (PubUtil.isEmpty(matchMonthPlanProdList)) {
            // 匹配不到直接返回
            return;
        }
        // 组装结果集
        for (FactoryMonthPlanFinalAdjustVo monthPlan : matchMonthPlanProdList) {
            MpAdjustDetailVo adjustDetailVo = new MpAdjustDetailVo();
            adjustDetailVo.setFactoryCode(contextDTO.getFactoryCode());
            adjustDetailVo.setOrdQty(ordQty);
            adjustDetailVo.setMaterialCode(materialCode);
            adjustDetailVo.setScheduledMachines(monthPlan.getCxMachineCode());
            // todo 暂时写死，后续获取
            adjustDetailVo.setHasSpecialMaterial("0");
            adjustDetailVo.setYear(contextDTO.getMpYear());
            adjustDetailVo.setMonth(contextDTO.getMpMonth());
            adjustDetailVo.setVersion(contextDTO.getVersion());
            adjustDetailVo.setMonthPlanVersion(monthPlan.getMonthPlanVersion());
            adjustDetailVo.setProductionVersion(monthPlan.getProductionVersion());
            adjustDetailVo.setStructureName(monthPlan.getStructureName());
            adjustDetailVo.setMaterialDesc(monthPlan.getMaterialDesc());
            // 调整前净需求量（上周）
            setPreviousNetQty(adjustDetailVo,monthPlan);
            // 添加到结果集
            resultList.add(adjustDetailVo);
        }
    }

    /**
     * 调整前净需求量（上周）
     * @param adjustDetailVo
     * @param monthPlan
     */
    private void setPreviousNetQty(MpAdjustDetailVo adjustDetailVo, FactoryMonthPlanFinalAdjustVo monthPlan) {
        // 获取上周的周数
        int week = DateUtil.weekOfMonth(new Date()) - 1;
        Integer previousNetQty = Convert.toInt(monthPlan.getTotalQty(),0);
        Integer adjustQty = Convert.toInt(monthPlan.getFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_ADJUST_QTY + week),0);
        if (adjustQty != 0 && week > 0) {
            previousNetQty = adjustQty;
        }
        adjustDetailVo.setPreviousNetQty(previousNetQty);
    }


    /**
     * 按物料编码分组，合并同分组下的成型机编码（逗号分隔）
     *
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
                .filter(vo -> StringUtils.isNotBlank(vo.getMaterialCode()))
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
        // 遍历分组，合并成型机编码
        monthPlanGroupMap.forEach((materialCode, list) -> {
            // 收集并合并成型机编码（多个逗号分隔）
            String mergedCxMachineCode = list.stream()
                    .map(FactoryMonthPlanFinalAdjustVo::getCxMachineCode)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining(","));
            // 构建合并后的月度生产计划
            FactoryMonthPlanFinalAdjustVo mergedVo = new FactoryMonthPlanFinalAdjustVo();
            FactoryMonthPlanFinalAdjustVo firstVo = list.get(0);
            BeanUtil.copyProperties(firstVo, mergedVo, false);
            mergedVo.setMaterialCode(materialCode);
            mergedVo.setCxMachineCode(mergedCxMachineCode);
            // 添加到结果集
            mergedList.add(mergedVo);
        });
        return mergedList;
    }

    /**
     * 生成调整需求计划
     * @param contextDTO
     */
    protected void createAdjustRequire(MpRollAdjustContextDTO contextDTO) {
        DpDemandPlan queryVo = new DpDemandPlan();
        queryVo.setFactoryCode(contextDTO.getFactoryCode());
        queryVo.setYear(contextDTO.getMpYear());
        queryVo.setMonth(contextDTO.getMpMonth());
        List<DpDemandPlan> dpDemandPlanList = dpDemandPlanService.createAdjustRequire(queryVo);
        contextDTO.setDpDemandPlanList(dpDemandPlanList);
    }


    /**
     * 设置净需求
     * @param contextDTO
     */
    protected void setCurrentNetQty(MpRollAdjustContextDTO contextDTO) {
        // 生成调整需求计划
        createAdjustRequire(contextDTO);
        // 需求计划列表
        List<DpDemandPlan> dpDemandPlanList = contextDTO.getDpDemandPlanList();
        log.warn("设置净需求 ==> 需求计划列表大小：{}", CollUtil.size(dpDemandPlanList));
        if (PubUtil.isEmpty(dpDemandPlanList)) {
            log.warn("设置净需求 ==> 根据工厂:[{}] 年月:[{}] 创建需求计划列表为空，返回", contextDTO.getFactoryCode(),
                    contextDTO.getYearMonth());
            return;
        }
        // 调整明细列表
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 需求计划分组Map
        Map<String, List<DpDemandPlan>> demandPlanMap = convertToDpDemandPlanMap(dpDemandPlanList);
        // 遍历计算
        for (MpAdjustDetailVo adjust : adjustList) {
            if (StringUtils.isEmpty(adjust.getMaterialCode())) {
                continue;
            }
            String materialCode = adjust.getMaterialCode();
            List<DpDemandPlan> dpDemandPlan = MapUtils.getObject(demandPlanMap, materialCode, new ArrayList<>());
            // 汇总排产净需求
            Integer netQtySum = dpDemandPlan.stream()
                    .filter(e -> e.getNetQty() != null)
                    .mapToInt(DpDemandPlan::getNetQty)
                    .sum();
            adjust.setCurrentNetQty(Convert.toInt(netQtySum,0));
        }
    }

    /**
     * 将DpDemandPlan转Map
     */
    private Map<String, List<DpDemandPlan>> convertToDpDemandPlanMap(List<DpDemandPlan> dpDemandPlanList) {
        if (PubUtil.isEmpty(dpDemandPlanList)) {
            return Collections.emptyMap();
        }
        return dpDemandPlanList.stream()
                .filter(demandPlan -> demandPlan != null && demandPlan.getMaterialCode() != null)
                .collect(Collectors.groupingBy(DpDemandPlan::getMaterialCode));
    }


    /**
     * 将MdmMonthSurplus转Map
     */
    private Map<String, MdmMonthSurplus> convertToSurplusMap(List<MdmMonthSurplus> surplusList) {
        if (PubUtil.isEmpty(surplusList)) {
            return Collections.emptyMap();
        }
        return surplusList.stream()
                .filter(surplus -> StringUtils.isNotEmpty(surplus.getMaterialCode()))
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
     * 计划剩余排产量 =【 1日 至 月底】.计划量 - 已生产量，出现负数，默认等于0
     *
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
        // 获取当前日期所属月份的最大天数
        LocalDate currentDate = LocalDate.now();
        int maxDayOfMonth = currentDate.lengthOfMonth();
        // 遍历目标列表，计算赋值
        for (MpAdjustDetailVo adjust : adjustList) {
            if (StringUtils.isEmpty(adjust.getMaterialCode())) {
                continue;
            }
            String materialCode = adjust.getMaterialCode();
            // 计算：day1~targetDay的累计值
            Integer totalScheduledQty = calculateQty(planGroupMap, materialCode, maxDayOfMonth);
            // 获取已生产量（空值按0处理）
            List<MpMonthPlanMonitor> monthPlanMonitorList = MapUtils.getObject(monitorGroupMap, materialCode, new ArrayList<>());
            Integer productionQty = Convert.toInt(monthPlanMonitorList.stream()
                    .filter(e -> e.getProductionQty() != null)
                    .mapToInt(MpMonthPlanMonitor::getProductionQty)
                    .sum(), 0);
            // 计划已排产量
            adjust.setMonthScheduledQty(totalScheduledQty);
            // 已生产量
            adjust.setProductionQty(productionQty);
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
        int total = 0;
        // 遍历day1~targetDay字段，累加值
        for (int day = 1; day <= targetDay; day++) {
            try {
                // 拼接字段名
                String fieldName = "day" + day;
                List<FactoryMonthPlanFinalAdjustVo> monthPlanList = MapUtils.getObject(planGroupMap, materialCode, new ArrayList<>());
                Integer dayValue = monthPlanList.stream()
                        .filter(e -> e.getFieldValueByFieldName(fieldName) != null)
                        .mapToInt(e -> ((Integer) e.getFieldValueByFieldName(fieldName)))
                        .sum();
                total += Convert.toInt(dayValue, 0);
            } catch (Exception e) {
                // 异常时跳过
                continue;
            }
        }
        return total;
    }

    /**
     * 设置其他字段
     *
     * @param contextDTO
     */
    protected void setOtherField(MpRollAdjustContextDTO contextDTO) {
        // 调整明细
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 循环设置
        adjustList.stream().forEach(vo -> {
            // 计算: 调整量 = 净需求 - 计划剩余排产量
            Integer pendingQty = Convert.toInt(vo.getCurrentNetQty(),0) - Convert.toInt(vo.getMonthUnScheduledQty(),0);
            vo.setPendingQty(pendingQty);
            // 确认调整量默认等于待调整量
            vo.setConfirmAdjustQty(pendingQty);
            // 计算：净需求变动 = 净需求 - 调整前净需求量
            Integer netQtyChange = Convert.toInt(vo.getCurrentNetQty(),0) - Convert.toInt(vo.getPreviousNetQty(),0);
            vo.setNetQtyChange(netQtyChange);
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


    protected List<MpAdjustDetailVo> sumByStructureAndMaterial(List<MpAdjustDetailVo> originalList) {
        if (PubUtil.isEmpty(originalList)) {
            return Collections.emptyList();
        }
        Map<String, MpAdjustDetailVo> sumMap = new HashMap<>();
        // 遍历集合，进行分组汇总
        for (MpAdjustDetailVo vo : originalList) {
            String structureName = vo.getStructureName();
            String materialCode = vo.getMaterialCode();
            String groupKey = vo.getGroupKey();
            Integer ordQty = Convert.toInt(vo.getOrdQty(),0);
            if (sumMap.containsKey(groupKey)) {
                MpAdjustDetailVo existVo = sumMap.get(groupKey);
                existVo.setOrdQty(existVo.getOrdQty() + ordQty);
            } else {
                MpAdjustDetailVo newVo = new MpAdjustDetailVo();
                BeanUtil.copyProperties(vo, newVo, Boolean.FALSE);
                newVo.setStructureName(structureName);
                newVo.setMaterialCode(materialCode);
                newVo.setOrdQty(ordQty);
                sumMap.put(groupKey, newVo);
            }
        }
        // 将Map中的汇总结果转换为List返回
        return new ArrayList<>(sumMap.values());
    }

}
