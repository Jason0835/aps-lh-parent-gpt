package com.zlt.aps.mp.adjust.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.mp.engine.capacity.MpAdjustDailyCapacityLimit;
import com.zlt.aps.mp.engine.scheduling.matching.MatchingProductionHandler;
import com.zlt.aps.mp.engine.adjust.MpWeekRollAdjustEngine;
import com.zlt.aps.mp.adjust.service.IMpAdjustStructureOutService;
import com.zlt.aps.mp.api.annotation.WeekAdjustType;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureOut;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.mp.api.domain.vo.MpAdjustDetailVo;
import com.zlt.aps.mp.api.enums.WeekAdjustTypeEnum;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 结构外调整策略
 * @author wengpc
 */
@Slf4j
@Service
@WeekAdjustType(adjustType = WeekAdjustTypeEnum.STRUCTURE_OUT)
public class MpAdjustStructureOutStrategy extends AbstractBaseWeekAdjustService {

    @Autowired
    private IMpAdjustStructureOutService mpAdjustStructureOutService;

    @Autowired
    private MatchingProductionHandler matchingProductionHandler;

    @Override
    public void doGenerateAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        // 1、设置版本号
        setVersion(contextDTO, BusiConstant.WeekRollAdjust.VERSION_PREFIX);
        // 2、构建结构外调整明细
        List<MpAdjustDetailVo> adjustDetailList = buildAdjustDetailList(contextDTO);
        // 3、构建结构内调整明细（月度计划有，无订单）
        List<MpAdjustDetailVo> adjustDetailByMonthPlanList = buildAdjustDetailByMonthPlanList(contextDTO);
        List<MpAdjustDetailVo> resultList = new ArrayList<>();
        resultList.addAll(adjustDetailList);
        resultList.addAll(adjustDetailByMonthPlanList);
        // 检查产品结构字段为空
//        List<String> errorMsgList = checkStructNameEmpty(resultList);
//        String errorMsg = Optional.ofNullable(errorMsgList)
//                .orElse(Collections.emptyList())
//                .stream()
//                .distinct()
//                .collect(Collectors.joining(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE));
//        Assert.isFalse(PubUtil.isNotEmpty(errorMsgList), () -> {
//            return new BusinessException(errorMsg);
//        });

//        // 检查有错误的信息
//        Map<String, List<String>> messageMap = contextDTO.getMessageMap();
//        List<String> errorMsgList = messageMap.get(ApsConstant.APS_STRING_1);
//        if (PubUtil.isNotEmpty(errorMsgList)) {
//            String errorMsg = Optional.ofNullable(errorMsgList)
//                    .orElse(Collections.emptyList())
//                    .stream()
//                    .distinct()
//                    .collect(Collectors.joining(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE));
//            Assert.isFalse(StringUtils.isNotEmpty(errorMsg), () -> {
//                return new BusinessException(errorMsg);
//            });
//        }

