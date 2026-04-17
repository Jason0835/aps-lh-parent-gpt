package com.zlt.aps.mp.adjust.controller;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.mp.adjust.service.IMpAdjustStructureInService;
import com.zlt.aps.mp.adjust.service.impl.MpWeekAdjustFactory;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.mp.api.domain.vo.MpAdjustDetailVo;
import com.zlt.aps.mp.common.utils.StringUtil;
import com.zlt.aps.mp.engine.scheduling.matching.MatchingAdjuestProductionHandler;
import com.zlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.mp.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.dto.MpWeekRollAdjustDTO;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.mp.api.enums.WeekAdjustTypeEnum;
import com.zlt.common.utils.PubUtil;
import com.zlt.msg.message.api.IMsgTemplateRemoteService;
import com.zlt.msg.message.domain.entity.MsgTemplate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MpWeekRollAdjustController.java
* 描    述：周程滚动调整 控制层类：....
*@author zlt
*@date 2025-12-24
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "周程滚动调整")
@RestController
@RequestMapping("/mpWeekRollAdjust")
public class MpWeekRollAdjustController extends BaseController {

    @Autowired
    private MpWeekAdjustFactory mpWeekAdjustFactory;

    @Autowired
    private RedisService redisService;

    @Autowired
    private IMpAdjustStructureInService mpAdjustStructureInService;

    @Autowired
    private IMsgTemplateRemoteService templateRemoteService;

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    @Autowired
    private MatchingAdjuestProductionHandler matchingAdjuestProductionHandler;


