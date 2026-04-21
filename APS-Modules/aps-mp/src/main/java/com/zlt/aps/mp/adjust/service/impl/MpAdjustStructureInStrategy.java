package com.zlt.aps.mp.adjust.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.enums.DataSourceEnum;
import com.zlt.aps.common.core.utils.ThreadPoolManager;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.mp.api.domain.vo.AdjustStructureOrderVo;
import com.zlt.aps.mp.engine.adjust.AdjustStructureOrderSorter;
import com.zlt.aps.mp.engine.capacity.MpAdjustDailyCapacityLimit;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.scheduling.matching.MatchingAdjuestProductionHandler;
import com.zlt.aps.mp.engine.scheduling.matching.MatchingProductionHandler;
import com.zlt.aps.mp.engine.adjust.MpWeekRollAdjustEngine;
import com.zlt.aps.mp.adjust.service.IMpAdjustStructureInService;
import com.zlt.aps.mp.api.annotation.WeekAdjustType;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.entity.MpTrialPlan;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.mp.api.domain.vo.MpAdjustDetailVo;
import com.zlt.aps.mp.api.enums.WeekAdjustTypeEnum;
import com.zlt.aps.mp.common.utils.StringUtil;
import com.zlt.common.utils.PubUtil;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 结构内调整策略
 * @author wengpc
 */
@Slf4j
@Service
@WeekAdjustType(adjustType = WeekAdjustTypeEnum.STRUCTURE_IN)
public class MpAdjustStructureInStrategy extends AbstractBaseWeekAdjustService {

    @Autowired
    private IMpAdjustStructureInService mpAdjustStructureInService;

    @Autowired
    private MatchingAdjuestProductionHandler matchingAdjuestProductionHandler;

    @Override
    public void doGenerateAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        // 1、设置版本号
        setVersion(contextDTO, BusiConstant.WeekRollAdjust.VERSION_PREFIX);
        // 2、构建结构内调整明细
        List<MpAdjustDetailVo> adjustDetailList = buildAdjustDetailList(contextDTO);
        // 3、构建结构内调整明细（试制量试计划）
        List<MpAdjustDetailVo> adjustDetailByTrialList = buildAdjustDetailByTrialList(contextDTO);
        // 4、构建结构内调整明细（月度计划有，无订单）
        List<MpAdjustDetailVo> adjustDetailByMonthPlanList = buildAdjustDetailByMonthPlanList(contextDTO);
        // 结构内调整明细结果列表
        List<MpAdjustDetailVo> resultList = new ArrayList<>();
        resultList.addAll(adjustDetailByTrialList);
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

        // 检查有错误的信息
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
        // 5、通过结构过滤调整明细
        filterAdjustDetailList(contextDTO,resultList);
        // 未获取到调整记录，抛出异常
        Assert.isFalse(PubUtil.isEmpty(resultList), () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindAdjustDetailList"),
                    contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 6、按照结构、物料编码维度进行分组，并汇总订单量
        resultList = sumByStructureAndMaterial(resultList, Boolean.FALSE);
        contextDTO.setAdjustDetailList(resultList);
        // 7、设置是否特殊材料
        setHasSpecialMaterial(contextDTO);
        // 8、设置净需求
        setCurrentNetQty(contextDTO);
        // 9、设置型腔、活块数量
        setMoldCavityInsert(contextDTO);
        // 10、设置计划剩余排产量、计划已排产量、已生产量
        setMonthUnScheduledQty(contextDTO);
        // 11、筛选：|净需求 - 计划剩余排产量| > 0的数据
        filterAdjustList(contextDTO.getAdjustDetailList());
        // 筛选后数据为空，抛出异常
        Assert.isFalse(PubUtil.isEmpty(contextDTO.getAdjustDetailList()), () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notMatchAdjustDetailList"), contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 12、设置其他字段
        setOtherField(contextDTO);
    }

    @Override
    public void doAutoAdjust(MpRollAdjustContextDTO contextDTO) {
        //注：结构内自动调整列表：关单直接排除，同时取订单列表与月计划最大并集；
        //结构内调整记录
        contextDTO.setMpAdjustStructureInList(getAdjustDataWithDoOddEven(mpAdjustStructureInService.selectMpAdjustStructureInList(contextDTO)));
        //1.结构内订单调整记录空检查
        if (PubUtil.isEmpty(contextDTO.getMpAdjustStructureInList())){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.orderAdjustRecordNotFound"),
                    contextDTO.getMpYear(),contextDTO.getMpMonth()));
        }