        // 4、通过排产机台、结构筛选结构外调整明细
        filterAdjustDetailList(contextDTO,resultList);
        // 未获取到调整记录，抛出异常
        Assert.isFalse(PubUtil.isEmpty(resultList), () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindAdjustDetailList"),
                    contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 5、按照结构、物料编码维度进行分组，并汇总订单量
        resultList = sumByStructureAndMaterial(resultList, Boolean.FALSE);
        contextDTO.setAdjustDetailList(resultList);
        // 6、设置是否特殊材料
        setHasSpecialMaterial(contextDTO);
        // 7、设置净需求
        setCurrentNetQty(contextDTO);
        // 8、设置型腔、活块数量
        setMoldCavityInsert(contextDTO);
        // 9、设置计划剩余排产量、计划已排产量、已生产量
        setMonthUnScheduledQty(contextDTO);
        // 10、筛选：|净需求 - 计划已排产量| > 0的数据
        filterAdjustList(contextDTO.getAdjustDetailList());
        // 筛选后数据为空，抛出异常
        Assert.isFalse(PubUtil.isEmpty(contextDTO.getAdjustDetailList()), () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notMatchAdjustDetailList"), contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 11、设置其他字段
        setOtherField(contextDTO);
    }


    /**
     * 通过排产机台、结构筛选结构外调整明细
     * @param contextDTO
     * @param adjustDetailList
     * @return
     */
    @Override
    protected void filterAdjustDetailList(MpRollAdjustContextDTO contextDTO, List<MpAdjustDetailVo> adjustDetailList) {
        if (PubUtil.isEmpty(adjustDetailList)) {
            return;
        }

        CollUtil.filter(adjustDetailList, item -> StringUtils.equals(item.getStructureName(), contextDTO.getStructureName())
                && (StringUtils.isEmpty(item.getScheduledMachines())
                || StringUtils.contains(item.getScheduledMachines(), contextDTO.getScheduledMachines())));
    }

    /**
     * 更新结构转产
     *
     * @param contextDTO
     */
    @Override
    protected void updateStructureAllocationList(MpRollAdjustContextDTO contextDTO) {
        // 工厂编码
        String factoryCode = contextDTO.getFactoryCode();
        // 年份
        Integer year = contextDTO.getMpYear();
        // 月份
        Integer month = contextDTO.getMpMonth();
        // 排产版本
        String productionVersion = contextDTO.getProductionVersion();
        // 结构名称
        String structureName = contextDTO.getStructureName();
        // 机台
        String scheduledMachines = contextDTO.getScheduledMachines();
        // 开始日期
        Integer startDay = contextDTO.getStartDay();
        // 结束日期
        Integer endDay = contextDTO.getEndDay();
        // 调整开始日期
        Integer adjustStartDay = contextDTO.getAdjustStartDay();
        // 调整结束日期
        Integer adjustEndDay = contextDTO.getAdjustEndDay();
        if (StringUtils.isEmpty(productionVersion) || StringUtils.isEmpty(factoryCode)
                || StringUtils.isEmpty(structureName) || StringUtils.isEmpty(scheduledMachines)
                || year == null || month == null || startDay == null || endDay == null
                || adjustStartDay == null || adjustEndDay == null) {
            log.warn("更新结构转产：必须字段为空，直接返回");
            return;
        }
        Integer allotDays = adjustEndDay - adjustStartDay + 1;
        // 更新结构转产
        try {
            LambdaUpdateWrapper<MpStructureAllocation> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(MpStructureAllocation::getStructureName, structureName)
                    .eq(MpStructureAllocation::getCxMachineCode, scheduledMachines)
                    .eq(MpStructureAllocation::getFactoryCode, factoryCode)
                    .eq(MpStructureAllocation::getYear, year)
                    .eq(MpStructureAllocation::getMonth, month)
                    .eq(MpStructureAllocation::getProductionVersion, productionVersion)
                    .set(MpStructureAllocation::getBeginDay, adjustStartDay)
                    .set(MpStructureAllocation::getEndDay, adjustEndDay)
                    .set(MpStructureAllocation::getAllotDays, allotDays);
            mpStructureAllocationEntityMapper.update(null, wrapper);
            log.info("更新结构转产成功");
        } catch (Exception e) {
            log.error("更新结构转产批量操作异常", e);
            throw new RuntimeException("更新结构转产失败", e);
        }
    }


    /**
     * 查询调整明细
     *
     * @param contextDTO
     */
    @Override
    protected void queryAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        if (contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null
                || StringUtils.isEmpty(contextDTO.getVersion())) {
            log.warn("查询调整明细：年份或者月份为空，直接返回");
            return;
        }
        // 年份
        Integer year = contextDTO.getMpYear();
        // 月份
        Integer month = contextDTO.getMpMonth();
        // 调整版本号
        String version = contextDTO.getVersion();

        MpAdjustStructureOut queryVO = new MpAdjustStructureOut();
        queryVO.setYear(year);
        queryVO.setMonth(month);
        queryVO.setVersion(version);

        LambdaQueryWrapper<MpAdjustStructureOut> queryWrapper = new LambdaQueryWrapper<>();
        buildAdjustDetailCondition(queryWrapper, queryVO);

        try {
            List<MpAdjustStructureOut> adjustStructureOutList = mpAdjustStructureOutEntityMapper.selectList(queryWrapper);
            List<MpAdjustDetailVo> adjustDetailList = BeanUtil.copyToList(adjustStructureOutList, MpAdjustDetailVo.class);
            contextDTO.setAdjustDetailList(adjustDetailList);
            log.info("查询调整明细成功，年份：{}，月份：{}，版本：{}，共查询:{}条记录",
                    year, month, version, adjustDetailList.size());
        } catch (Exception e) {
            log.error("查询调整明细异常，年份：{}，月份：{}，版本：{}", year, month, version, e);
            throw new RuntimeException("查询调整明细失败", e);
        }
    }