    /**
     * 获取调整明细列表
     */
    @ApiOperation("获取调整明细列表")
    @PostMapping("/getAdjustDetailList")
    @DistributedLock(
            key = "'ADJ:GET:' + #weekRollAdjustDTO.adjustType + #weekRollAdjustDTO.mpYear + #weekRollAdjustDTO.mpMonth",
            waitTime = 0,
            leaseTime = -1,
            failMsg = "ui.data.alert.getAdjustDetail.run",
            args = {"#weekRollAdjustDTO.mpYear","#weekRollAdjustDTO.mpMonth"}
    )
    public TableDataInfo getAdjustDetailList(@RequestBody MpWeekRollAdjustDTO weekRollAdjustDTO) {
        // 获取周程滚动调整策略
        IMpWeekAdjustService weekAdjustStrategy = mpWeekAdjustFactory.getStrategy(weekRollAdjustDTO.getAdjustType());
        if (weekAdjustStrategy == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindStrategy"));
        }
        // 构建上下文对象
        MpRollAdjustContextDTO contextDTO = buildAdjustContext(weekRollAdjustDTO);
        log.info("获取调整明细 ==> 开始执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
        // 执行周程滚动调整策略（生成调整明细）
        weekAdjustStrategy.generateAdjust(contextDTO);
        log.info("获取调整明细 ==> 完成执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
        return getDataTable(contextDTO.getAdjustDetailList());
    }

    /**
     * 自动调整
     */
    @ApiOperation("自动调整")
    @PostMapping("/autoAdjust")
    public AjaxResult autoAdjust(@RequestBody MpWeekRollAdjustDTO weekRollAdjustDTO) {
        String key = ApsConstant.REDIS_ADJUST_STRUCT_AUTO + weekRollAdjustDTO.getFactoryCode()+weekRollAdjustDTO.getAdjustType();
        if (!StringUtil.isEmptyWithTrim(weekRollAdjustDTO.getScheduledMachines())){
            key = key+weekRollAdjustDTO.getScheduledMachines();
        }
        if (ApsConstant.TRUE.equals(redisService.getCacheObject(key))) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.distributed.lock.fail"));
        }
        redisService.setCacheObject(key, ApsConstant.TRUE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
        try{
            // 获取周程滚动调整策略
            IMpWeekAdjustService weekAdjustStrategy = mpWeekAdjustFactory.getStrategy(weekRollAdjustDTO.getAdjustType());
            if (weekAdjustStrategy == null) {
                throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindStrategy"));
            }
            // 构建上下文对象
            MpRollAdjustContextDTO contextDTO = buildAutoAdjustContext(weekRollAdjustDTO,weekAdjustStrategy);
            log.info("自动调整 ==> 开始执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(weekRollAdjustDTO.getAdjustType()).getName(),
                    String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
            // 执行周程滚动调整策略（自动调整）
            weekAdjustStrategy.autoAdjust(contextDTO);
            log.info("自动调整 ==> 完成执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(weekRollAdjustDTO.getAdjustType()).getName(),
                    String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
            // 排序 按英寸->结构->最大型腔数->主花纹->活块数->物料描述
            sortAdjustResultList(contextDTO.getAdjustResultList());
            if (StringUtil.isEmptyWithTrim(contextDTO.getMsgStructureAdjustPreClose().toString())){
                return AjaxResult.success(contextDTO.getMsgStructureAdjustPreClose().toString(),contextDTO.getAdjustResultList());
            }
            return AjaxResult.success(contextDTO.getAdjustResultList());
        }finally {
            redisService.setCacheObject(key, ApsConstant.FALSE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
        }
    }

    /**
     * 排序：按英寸->结构->最大型腔数->主花纹->活块数->物料描述
     * @param mpAdjustResultList
     */
    protected void sortAdjustResultList(List<MpAdjustResult> mpAdjustResultList) {
        if (PubUtil.isEmpty(mpAdjustResultList)) {
            return;
        }
        // 主花纹的最大型腔数
        Map<String, Integer> maxMouldCavityQtyMap = new HashMap<>();
        for (MpAdjustResult adjustResult: mpAdjustResultList) {
            //记录主花纹的最大型腔数
            Integer maxMouldCavityQty = maxMouldCavityQtyMap.getOrDefault(adjustResult.getMainPattern(), 0);
            maxMouldCavityQtyMap.put(adjustResult.getMainPattern(), Math.max(maxMouldCavityQty, adjustResult.getMouldCavityQty()));
        }
        mpAdjustResultList.stream().forEach(s -> { // 设置对应的最大型腔数和最大活块数
            s.setMaxMouldCavityQty(maxMouldCavityQtyMap.getOrDefault(s.getMainPattern(), 0));
        });

        Collections.sort(mpAdjustResultList, getAdjustResultSortComparator());
    }

    /**
     * 排序器：按英寸->结构->最大型腔数->主花纹->活块数->物料描述
     * @return
     */
    protected Comparator<MpAdjustResult> getAdjustResultSortComparator() {
        // 一级排序：结构名称升序，空值排最后
        return Comparator.comparing(MpAdjustResult::getTbrProSize, Comparator.nullsLast(String::compareTo))
                .thenComparing(MpAdjustResult::getStructureName,Comparator.nullsLast(String::compareTo))
                // 最大型腔数
                .thenComparing(MpAdjustResult::getMaxMouldCavityQty, Comparator.reverseOrder())
                // 主花纹
                .thenComparing(MpAdjustResult::getMainPattern, Comparator.nullsLast(String::compareTo))
                // 活块数
                .thenComparing(MpAdjustResult::getTypeBlockQty, Comparator.reverseOrder())
                // 物料描述
                .thenComparing(MpAdjustResult::getMaterialDesc, Comparator.nullsLast(String::compareTo));
    }

    /**
     * 确认调整结果
     */
    @ApiOperation("确认调整结果")
    @PostMapping("/confirmAdjust")
    public AjaxResult confirmAdjust(@RequestBody MpWeekRollAdjustDTO weekRollAdjustDTO) {
        // 获取周程滚动调整策略
        IMpWeekAdjustService weekAdjustStrategy = mpWeekAdjustFactory.getStrategy(weekRollAdjustDTO.getAdjustType());
        if (weekAdjustStrategy == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindStrategy"));
        }
        // 构建上下文对象
        MpRollAdjustContextDTO contextDTO = buildAdjustContext(weekRollAdjustDTO);
        log.info("确认调整 ==> 开始执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
        // 执行周程滚动调整策略（确认调整）
        weekAdjustStrategy.confirmAdjust(contextDTO);
        log.info("确认调整 ==> 完成执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
        return AjaxResult.success();
    }


    /**
     * 构建获取调整订单上下文对象
     * @param weekRollAdjustDTO
     * @return
     */
    private MpRollAdjustContextDTO buildAdjustContext(MpWeekRollAdjustDTO weekRollAdjustDTO) {
        MpRollAdjustContextDTO contextDTO = BeanUtil.copyProperties(weekRollAdjustDTO, MpRollAdjustContextDTO.class);
        return contextDTO;
    }

    /**
     * 构建自动调整上下文对象（并行优化版）
     * <p>将互不依赖的数据查询拆分为三个阶段：
     * <ul>
     *   <li>阶段1（串行）：initVersion，获取 productionVersion/productType 供后续查询使用</li>
     *   <li>阶段2（并行）：11个独立查询同时发起，等待全部完成</li>
     *   <li>阶段3（串行）：依赖阶段2结果的后续初始化</li>
     * </ul>
     * </p>
     * @param weekRollAdjustDTO 请求参数
     * @param weekAdjustStrategy 周程滚动调整策略
     * @return 上下文对象
     */
    private MpRollAdjustContextDTO buildAutoAdjustContext(MpWeekRollAdjustDTO weekRollAdjustDTO, IMpWeekAdjustService weekAdjustStrategy) {
        Date startTime = new Date();
        log.debug(String.format("自动调整初始化,开始时间:%s", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, startTime)));
        MpRollAdjustContextDTO contextDTO = BeanUtil.copyProperties(weekRollAdjustDTO, MpRollAdjustContextDTO.class);

        // ===== 阶段1（串行）：初始定稿版本信息，后续查询依赖 productionVersion / productType =====
        weekAdjustStrategy.initVersion(contextDTO);
        if (PubUtil.isEmpty(contextDTO.getFactoryProductionVersionList())) {
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecordNotFound"),
                    contextDTO.getMpYear(), contextDTO.getMpMonth()));
        }
        MpFactoryProductionVersion firstVersion = contextDTO.getFactoryProductionVersionList().get(0);
        contextDTO.setMonthPlanVersion(firstVersion.getMonthPlanVersion());
        contextDTO.setProductType(firstVersion.getProductTypeCode());
        contextDTO.setProductionVersion(firstVersion.getProductionVersion());

        // ===== 阶段2（并行）：各独立查询并发执行，互不依赖 =====
        // 2.1 月定稿数据
        CompletableFuture<Void> finalListFuture = CompletableFuture.runAsync(
                () -> contextDTO.setFactoryMonthPlanProdFinalList(mpAdjustStructureInService.selectMpFinalList(contextDTO)));
        // 2.2 结构转产列表
        CompletableFuture<Void> structureAllocationFuture = CompletableFuture.runAsync(
                () -> contextDTO.setStructureAllocationList(mpAdjustStructureInService.selectMpStructureAllocationList(contextDTO)));
        // 2.3 周程滚动参数
        CompletableFuture<Void> paramMapFuture = CompletableFuture.runAsync(
                () -> contextDTO.setParamMap(mpAdjustStructureInService.getMpWeekAdjustParam(contextDTO.getFactoryCode(), contextDTO.getProductType())));
        // 2.4 结构硫化配比
        CompletableFuture<Void> structureLhRatioFuture = CompletableFuture.runAsync(
                () -> contextDTO.setStructureLhRatio(mpAdjustStructureInService.getStructureLhRatio(contextDTO)));
        // 2.5 工作日历
        CompletableFuture<Void> workCalendarFuture = CompletableFuture.runAsync(
                () -> contextDTO.setWorkCalendarMap(mpAdjustStructureInService.getWorkCalendarMap(contextDTO)));
        // 2.6 周期结构最低硫化机台数
        CompletableFuture<Void> cycleStructureMinFuture = CompletableFuture.runAsync(
                () -> contextDTO.setCycleStructureMinLhMachinesMap(mpAdjustStructureInService.getCycleStructureMinMachinesMap(contextDTO)));
        // 2.7 型腔与活块数量
        CompletableFuture<Void> cavityBlockFuture = CompletableFuture.runAsync(
                () -> contextDTO.setCavity2BlockMap(mpAdjustStructureInService.getCavityAndBlockQtyMap(contextDTO)));
        // 2.8 总硫化机台数
        CompletableFuture<Void> lhMachineCountFuture = CompletableFuture.runAsync(
                () -> contextDTO.setTotalLhMachines(mpAdjustStructureInService.getLhMachineCount(contextDTO)));
        // 2.9 结构统计
        CompletableFuture<Void> structureStatisticFuture = CompletableFuture.runAsync(
                () -> contextDTO.setStructureStatisticMap(mpAdjustStructureInService.loadMpMonthPlanStatistics(contextDTO)));
        // 2.10 消息模板（内部含2个远程调用，也并行处理）
        CompletableFuture<Void> msgTemplateFuture = CompletableFuture.runAsync(
                () -> initMsgTemplate(contextDTO));
        // 2.11 工厂名称
        CompletableFuture<Void> factoryNameFuture = CompletableFuture.runAsync(() -> {
            List<SysDictData> dictDataList = iSysDictDataCacheService.getType("biz_factory_name");
            String factoryName = dictDataList.stream()
                    .filter(dictData -> dictData.getDictValue().equals(contextDTO.getFactoryCode()))
                    .findFirst().get().getDictLabel();
            contextDTO.setFactoryName(factoryName);
        });

        try {
            CompletableFuture.allOf(
                    finalListFuture, structureAllocationFuture, paramMapFuture,
                    structureLhRatioFuture, workCalendarFuture, cycleStructureMinFuture,
                    cavityBlockFuture, lhMachineCountFuture, structureStatisticFuture,
                    msgTemplateFuture, factoryNameFuture
            ).join();
            log.debug("自动调整初始化,并行数据查询全部完成");
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            log.error("自动调整初始化,并行数据查询失败! 原因:{}", cause.getMessage(), cause);
            if (cause instanceof BusinessException) {
                throw (BusinessException) cause;
            }
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.initDataFailure"), cause);
        }

        // ===== 阶段3（串行）：依赖阶段2结果的后续初始化 =====
        // 设置调整日（依赖 paramMap）
        setAdjustDate(contextDTO);
        contextDTO.setVersion(weekRollAdjustDTO.getVersion());
        contextDTO.setAdjustType(weekRollAdjustDTO.getAdjustType());
        // 初始调整过程日志
        contextDTO.setAdjustProcLogList(new ArrayList<>());
        // 设置OEM配置集合（依赖 paramMap）
        initOemParam(contextDTO);
        // 加载搭配排产的必要基础数据（依赖以上所有数据）
        matchingAdjuestProductionHandler.initAdjustContextDTO(contextDTO);
        Date endTime = new Date();
        log.debug(String.format("自动调整初始化,结束时间:%s,总耗时:%s毫秒", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, endTime), DateUtils.getDiffMillTime(startTime, endTime)));
        return contextDTO;
    }

    /**
     * 初始消息模板（并行获取两个远程模板）
     * @param contextDTO
     */
    private void initMsgTemplate(MpRollAdjustContextDTO contextDTO) {
        // 并行获取两个消息模板
        CompletableFuture<MsgTemplate> remainQtyFuture = CompletableFuture.supplyAsync(
                () -> templateRemoteService.getTemplateInfo(MsgTemplateEnums.MP_SKU_REMAIN_QTY_NO_FULL.getCode()));
        CompletableFuture<MsgTemplate> preCloseFuture = CompletableFuture.supplyAsync(
                () -> templateRemoteService.getTemplateInfo(MsgTemplateEnums.MP_STRUCTURE_ADJUST_PRE_CLOSE.getCode()));
        try {
            CompletableFuture.allOf(remainQtyFuture, preCloseFuture).join();
        } catch (CompletionException e) {
            log.warn("消息模板获取失败，将使用空模板继续执行. 原因:{}", e.getCause().getMessage());
        }
        //1. SKU原余量小于调整次日至锁定截止日的计划量提醒
        MsgTemplate remainQtyTemplate = remainQtyFuture.getNow(null);
        if (remainQtyTemplate != null) {
            contextDTO.setMsgTemplateWithRemainQtyNoFull(remainQtyTemplate.getContent());
        }
        contextDTO.setMsgRemainQtyNoFull(new StringBuilder());
        //2. 结构内调整减量提前收尾
        MsgTemplate preCloseTemplate = preCloseFuture.getNow(null);
        if (preCloseTemplate != null) {
            contextDTO.setMsgTemplateWithStructureAdjustPreClose(preCloseTemplate.getContent());
        }
        contextDTO.setMsgStructureAdjustPreClose(new StringBuilder());
    }

    /**
     * 初始OEM相关参数
     * @param contextDTO
     */
    private void initOemParam(MpRollAdjustContextDTO contextDTO) {
        String oemBrandConfig = (String) contextDTO.getParamMap().get(MonthPlanEnums.OEM_BRAND_CONFIG.getCode());
        Set<String> oemBrandConfigSet = Collections.emptySet();
        if (!StringUtil.isEmptyWithTrim(oemBrandConfig)){
            oemBrandConfigSet = Stream.of(oemBrandConfig.split(StringConstant.COMMA)).collect(Collectors.toSet());
        }
        contextDTO.setOemBrandConfigSet(oemBrandConfigSet);
        if (contextDTO.getParamMap().get(MonthPlanEnums.OEM_BRAND_CAPACITY.getCode()) == null){
            contextDTO.setTotalOemQty(0);
        }else{
            contextDTO.setTotalOemQty((Integer) contextDTO.getParamMap().get(MonthPlanEnums.OEM_BRAND_CAPACITY.getCode()));
        }
    }

    /**
     * 设置调整日
     * @param contextDTO 周程滚动上下文
     */
    private void setAdjustDate(MpRollAdjustContextDTO contextDTO) {
        String weekRollAdjustDate = (String) contextDTO.getParamMap().get(MonthPlanEnums.WEEK_ROLL_ADJUST_DATE.getCode());
        Date adjustDate = StringUtil.isEmptyWithTrim(weekRollAdjustDate) ? DateUtils.getNowDate() : DateUtils.parseDate(weekRollAdjustDate);
        if (contextDTO.getMpMonth() != DateUtils.getMonth(adjustDate)){
            //若调整月不等于当前月，则将调整日设置1
            contextDTO.setAdjustDay(FactoryConstant.MONTH_START_DAY);
        }else{
            contextDTO.setAdjustDay(DateUtils.getDay(adjustDate));
        }
    }

}