        //2.按结构序列化分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> mpProdFinalMap =  convertToMap(contextDTO.getFactoryMonthPlanProdFinalList());
        Map<String, List<MpAdjustStructureIn>> adjustStructInMap = contextDTO.getMpAdjustStructureInList().stream().collect(Collectors.groupingBy(item->item.getStructureName()));
        List<FactoryMonthPlanFinalAdjustVo> newMpFinalList = Collections.synchronizedList(new ArrayList<>());
        List<FactoryMonthPlanFinalAdjustVo> newMpLogList = Collections.synchronizedList(new ArrayList<>());
        List<MpMonthPlanStatistics> monthPlanStatisticsResultList = Collections.synchronizedList(new ArrayList<>());
        ///List<Future> futureList = new ArrayList<>();
        MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();
        String structureCondi = contextDTO.getStructureName();
        List<AdjustStructureOrderVo> structureOrderVoList = getAdjustStructureOrderList(contextDTO.getMpAdjustStructureInList());
        for (AdjustStructureOrderVo structureVo : structureOrderVoList){
        //for (Map.Entry<String, List<MpAdjustStructureIn>> entry : adjustStructInMap.entrySet()) {

            if (!StringUtil.isEmptyWithTrim(structureCondi)){
                //若传进来的结构有称有值，则按此结构调整
                if (!structureVo.getStructureName().equals(structureCondi)){
                    continue;
                }
            }

            final String currentStructureName = structureVo.getStructureName();
            final List<MpAdjustStructureIn> currentAdjustList = new ArrayList<>(adjustStructInMap.get(currentStructureName));
            //Future future = ThreadPoolManager.getInstance().submit(() -> {
                //2.1 初始结构上下文
                MpRollAdjustContextDTO copyContextDTO = copyContext(contextDTO,currentStructureName);
                List<FactoryMonthPlanFinalAdjustVo> oneStructMpFinalList = new ArrayList<>(mpProdFinalMap.get(copyContextDTO.getStructureName()) == null ? new ArrayList<>():mpProdFinalMap.get(currentStructureName));
                // 初始OEM标识
                initOemFlag(copyContextDTO,oneStructMpFinalList);
                if (YesOrNoEnum.YES.getCode().equals(currentAdjustList.get(0).getHasSpecialMaterial())){
                    //若是特殊结构,预存特殊结构的总实际排产量
                    setSpecStructureTotalQty(copyContextDTO,oneStructMpFinalList);
                }
                //2.2 执行结构内调整
                Date startTime = new Date();
                copyContextDTO.getLogDetail().append(String.format("结构:%s,自动调整,开始时间:%s",currentStructureName, DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
                weekRollAdjustEngine.doStructureInForOne(copyContextDTO,adjustStructInMap.get(currentStructureName), oneStructMpFinalList);
                Date endTime = new Date();
                copyContextDTO.getLogDetail().append(String.format("结构:%s,自动调整,结束时间:%s,总耗时:%s毫秒",currentStructureName, DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

                //2.3.在搭配排产前，重算每日产能限制，包括硫化机台数、胎胚种类数
                MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
                reCalcAdjustDailyCapacityLimit(copyContextDTO, oneStructMpFinalList,adjustDailyCapacityLimitObj);
                //2.4.检查结构是否可提前收尾, 不通过 = 执行搭配排产 通过 = 不执行搭配排产
                if (!weekRollAdjustEngine.checkStructurePreClose(copyContextDTO)){
                    //2.5 执行结构内搭配排产,特殊结构总计划量：contextDTO.getSpecStructureTotalQty()
                    //=========================================================
                    matchingAdjuestProductionHandler.structureAdjuestBoots(copyContextDTO, oneStructMpFinalList); // 补量
                    matchingAdjuestProductionHandler.matchingAdjustProduction(copyContextDTO, oneStructMpFinalList, true); // 搭配
                    //=========================================================
                    //2.6.在搭配排产后，重算每日产能限制，包括硫化机台数、胎胚种类数
                    reCalcAdjustDailyCapacityLimit(copyContextDTO, oneStructMpFinalList,adjustDailyCapacityLimitObj);
                }
               //2.7.设置模具变化信息
                for (FactoryMonthPlanFinalAdjustVo mpFinalVo:oneStructMpFinalList){
                    weekRollAdjustEngine.setMouldChangeInfo(adjustDailyCapacityLimitObj,copyContextDTO.getParamMap(),copyContextDTO.getStructureStartDay(),mpFinalVo,copyContextDTO.getDailyCapacityLimitVoMap());
                }

                newMpFinalList.addAll(oneStructMpFinalList);
                newMpLogList.addAll(copyContextDTO.getAdjustProcLogList());

                //2.8 构建月计划统计结果
                MpMonthPlanStatistics monthPlanStatisticsVo = buildMonthPlanStatistics(copyContextDTO, oneStructMpFinalList, YesOrNoEnum.YES.getCode());
                monthPlanStatisticsResultList.add(monthPlanStatisticsVo);

                //2.9 保存调整日志
                saveMpAdjustLog(copyContextDTO);

                //return currentStructureName;
            //});
            //futureList.add(future);
        }

        /*futureList.forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {
                log.error("线程执行异常", e);
                // 获取原始异常
                Throwable cause = e.getCause();
                String errorMsg = cause != null ? cause.getMessage() : e.getMessage();
                // 只抛出消息，不带类名
                throw new BusinessException(errorMsg);
            }
        });*/
        contextDTO.setSaveMpProdFinalList(newMpFinalList);
        contextDTO.setSaveAdjustProcLogList(newMpLogList);
        contextDTO.setMonthPlanStatisticsList(monthPlanStatisticsResultList);
    }

    /**
     * 获取结构内调整排序列表
     * @param mpAdjustStructureInList
     * @return
     */
    private List<AdjustStructureOrderVo> getAdjustStructureOrderList(List<MpAdjustStructureIn> mpAdjustStructureInList){
        if (PubUtil.isEmpty(mpAdjustStructureInList)){
            return null;
        }
        AdjustStructureOrderVo structureOrderVo;
        Map<String,AdjustStructureOrderVo> structureOrderMap = new HashMap<>();
        for (MpAdjustStructureIn structureIn : mpAdjustStructureInList){
            structureOrderVo = structureOrderMap.get(structureIn.getStructureName());
            if (structureOrderVo == null){
                structureOrderVo = new AdjustStructureOrderVo();
                structureOrderVo.setStructureName(structureIn.getStructureName());
                structureOrderVo.setHeightPriorityCount(0);
                structureOrderVo.setMouldLimitCount(0);
                structureOrderVo.setHasSpecialMaterial(YesOrNoEnum.NO.getCode());
            }
            // 统计高优先级的个数
            if (structureIn.getHeightQty() > 0){
                structureOrderVo.setHeightPriorityCount(structureOrderVo.getHeightPriorityCount() + 1);
            }
            // 统计模具受限的个数
            if (structureIn.getTypeBlockQty().equals(FactoryConstant.MOULD_LIMIT_COUNT)){
                structureOrderVo.setMouldLimitCount(structureOrderVo.getMouldLimitCount() + 1);
            }
            structureOrderVo.setHasSpecialMaterial(structureIn.getHasSpecialMaterial());
            structureOrderMap.put(structureIn.getStructureName(),structureOrderVo);
        }
        List<AdjustStructureOrderVo> structureOrderVoList = structureOrderMap.values().stream().collect(Collectors.toList());
        //排序：结构内的高优先级SKU个数多的优先 -> 模具受限的SKU个数多的优先
        AdjustStructureOrderSorter.sort(structureOrderVoList);
        return structureOrderVoList;
    }

    /**
     * 获取调整数据，并处理奇偶性
     * @param mpAdjustStructureInList
     * @return
     */
    private List<MpAdjustStructureIn> getAdjustDataWithDoOddEven(List<MpAdjustStructureIn> mpAdjustStructureInList){
        if (PubUtil.isEmpty(mpAdjustStructureInList)){
            return mpAdjustStructureInList;
        }
        for (MpAdjustStructureIn structureIn:mpAdjustStructureInList){
            if (structureIn.getConfirmAdjustQty() == null || structureIn.getConfirmAdjustQty() <= 0){
                continue;
            }
            if (ConstructionStageEnum.TRIAL_PRODUCTION.getStage().equals(structureIn.getConstructionStage()) ||
                    ConstructionStageEnum.MEASUREMENT.getStage().equals(structureIn.getConstructionStage())){
                //试制或量试，略过
                continue;
            }

            if (isEven(structureIn.getConfirmAdjustQty())){
                //偶数 + 2
                structureIn.setConfirmAdjustQty(structureIn.getConfirmAdjustQty() + ProductionConstant.ADD_LOSS_QTY_EVEN_NUMBER);
            }else{
                //奇数 + 3
                structureIn.setConfirmAdjustQty(structureIn.getConfirmAdjustQty() + ProductionConstant.ADD_LOSS_QTY_ODD_NUMBER);
            }
        }
        return mpAdjustStructureInList;
    }


    /**
     * 复制上下文副本
     * @param contextDTO
     * @return
     */
    private synchronized MpRollAdjustContextDTO copyContext(MpRollAdjustContextDTO contextDTO,String currentStructureName){
        MpRollAdjustContextDTO copyContextDTO = new MpRollAdjustContextDTO();
        BeanUtils.copyProperties(contextDTO,copyContextDTO);
        //1）结构内，按结构分别调整
        copyContextDTO.setStructureName(currentStructureName);
        copyContextDTO.setFactoryMonthPlanProdFinalList(new ArrayList<>(contextDTO.getFactoryMonthPlanProdFinalList()));
        copyContextDTO.setAdjustProcLogList(new ArrayList<>());
        copyContextDTO.setSpecStructureTotalQty(0);
        List<MpStructureAllocation> structureAllocationList = copyContextDTO.getStructureAllocationList().stream().filter(x->x.getStructureName().equals(copyContextDTO.getStructureName())).collect(Collectors.toList());
        copyContextDTO.setOneStructureAllocationList(structureAllocationList);
        //2）初始锁定日
        copyContextDTO.setLockEndDay(getLockEndDay(copyContextDTO));
        //3）初始结构开始日、收尾日
        initStructureStartAndEndDay(copyContextDTO);
        //4）初始化日志和消息
        copyContextDTO.setLogDetail(new StringBuilder());
        return copyContextDTO;
    }

    /**
     * 回填实际调整
     * @param contextDTO 周程滚动上下文
     */
    @Override
    protected void backfillRealAdjustResult(MpRollAdjustContextDTO contextDTO) {
        List<FactoryMonthPlanFinalAdjustVo> mpFinalAdjustList = contextDTO.getSaveMpProdFinalList();
        if (PubUtil.isEmpty(mpFinalAdjustList)){
            return;
        }
        // 构建 "materialCode|constructionStage" -> actualAdjustQty 映射，直接执行 UPDATE，无需先查询
        Map<String, Integer> materialStageMap = mpFinalAdjustList.stream()
                .filter(item -> item.getMaterialCode() != null && item.getConstructionStage() != null)
                .collect(Collectors.toMap(
                        item -> item.getMaterialCode() + "|" + item.getConstructionStage(),
                        item -> Convert.toInt(item.getActualAdjustQty(), 0),
                        (existing, replacement) -> existing
                ));
        if (materialStageMap.isEmpty()) {
            return;
        }
        mpAdjustStructureInEntityMapper.updateActualAdjustQtyBatch(
                contextDTO.getFactoryCode(),
                contextDTO.getMpYear(),
                contextDTO.getMpMonth(),
                contextDTO.getVersion(),
                materialStageMap
        );
    }


    /**
     * 通过结构过滤调整明细
     * @param contextDTO
     * @param adjustDetailList
     * @return
     */
    @Override
    protected void filterAdjustDetailList(MpRollAdjustContextDTO contextDTO,
                                                            List<MpAdjustDetailVo> adjustDetailList) {
        if (PubUtil.isEmpty(adjustDetailList) || PubUtil.isEmpty(contextDTO.getStructureAllocationList())) {
            return;
        }
        Set<String> structureNameSet = contextDTO.getStructureAllocationList().stream()
                .map(MpStructureAllocation::getStructureName)
                .collect(Collectors.toSet());

        CollUtil.filter(adjustDetailList, item -> structureNameSet.contains(item.getStructureName()));

        // 排除手动新增且在月计划中不存在的结构
        Set<String> monthStructureNameSet = contextDTO.getFactoryMonthPlanProdFinalList().stream()
                .filter(vo -> StringUtils.isNotEmpty(vo.getStructureName()))
                .map(FactoryMonthPlanFinalAdjustVo::getStructureName)
                .collect(Collectors.toSet());

        Set<String> addStructureNameSet = contextDTO.getStructureAllocationList().stream()
                .filter(vo -> DataSourceEnum.HAND.getCode().equals(vo.getDataSource()))
                .map(MpStructureAllocation::getStructureName)
                .collect(Collectors.toSet());

        CollUtil.filter(adjustDetailList, item -> !addStructureNameSet.contains(item.getStructureName())
                || (addStructureNameSet.contains(item.getStructureName()) && monthStructureNameSet.contains(item.getStructureName())));

    }


    @Override
    public void specialInit(MpRollAdjustContextDTO contextDTO) {

    }

    @Override
    public void specialCheck(MpRollAdjustContextDTO contextDTO) {

    }


    /**
     * 构建结构内调整明细（试制量试计划）
     * @param contextDTO
     * @return
     */
    private List<MpAdjustDetailVo> buildAdjustDetailByTrialList(MpRollAdjustContextDTO contextDTO) {
        // 试制量试计划列表
        List<MpTrialPlan> trialPlanList = contextDTO.getMpTrialPlanList();
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 结果集初始化
        List<MpAdjustDetailVo> resultList = new ArrayList<>();
        // 列表为空则直接返回空结果
        if (PubUtil.isEmpty(trialPlanList)) {
            return resultList;
        }
        // 生产计划列表按照物料编码进行分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap = monthPlanProdList.stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
        // 遍历试制量试计划列表，匹配生产计划
        for (MpTrialPlan trialPlan : trialPlanList) {
            String materialCode = trialPlan.getMaterialCode();
            // 物料编码为空则跳过
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            matchMonthPlanList(contextDTO,resultList,materialCode,monthPlanMap,
                    Convert.toInt(trialPlan.getTrialQty(),0), ApsConstant.TRUE, trialPlan.getId());
        }
        return resultList;
    }

    /**
     * 筛选：|净需求 - 计划剩余排产量| > 0的数据
     * @param adjustList
     */
    private void filterAdjustList(List<MpAdjustDetailVo> adjustList) {
        if (PubUtil.isEmpty(adjustList)) {
            return;
        }
        adjustList.removeIf(adjust -> {
            Integer currentNetQty = Convert.toInt(adjust.getCurrentNetQty(),0);
            Integer monthUnScheduledQty = Convert.toInt(adjust.getMonthUnScheduledQty(),0);
            boolean isOnlyConventionReserveHasValue = isOnlyConventionReserveHasValue(adjust);
            return (Math.abs(currentNetQty - monthUnScheduledQty) == 0) || isOnlyConventionReserveHasValue;
        });
    }

    @Override
    public void saveAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isEmpty(contextDTO.getAdjustDetailList())) {
            return;
        }
        List<MpAdjustDetailVo> resultList = baseDao.saveWithQuery(contextDTO.getAdjustDetailList());
        contextDTO.setAdjustDetailList(resultList);
    }

}