    /**
     * 更新调整明细
     * 将本次调整的量，回填到"调整明细".实际调整；置换过程回填到“调整明细".调整原因
     *
     * @param contextDTO
     */
    @Override
    protected void updateAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        if (PubUtil.isEmpty(adjustResultList) || PubUtil.isEmpty(adjustDetailList)) {
            log.warn("更新调整明细：调整结果列表或调整明细列表为空，直接返回");
            return;
        }
        // 调整结果按照物料编号分组
        Map<String, List<MpAdjustResult>> adjustDetailMap = buildMaterialCodeAdjustMap(adjustResultList);
        // 遍历调整明细列表匹配调整结果(更新实际调整、调整原因)
        List<MpAdjustStructureOut> adjustStructureOutList = BeanUtil.copyToList(adjustDetailList, MpAdjustStructureOut.class);
        for (MpAdjustStructureOut adjustStructureOut : adjustStructureOutList) {
            String materialCode = adjustStructureOut.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            MpAdjustResult adjustResult = getFirstAdjustResult(adjustDetailMap, materialCode);
            if (adjustResult == null) {
                log.warn("更新调整明细：物料编号:{}未查询到对应调整结果，跳过", materialCode);
                continue;
            }
            // 实际调整
            adjustStructureOut.setActualAdjustQty(Convert.toInt(adjustResult.getTotalPlanQty(), 0));
            // 调整原因 TODO
            adjustStructureOut.setAdjustReason("");
        }
        // 更新调整明细
        try {
            baseDao.updateBatch(adjustStructureOutList);
            log.info("更新调整明细成功，共更新:{}条记录", adjustStructureOutList.size());
        } catch (Exception e) {
            log.error("更新调整明细批量操作异常", e);
            throw new RuntimeException("更新调整明细失败", e);
        }
    }

    /**
     * 筛选：|净需求 - 计划已排产量| > 0的数据
     * @param adjustList
     */
    private void filterAdjustList(List<MpAdjustDetailVo> adjustList) {
        if (PubUtil.isEmpty(adjustList)) {
            return;
        }
        adjustList.removeIf(adjust -> {
            Integer currentNetQty = Convert.toInt(adjust.getCurrentNetQty(),0);
            Integer monthScheduledQty = Convert.toInt(adjust.getMonthScheduledQty(),0);
            boolean isOnlyConventionReserveHasValue = isOnlyConventionReserveHasValue(adjust);
            return (Math.abs(currentNetQty - monthScheduledQty) == 0) || isOnlyConventionReserveHasValue;
        });
    }

    @Override
    public void doAutoAdjust(MpRollAdjustContextDTO contextDTO) {
        //结构外调整记录
        contextDTO.setMpAdjustStructureOutList(mpAdjustStructureOutService.selectMpAdjustStructureOutList(contextDTO));
        //1.结构外订单调整记录空检查
        if (PubUtil.isEmpty(contextDTO.getMpAdjustStructureOutList())){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.orderAdjustRecordNotFound"),
                    contextDTO.getMpYear(),contextDTO.getMpMonth()));
        }

        //2.按结构序列化分组
        //Map<String, List<FactoryMonthPlanFinalAdjustVo>> mpProdFinalMap = contextDTO.getFactoryMonthPlanProdFinalList().stream().collect(Collectors.groupingBy(item->item.getStructureName()));
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> mpProdFinalMap =  convertToMap(contextDTO.getFactoryMonthPlanProdFinalList());
        Date startTime,endTime;
        MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();
        //contextDTO.setStructureName(contextDTO.getStructureName());
        List<FactoryMonthPlanFinalAdjustVo> oneStructMpFinalList = mpProdFinalMap.get(contextDTO.getStructureName()) == null ? new ArrayList<>():mpProdFinalMap.get(contextDTO.getStructureName());
        contextDTO.setSpecStructureTotalQty(0);
        if (YesOrNoEnum.YES.getCode().equals(contextDTO.getMpAdjustStructureOutList().get(0).getHasSpecialMaterial())){
            //若是特殊结构,预存特殊结构的总实际排产量
            setSpecStructureTotalQty(contextDTO,oneStructMpFinalList);
        }
        List<MpStructureAllocation> structureAllocationList = contextDTO.getStructureAllocationList().stream().filter(x->x.getStructureName().equals(contextDTO.getStructureName())).collect(Collectors.toList());
        //3.更新结构转产表对应成型机台的调整开始日、结束日
        updateStructureAdjustDayByMachine(structureAllocationList,contextDTO);
        //4.补充更新周程滚动上下文
        contextDTO.setOneStructureAllocationList(structureAllocationList);
        //4.1初始锁定日
        contextDTO.setLockEndDay(getLockEndDay(contextDTO));
        //4.2初始结构收尾日
        initStructureStartAndEndDay(contextDTO);
        //4.3初始化日志和消息
        contextDTO.setLogDetail(new StringBuilder());
        //5.处理单个结构间调整
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,自动调整,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        weekRollAdjustEngine.doStructureOutForOne(contextDTO,contextDTO.getMpAdjustStructureOutList(), oneStructMpFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,自动调整,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //6.在搭配排产前，重算每日产能限制，包括硫化机台数、胎胚种类数
        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
        reCalcAdjustDailyCapacityLimit(contextDTO, oneStructMpFinalList, adjustDailyCapacityLimitObj);

        //7 执行结构间搭配排产,暂不考虑特殊结构
        //=========================================================
        matchingProductionHandler.matchingAdjustProduction(contextDTO, oneStructMpFinalList, false);
        //=========================================================

        //8.在搭配排产前，重算每日产能限制，包括硫化机台数、胎胚种类数
        reCalcAdjustDailyCapacityLimit(contextDTO, oneStructMpFinalList, adjustDailyCapacityLimitObj);

        //9.设置模具变化信息
        for (FactoryMonthPlanFinalAdjustVo mpFinalVo:oneStructMpFinalList){
            weekRollAdjustEngine.setMouldChangeInfo(adjustDailyCapacityLimitObj,contextDTO.getParamMap(),contextDTO.getStructureStartDay(),mpFinalVo,contextDTO.getDailyCapacityLimitVoMap());
        }

        //10.构建月计划统计结果
        MpMonthPlanStatistics monthPlanStatisticsVo = buildMonthPlanStatistics(contextDTO, mpProdFinalMap.get(contextDTO.getStructureName()));

        contextDTO.setSaveMpProdFinalList(oneStructMpFinalList);
        List<MpMonthPlanStatistics> monthPlanStatisticsList = new ArrayList<>();
        monthPlanStatisticsList.add(monthPlanStatisticsVo);
        contextDTO.setMonthPlanStatisticsList(monthPlanStatisticsList);
        contextDTO.setSaveAdjustProcLogList(contextDTO.getAdjustProcLogList());

        //11.保存调整日志
        saveMpAdjustLog(contextDTO);
    }

    @Override
    protected void backfillRealAdjustResult(MpRollAdjustContextDTO contextDTO) {
        // 重新获取调整记录，避免过程中浅拷贝导致数据被更新
        List<MpAdjustStructureOut> mpAdjustStructureOutList = mpAdjustStructureOutService.selectMpAdjustStructureOutList(contextDTO);
        if (PubUtil.isEmpty(mpAdjustStructureOutList)){
            return;
        }
        List<FactoryMonthPlanFinalAdjustVo> mpFinalAdjustList = contextDTO.getSaveMpProdFinalList();
        if (PubUtil.isEmpty(mpFinalAdjustList)){
            return;
        }
        Map<String, FactoryMonthPlanFinalAdjustVo> mpFinalAdjustMap = mpFinalAdjustList.stream().collect(Collectors.groupingBy(item->item.getMaterialCode(),
                Collectors.collectingAndThen(Collectors.toList(),m-> {
                    return m.get(0);
                })));
        //更新实际调整量
        FactoryMonthPlanFinalAdjustVo mpFinalVo;
        for (MpAdjustStructureOut structureOut:mpAdjustStructureOutList){
            // 实际调整默认：0
            if (structureOut.getActualAdjustQty() == null) {
                structureOut.setActualAdjustQty(0);
            }
            mpFinalVo = mpFinalAdjustMap.get(structureOut.getMaterialCode());
            if (mpFinalVo != null){
                structureOut.setActualAdjustQty(Convert.toInt(mpFinalVo.getActualAdjustQty(), 0));
            }
        }
        baseDao.updateBatch(mpAdjustStructureOutList);
    }

    /**
     * 更新结构转产表对应成型机台的调整开始日、结束日
     * @param structureAllocationList
     * @param contextDTO
     */
    private void updateStructureAdjustDayByMachine(List<MpStructureAllocation> structureAllocationList,MpRollAdjustContextDTO contextDTO){
        if (PubUtil.isEmpty(structureAllocationList)){
            return;
        }
        for (MpStructureAllocation structureAllocation:structureAllocationList){
            if (structureAllocation.getCxMachineCode().equals(contextDTO.getScheduledMachines())){
                //更新结构转产表对应成型机台的调整开始日、结束日
                structureAllocation.setBeginDay(contextDTO.getAdjustStartDay());
                structureAllocation.setEndDay(contextDTO.getAdjustEndDay());
                return;
            }
        }
    }


    @Override
    public void specialInit(MpRollAdjustContextDTO contextDTO) {
    }

    @Override
    public void specialCheck(MpRollAdjustContextDTO contextDTO) {
        // 判断机台是否为空
        Assert.isFalse(StringUtils.isEmpty(contextDTO.getScheduledMachines()), I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.scheduledMachinesEmpty"));
        // 判断结构是否为空
        Assert.isFalse(StringUtils.isEmpty(contextDTO.getStructureName()), I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.structureNameEmpty"));
    }

    @Override
    public void saveAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isEmpty(contextDTO.getAdjustDetailList())) {
            return;
        }
        List<MpAdjustStructureOut> adjustStructureOutList = baseDao.saveWithQuery(BeanUtil.copyToList(contextDTO.getAdjustDetailList(), MpAdjustStructureOut.class));
        List<MpAdjustDetailVo> resultList = BeanUtil.copyToList(adjustStructureOutList, MpAdjustDetailVo.class);
        contextDTO.setAdjustDetailList(resultList);
    }

    /**
     * 初始结构开始日\收尾日
     * @param contextDTO 周程滚动调整上下文对象
     */
    @Override
    protected void initStructureStartAndEndDay(MpRollAdjustContextDTO contextDTO){
        int beginDay = FactoryConstant.MONTH_MAX_DAY;
        int endDay = 0;
        List<MpStructureAllocation> structureAllocationList = contextDTO.getOneStructureAllocationList();
        if (PubUtil.isNotEmpty(structureAllocationList)){
            // 取最大的成型机收尾日作为结构的收尾日
            for (MpStructureAllocation allocation:structureAllocationList){
                if (beginDay > allocation.getBeginDay()){
                    beginDay = allocation.getBeginDay();
                }
                if (endDay < allocation.getEndDay()){
                    endDay = allocation.getEndDay();
                }
            }
        }

        contextDTO.setStructureStartDay(beginDay);
        contextDTO.setStructureDeadLine(endDay);
        //若结构收尾日小于锁定日，提示
        if (contextDTO.getStructureDeadLine() <= contextDTO.getLockEndDay()){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.adjustDayLtLockEndDay"),
                    contextDTO.getStructureDeadLine(),contextDTO.getLockEndDay()));
        }
    }
}
