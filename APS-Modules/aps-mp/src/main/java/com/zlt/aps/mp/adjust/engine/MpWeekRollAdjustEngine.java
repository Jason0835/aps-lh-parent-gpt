package com.zlt.aps.mp.adjust.engine;

import cn.hutool.core.convert.Convert;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.enums.UrgencyTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.mp.common.utils.PubUtil;
import com.zlt.aps.mp.common.utils.StringUtil;
import com.zlt.aps.mp.engine.capacity.MpAdjustDailyCapacityLimit;
import com.zlt.aps.mp.engine.check.DayTotalCapacityChecker;
import com.zlt.aps.mp.engine.check.SkuSecondChecker;
import com.zlt.aps.mp.engine.deduct.DeductMouldScheduler;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.deduct.DailyScheduleVo;
import com.zlt.aps.mp.api.domain.deduct.DeductMouldVo;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureOut;
import com.zlt.aps.mp.api.domain.vo.DailyMouldAvailabilityResult;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sandy
 * @version 1.0
 * @Description 周程滚动调整引擎
 * @date 2025/12/19
 */
@Slf4j
@Service
public class MpWeekRollAdjustEngine {

    private static final String Z_K_H = "\\{}";

    /**
     * 结构内调整，按结构分别调整
     * @param contextDTO 周程滚动调整上下文
     * @param mpAdjustStructureInList 结构内调整记录列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    public void doStructureInForOne(MpRollAdjustContextDTO contextDTO, List<MpAdjustStructureIn> mpAdjustStructureInList, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        //1.解析出关单/减量、在产SKU、新增SKU以及暂缓
        List<MpAdjustStructureIn> deductAdjustList = new ArrayList<>();
        List<MpAdjustStructureIn> onIncrementAdjustList = new ArrayList<>();
        List<MpAdjustStructureIn> incrementAdjustList = new ArrayList<>();
        List<MpAdjustStructureIn> trialAdjustList = new ArrayList<>();
        if (PubUtil.isEmpty(mpProdFinalList)) {
            mpProdFinalList = new ArrayList<>();
        }
        List<String> onMaterialCodeList = mpProdFinalList.stream().map(x -> x.getMaterialCode()).collect(Collectors.toList());
        Date startTime,endTime;
        StringBuffer sbError = new StringBuffer();
        for (MpAdjustStructureIn adjustStructureIn:mpAdjustStructureInList){
            //1.0 检查日硫化量及主花纹
            checkDayLhQtyWithMainPattern(sbError,adjustStructureIn);

            if (ConstructionStageEnum.MEASUREMENT.getStage().equals(adjustStructureIn.getConstructionStage())){
                if (adjustStructureIn.getConfirmAdjustQty() > 0){
                    trialAdjustList.add(adjustStructureIn);
                }
                continue;
            }
            if (adjustStructureIn.getConfirmAdjustQty() < 0){
                //1.1 减量
                deductAdjustList.add(adjustStructureIn);
            }
            if (adjustStructureIn.getConfirmAdjustQty() > 0){
                //1.2 增量
                if (onMaterialCodeList.indexOf(adjustStructureIn.getMaterialCode())>=0){
                    //在机SKU增量
                    onIncrementAdjustList.add(adjustStructureIn);
                }else{
                    //新增SKU
                    incrementAdjustList.add(adjustStructureIn);
                }
            }

        }
        if (!StringUtil.isEmptyWithTrim(sbError.toString())){
            throw new BusinessException(sbError.toString());
        }
        //2.减量调整
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        doStructureInWithDeduct(contextDTO,deductAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //3、初始日产能限制
        // 锁定次日 作为 可开始日
        int lockNextDay = contextDTO.getLockEndDay() + 1;
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = new MpAdjustDailyCapacityLimit().getDailyCapacityLimitMap(contextDTO.getStructureStartDay(),mpProdFinalList,contextDTO.getOneStructureAllocationList());
        initDayProductionInfo(contextDTO,dailyCapacityLimitVoMap);
        contextDTO.setDailyCapacityLimitVoMap(ObjectUtils.defaultIfNull(dailyCapacityLimitVoMap, new HashMap<>()));
        //4、拆出搭配量，用于快速判断是否搭配
        StringBuilder beginDaySb = new StringBuilder();
        mpProdFinalList.stream().forEach(x->{
            if (x.getBeginDay() == null){
                beginDaySb.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecord.notBeginDay"),
                        x.getMaterialCode())).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
            }
            if (x.getConventionProductionQty() >0) {
                splitMatchQtyByDay(contextDTO,x.getConventionProductionQty(), lockNextDay,x);
            }
        });
        if (!StringUtil.isEmptyWithTrim(beginDaySb.toString())){
            throw new BusinessException(beginDaySb.toString());
        }
        //5.在机SKU增量
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        doStructureInWithOnlineInc(contextDTO,onIncrementAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //6.新增SKU
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        doStructureInWithNewSku(contextDTO,incrementAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //7.优化：其他SKU往前移动
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        moveForwardWithOtherSku(contextDTO,lockNextDay,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //8.试制
        //1）试制不受胎胚种类数\机台数的限制；
        //2）试制是紧急的，可以在锁定期内插单；普通的，在锁定期外1天插单；
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【试制排产】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        doStructureInWithTrial(contextDTO,trialAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【试制排产】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

    }

    /**
     * 初始化日产信息（包括日最大排产量、开停产标识、日产比例）
     * @param contextDTO
     * @param dailyCapacityLimitVoMap
     */
    private void initDayProductionInfo(MpRollAdjustContextDTO contextDTO,Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap){
        Map<Integer, MdmWorkCalendar> workCalendarMap = contextDTO.getWorkCalendarMap();
        if (PubUtil.isEmpty(dailyCapacityLimitVoMap) || PubUtil.isEmpty(workCalendarMap)){
            return;
        }
        Integer dayMaxCapacity = (Integer) contextDTO.getParamMap().get(MonthPlanEnums.DAY_MAX_CAPACITY.getCode());
        MpDailyCapacityLimitVo dailyCapacityLimitVo;
        MdmWorkCalendar workCalendar;
        for (Map.Entry<Integer, MpDailyCapacityLimitVo> entry : dailyCapacityLimitVoMap.entrySet()) {
            dailyCapacityLimitVo = entry.getValue();
            workCalendar = workCalendarMap.get(entry.getKey());
            if (workCalendar == null){
                continue;
            }
            dailyCapacityLimitVo.setDayOpenCloseFlag(workCalendar.getDayFlag());
            dailyCapacityLimitVo.setDayProductionRate(workCalendar.getRate());
            //日最大排产量 = 日最大产能*比率/100
            dailyCapacityLimitVo.setMaxDayProductionQty(dayMaxCapacity*workCalendar.getRate()/100);
            //若前日是停产，今日第1天开产，即开产首日，则仍按正常产能分配
            dailyCapacityLimitVo.setOpenProductionFirstDay(isOpenProductionFirstDay(workCalendarMap,entry.getKey()));
            if (dailyCapacityLimitVo.isOpenProductionFirstDay()){
                dailyCapacityLimitVo.setMaxDayProductionQty(dayMaxCapacity);
            }
        }
    }

    /**
     * 检查是否开产首日
     * @param workCalendarMap 日历Map
     * @param checkDay 检查日
     * @return true--开产首日，false--不是开产首日
     */
    private boolean isOpenProductionFirstDay(Map<Integer, MdmWorkCalendar> workCalendarMap,int checkDay){
        int preDay = checkDay - 1;
        preDay = preDay < FactoryConstant.MONTH_START_DAY ? FactoryConstant.MONTH_START_DAY:preDay;
        return !YesOrNoEnum.YES.getCode().equals(workCalendarMap.get(preDay).getDayFlag()) &&
                YesOrNoEnum.YES.getCode().equals(workCalendarMap.get(checkDay).getDayFlag());
    }

    /**
     * 检查是否自动补量
     * @param paramMap 参数Map
     * @param iDay 检查天
     * @param mpFinalVo 定稿Vo
     * @return true--自动补；false--不自动补
     */
    private boolean checkAutoReplenishment(Map<String,Object> paramMap,Integer iDay,FactoryMonthPlanFinalAdjustVo mpFinalVo){
        //1. 检查排产分类，是否主销或常规，退出
        String productionType = (String)paramMap.get(MonthPlanEnums.BOOST_PRODUCTION_TYPE_VALUE.getCode());
        if (StringUtil.isEmptyWithTrim(productionType)){
            return false;
        }
        List<String> productionTypeList = Arrays.asList(productionType.split(","));
        if (productionTypeList.indexOf(mpFinalVo.getProductionType())<0){
            return false;
        }
        //2. 检查自动补量天数
        Integer boostDay = (Integer) paramMap.get(MonthPlanEnums.MAX_BOOST_DAY.getCode());
        if (boostDay == null){
            return false;
        }
        // 起始日 = 月底最后1天 - 补量天数;
        int startDay = getLastDayNumberOfMonth(LocalDate.of(mpFinalVo.getYear(), mpFinalVo.getMonth(), 1)) - boostDay;
        return iDay > startDay;
    }

    /**
     * 获取指定日期的月份最后一天号数
     */
    private int getLastDayNumberOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
    }

    /**
     * 结构内调整，试制排产
     * @param contextDTO 周程滚动调整上下文
     * @param trialAdjustList 试制排产列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void doStructureInWithTrial(MpRollAdjustContextDTO contextDTO,
                                        List<MpAdjustStructureIn> trialAdjustList,
                                        List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if (PubUtil.isEmpty(trialAdjustList)) {
            return;
        }
        //1、排序：按紧急程度/普通程度
        trialAdjustList = trialAdjustList.stream().sorted(Comparator.comparing(MpAdjustStructureIn::getUrgencyType,Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        Integer newOnlineDay;
        FactoryMonthPlanFinalAdjustVo mpFinalVo;
        String dayField;
        for (MpAdjustStructureIn structureIn:trialAdjustList){
            // 设置 试制计划量
            mpFinalVo = createMpFinalAdjustVo(contextDTO, structureIn);
            if (UrgencyTypeEnum.URGENCY.getValue().equals(structureIn.getUrgencyType())){
                //紧急,可以从调整日开始
                newOnlineDay = getTrialNewOnlineDay(contextDTO,contextDTO.getAdjustDay(),contextDTO.getStructureDeadLine(),mpProdFinalList);
            }else {
                //紧急,可以从锁定次日工始
                newOnlineDay = getTrialNewOnlineDay(contextDTO,contextDTO.getLockEndDay()+1,contextDTO.getStructureDeadLine(),mpProdFinalList);
            }
            if (newOnlineDay == null){
                contextDTO.getLogDetail().append(String.format("结构:%s,【试制排产】,物料编码:%s,没有获取到可上机日,可能是日限制个数或周日或结构起产日的约束!",contextDTO.getStructureName(), structureIn.getMaterialCode())).append(ApsConstant.DIVISION);
                //提示：试制物料编码:%s,没有获取到有效的上机日,可能是周日、结构起产日或日限制个数达到限制！
                addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.trial.noProduction"),mpFinalVo.getMaterialCode()));
                continue;
            }
            // 设置 试制计划量
            dayField = FactoryConstant.DAY_FIELD + newOnlineDay;
            mpFinalVo.setFieldValueByFieldName(dayField,structureIn.getConfirmAdjustQty());
            mpFinalVo.setTrialProductionQty(structureIn.getConfirmAdjustQty());
            mpProdFinalList.add(mpFinalVo);
            contextDTO.getFactoryMonthPlanProdFinalList().add(mpFinalVo);
            //重置开始日\结束日\汇总值
            resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),mpFinalVo);
            contextDTO.getLogDetail().append(String.format("结构:%s,【试制排产】,物料编码:%s,排产上机日:%s,排产量:%s!",contextDTO.getStructureName(), structureIn.getMaterialCode(), newOnlineDay,structureIn.getConfirmAdjustQty())).append(ApsConstant.DIVISION);
            //提示：试制物料编码:%s,排产日:%s,排产量:%s！
            addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.trial.productionLog"),mpFinalVo.getMaterialCode(),newOnlineDay,mpFinalVo.getTotalQty()));
        }
    }

    /**
     * 添加调整过程日志
     * @param contextDTO 周程滚动上下文
     * @param mpFinalVo 当前定稿Vo
     * @param procLog 过程日志信息
     */
    private void addAdjustProcLog(MpRollAdjustContextDTO contextDTO,FactoryMonthPlanFinalAdjustVo mpFinalVo,String procLog){
        mpFinalVo.getAdjustDetail().append(procLog).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
        contextDTO.getAdjustProcLogList().removeIf(item->item.getMaterialCode().equals(mpFinalVo.getMaterialCode()));
        contextDTO.getAdjustProcLogList().add(mpFinalVo);
    }

    /**
     * 获取试制/量试新的上机日
     * @param contextDTO 周程滚动调整上下文
     * @param startDay 开始日
     * @param endDay 结束日
     * @param mpProdFinalList
     * @return
     */
    private Integer getTrialNewOnlineDay(MpRollAdjustContextDTO contextDTO,Integer startDay, Integer endDay, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList){
        String dayField;
        int iCount;
        startDay = startDay <= 0 ? FactoryConstant.MONTH_START_DAY:startDay;
        //试制、量试SKU单日上限的数量
        int upLimit = (Integer) contextDTO.getParamMap().get(MonthPlanEnums.TRIAL_SKU_SINGLE_DAY_QTY_UP_LIMIT.getCode());
        //试制、量试SKU在结构起产日是否允许排产
        String isStartDayStr = (String)contextDTO.getParamMap().get(MonthPlanEnums.TRIAL_SKU_STRUCT_START_DAY_IS_PRODUCTION.getCode());
        //试制、量试SKU在周日是否允许排产
        String isSunDayStr = (String)contextDTO.getParamMap().get(MonthPlanEnums.TRIAL_SKU_SUNDAY_IS_PRODUCTION.getCode());
        for (int i = startDay; i <= endDay; i++){
            if (!FactoryConstant.YES_VALUE.equals(isStartDayStr) && contextDTO.getStructureStartDay() == i){
                //1、若试制、量试SKU在结构起产日不允许排产，则需排除
                continue;
            }
            if (!FactoryConstant.YES_VALUE.equals(isSunDayStr) && isDayOfMonthSunday(i,contextDTO.getMpMonth(),contextDTO.getMpYear())){
                //2、若试制、量试SKU在周日不允许排产，则需排除
                continue;
            }

            //3、试制、量试SKU单日上限的数量限制
            iCount = 0;
            for (FactoryMonthPlanFinalAdjustVo finalAdjustVo:mpProdFinalList){
                dayField = FactoryConstant.DAY_FIELD + i;
                if (finalAdjustVo.getFieldValueByFieldName(dayField) != null) {
                    if (ConstructionStageEnum.MEASUREMENT.getStage().equals(finalAdjustVo.getConstructionStage()) ||
                            ConstructionStageEnum.TRIAL_PRODUCTION.getStage().equals(finalAdjustVo.getConstructionStage())){
                        iCount +=1;
                    }
                }
            }
            if (iCount < upLimit){
                return i;
            }
        }
        return null;
    }

    /**
     * 判断指定年月的指定日是否为周日
     * @param dayOfMonth 日期（1-31）
     * @param month 月份（1-12）
     * @param year 年份
     * @return 如果是周日返回true
     */
    private boolean isDayOfMonthSunday(int dayOfMonth, int month, int year) {
        LocalDate date = LocalDate.of(year, month, dayOfMonth);
        return date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /**
     * 结构调整-结构缩短/延长
     * @param contextDTO 周程滚动调整上下文
     * @param mpAdjustStructureOutList 结构调整记录列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    public void doStructureOutForOne(MpRollAdjustContextDTO contextDTO, List<MpAdjustStructureOut> mpAdjustStructureOutList, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        //1.解析出关单/减量、在产SKU、新增SKU以及暂缓
        List<MpAdjustStructureOut> deductAdjustList = new ArrayList<>();
        List<MpAdjustStructureOut> onIncrementAdjustList = new ArrayList<>();
        List<MpAdjustStructureOut> incrementAdjustList = new ArrayList<>();
        List<String> onMaterialCodeList = new ArrayList<>();
        if (PubUtil.isNotEmpty(mpProdFinalList)){
            onMaterialCodeList = mpProdFinalList.stream().map(x->x.getMaterialCode()).collect(Collectors.toList());
        }
        Date startTime,endTime;
        StringBuffer sbError = new StringBuffer();
        for (MpAdjustStructureOut adjustStructureOut:mpAdjustStructureOutList){
            //1.0 检查日硫化量
            checkDayLhQtyWithMainPattern(sbError,adjustStructureOut);

            if (adjustStructureOut.getConfirmAdjustQty() < 0){
                //1.1 减量
                deductAdjustList.add(adjustStructureOut);
            }
            if (adjustStructureOut.getConfirmAdjustQty() > 0){
                //1.2 增量
                if (onMaterialCodeList.indexOf(adjustStructureOut.getMaterialCode())>=0){
                    //在机SKU增量
                    onIncrementAdjustList.add(adjustStructureOut);
                }else{
                    //新增SKU
                    incrementAdjustList.add(adjustStructureOut);
                }
            }

        }
        if (!StringUtil.isEmptyWithTrim(sbError.toString())){
            throw new BusinessException(sbError.toString());
        }
        //2.减量调整
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        doStructureOutWithDeduct(contextDTO,deductAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //3、初始日产能限制
        // 锁定次日 作为 可开始日
        int lockNextDay = contextDTO.getLockEndDay() + 1;
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = new MpAdjustDailyCapacityLimit().getDailyCapacityLimitMap(contextDTO.getStructureStartDay(),mpProdFinalList,contextDTO.getOneStructureAllocationList());
        // 初始日最大排产量、开停产标识、比例
        initDayProductionInfo(contextDTO,dailyCapacityLimitVoMap);
        contextDTO.setDailyCapacityLimitVoMap(ObjectUtils.defaultIfNull(dailyCapacityLimitVoMap, new HashMap<>()));
        //4、拆出搭配量，用于快速判断是否搭配
        mpProdFinalList.stream().forEach(x->{
            if (x.getConventionProductionQty() >0) {
                splitMatchQtyByDay(contextDTO,x.getConventionProductionQty(), lockNextDay,x);
            }
        });

        //5.在机SKU增量
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        doStructureOutWithOnlineInc(contextDTO,onIncrementAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //6.新增SKU
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        doStructureOutWithNewSku(contextDTO,incrementAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //7.优化：其他SKU往前移动
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        //结构间调整，锁定次日有可能小于结构起产日，将其调到结构起产日
        int startDay = lockNextDay < contextDTO.getStructureStartDay() ? contextDTO.getStructureStartDay():lockNextDay;
        moveForwardWithOtherSku(contextDTO,startDay,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);
    }

    /**
     * 检查日硫化量及主花纹是否为空
     * @param sbError
     * @param structureIn
     */
    private void checkDayLhQtyWithMainPattern(StringBuffer sbError, MpAdjustStructureIn structureIn){
        if (structureIn.getDayVulcanizationQty() == null || structureIn.getDayVulcanizationQty() == 0){
            sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecord.notDayLhQty"),
                    structureIn.getMaterialCode())).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
        }
        if (StringUtil.isEmptyWithTrim(structureIn.getMainPattern())){
            sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecord.notMainPattern"),
                    structureIn.getMaterialCode())).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
        }
    }

    /**
     * 检查日硫化量及主花纹是否为空
     * @param sbError
     * @param structureOut
     */
    private void checkDayLhQtyWithMainPattern(StringBuffer sbError, MpAdjustStructureOut structureOut){
        if (structureOut.getDayVulcanizationQty() == null || structureOut.getDayVulcanizationQty() == 0){
            sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecord.notDayLhQty"),
                    structureOut.getMaterialCode())).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
        }
        if (StringUtil.isEmptyWithTrim(structureOut.getMainPattern())){
            sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecord.notMainPattern"),
                    structureOut.getMaterialCode())).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
        }
    }

    /**
     * 优化：其他SKU往前移动
     * @param contextDTO 调整上下文
     * @param lockNextDay 锁定次日
     * @param mpProdFinalList 定稿列表
     */
    private void moveForwardWithOtherSku(MpRollAdjustContextDTO contextDTO, int lockNextDay,
                                         List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList){
        //1、从锁定次日向后遍历排产计划，检测每日硫化机台数不超限制数且每日胎胚种类数不超限制数的日期，记为有空间的日期；
        //2、在有空间的日期向后依次找SKU，越靠近的SKU优先移动；
        //3、将SKU整体模拟往前移动到空间日期，并向后再次检测每日硫化机台数、胎胚种类数的符合性，若符合，则可以移动，否则不能移动，继续找下一个SKU；
        int secStartDay;
        List<FactoryMonthPlanFinalAdjustVo> canMoveFinalList;
        FactoryMonthPlanFinalAdjustVo bakMpFinalVo;
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = contextDTO.getDailyCapacityLimitVoMap();
        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
        //从锁定次日到结构收尾日，依次遍历
        for (int i = lockNextDay; i<= contextDTO.getStructureDeadLine(); i++){

            //1、从第2天开始查找SKU
            secStartDay = i+1;
            canMoveFinalList = findCanMoveSkuList(mpProdFinalList,secStartDay);
            if (getTrialNewOnlineDay(contextDTO,i,i, mpProdFinalList) == null){
                //今天不可以移动量试
                canMoveFinalList = canMoveFinalList.stream().filter(x->ConstructionStageEnum.FORMAL_PRODUCTION.getStage().equals(x.getConstructionStage())).collect(Collectors.toList());
            }
            if (PubUtil.isEmpty(canMoveFinalList)){
                //若没有可以移动的列表，则退出
                contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,排产日:%s,排产次日:%s查找可移动的SKU,未找到退出！",contextDTO.getStructureName(),i,secStartDay)).append(ApsConstant.DIVISION);
                break;
            }
            //2、移动SKU列表，直到第I天没有剩余空间
            int cavityQty;
            for (FactoryMonthPlanFinalAdjustVo finalVo:canMoveFinalList){
                adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList,i, dailyCapacityLimitVoMap.get(i), contextDTO.getParamMap(),finalVo.getMainPattern());
                contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,排产日:%s,其产能限制信息:%s！",contextDTO.getStructureName(),i,contextDTO.getDailyCapacityLimitVoMap().get(i) == null ? "" : contextDTO.getDailyCapacityLimitVoMap().get(i).toString())).append(ApsConstant.DIVISION);
                //2.1、预检查: 当前每日硫化机台数\当前每日胎胚种类数 符合性
                if (!adjustDailyCapacityLimitObj.preCheckCapacitySatisfy(contextDTO.getDailyCapacityLimitVoMap().get(i))){
                    contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,排产日:%s,每日硫化机台数或每日胎胚种类数不符合产能限制,退出！",contextDTO.getStructureName(),i)).append(ApsConstant.DIVISION);
                    continue;
                }
                //2.2、预检查：主花纹向下模具数量(/2转成机台数) 符合性
                cavityQty = getNewCavityQty(contextDTO,finalVo,i);
                contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,物料编码:%s,排产日:%s,获取到新的型腔数:%s！",contextDTO.getStructureName(),finalVo.getMaterialCode(),i,cavityQty)).append(ApsConstant.DIVISION);
                if (!preCheckMouldSatisfy(dailyCapacityLimitVoMap.get(i),cavityQty)){
                    contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,物料编码:%s,排产日:%s,主花纹:%s,其主花纹模具数不符合产能限制,退出！",contextDTO.getStructureName(),finalVo.getMaterialCode(),i,finalVo.getMainPattern())).append(ApsConstant.DIVISION);
                    continue;
                }

                bakMpFinalVo = new FactoryMonthPlanFinalAdjustVo();
                BeanUtils.copyProperties(finalVo,bakMpFinalVo);

                //2.3、清空定稿表日计划量
                clearMpFinalDayValue(contextDTO, i, finalVo);

                //2.4、增模排产
                contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】--增模排产,排产日:%s,物料编码:%s,开始！",contextDTO.getStructureName(), i,finalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                incMouldProduction(mpProdFinalList, contextDTO, i, finalVo.getTotalQty(), finalVo,bakMpFinalVo);
                contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】--增模排产,排产日:%s,物料编码:%s,结束！",contextDTO.getStructureName(), i,finalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                //重置开始日\结束日\汇总值
                resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),finalVo);
                //提示：【SKU移动】物料编码:%s,从排产日:%s,移动到排产日:%s,移动前总计划量:%s,移动后总计划量:%s！
                addAdjustProcLog(contextDTO,finalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.moveForward.moveLog"),finalVo.getMaterialCode(),bakMpFinalVo.getBeginDay(),i,bakMpFinalVo.getTotalQty(),finalVo.getTotalQty()));
                //设置移动标志，减少重复移动
                finalVo.setMoveFlag(true);
                //2.5、检查是否还有剩余空间，若没有，则退出
                if (!adjustDailyCapacityLimitObj.checkCapacitySatisfy(contextDTO.getDailyCapacityLimitVoMap().get(i))){
                    contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,排产日:%s,每日硫化机台数或每日胎胚种类数不符合产能限制,退出！",contextDTO.getStructureName(),i)).append(ApsConstant.DIVISION);
                    break;
                }
            }
        }

    }

    /**
     * 查询可以移动的SKU列表
     * @param mpProdFinalList
     * @param secStartDay 第2天可开始日
     * @return 可以移动的SKU列表
     */
    private List<FactoryMonthPlanFinalAdjustVo> findCanMoveSkuList(List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList, int secStartDay) {
        List<FactoryMonthPlanFinalAdjustVo> finalVoList = mpProdFinalList.stream().filter(x-> (!x.isMoveFlag()) && x.getBeginDay()!=null && x.getBeginDay()>=secStartDay)
                .sorted(Comparator.comparing(FactoryMonthPlanFinalAdjustVo::getBeginDay)
                .thenComparing((o1, o2) -> {
                    // 自定义比较逻辑(总的已排实单量)
                    int totalQty1 = o1.getHeightProductionQty() + o1.getMidProductionQty();
                    int totalQty2 = o2.getHeightProductionQty() + o2.getMidProductionQty();
                    return Integer.compare(totalQty2,totalQty1);
                })).collect(Collectors.toList());
       /* List<FactoryMonthPlanFinalAdjustVo> finalVoList = mpProdFinalList.stream()
                .filter(x->x.getBeginDay()!=null && secStartDay == x.getBeginDay()).sorted((o1, o2) -> {
                    // 自定义比较逻辑(总的已排实单量)
                    int totalQty1 = o1.getHeightProductionQty() + o1.getMidProductionQty();
                    int totalQty2 = o2.getHeightProductionQty() + o2.getMidProductionQty();
                    return Integer.compare(totalQty2,totalQty1);
            }).collect(Collectors.toList());*/
    /*    if (PubUtil.isNotEmpty(finalVoList)){
            return finalVoList;
        }*/
       /* if (secStartDay >= FactoryConstant.MONTH_MAX_DAY){
            //若第2天可开始日 已到月底最后1天，则退出
            return null;
        }
        // 加1天，递归查找
        return findCanMoveSkuList(mpProdFinalList,secStartDay+1);*/
        return finalVoList;
    }

    /**
     * 结构内调整：减量
     * @param contextDTO 周程滚动调整上下文
     * @param deductAdjustList 减量调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void doStructureInWithDeduct(MpRollAdjustContextDTO contextDTO,
                                         List<MpAdjustStructureIn> deductAdjustList, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if(PubUtil.isEmpty(deductAdjustList)){
            return;
        }
        //注：实单减量，先扣月计划的已排实单
        Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap = mpProdFinalList.stream().collect(Collectors.groupingBy(item->item.getMaterialCode(),
                Collectors.collectingAndThen(Collectors.toList(),m-> {
                    return m.get(0);
                })));
        FactoryMonthPlanFinalAdjustVo mpFinalVo;
        int reAdjustQty,needDeductQty;
        //1、按结构内调整记录依次匹配月计划定稿表
        for (MpAdjustStructureIn deductAdjust:deductAdjustList){
            mpFinalVo = mpProdFinalMap.get(deductAdjust.getMaterialCode());
            if (mpFinalVo == null){
                continue;
            }
            mpFinalVo.setOriTotalQty(mpFinalVo.getTotalQty());
            mpFinalVo.setHasSpecialMaterial(deductAdjust.getHasSpecialMaterial());
            //剩余调整量绝对值
            reAdjustQty =  Math.abs(deductAdjust.getConfirmAdjustQty());
            //2、先设置锁定量，再按高到中依次扣减排产量
            //设置锁定量
            setLockQty(contextDTO.getLockEndDay(),mpFinalVo);
            //允许扣减量,实单-锁定量
            int allowDeductQty = calcAllowDeductQty(mpFinalVo);
            needDeductQty = allowDeductQty >= reAdjustQty ? reAdjustQty:allowDeductQty;
            int realBillQty = getRealBillQty(mpFinalVo);
            contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,物料编码:%s,确认调整量:%s,实单量:%s,锁定量:%s,允许扣减量:%s",contextDTO.getStructureName(),deductAdjust.getMaterialCode(),reAdjustQty,realBillQty,mpFinalVo.getLockQty(),allowDeductQty)).append(ApsConstant.DIVISION);
            if (needDeductQty == 0){
                //提示：【减量】物料编码:%s，确认调整量:%s，实单量:%s，锁定量:%s，允许扣减量:%s！
                addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.zeroAdjustDeduct"),deductAdjust.getMaterialCode(),reAdjustQty,realBillQty,mpFinalVo.getLockQty(),allowDeductQty));
                continue;
            }
            doNeedDeductProductionQty(contextDTO,needDeductQty, mpFinalVo);
            //3、遍历31天日排产量，根据实际扣减量依次扣减
            deductScheduleQtyByDay(contextDTO, mpFinalVo);
            //4.重置开始日\结束日\汇总值
            resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),mpFinalVo);
        }
    }

    /**
     * 结构调整：减量
     * @param contextDTO 周程滚动调整上下文
     * @param deductAdjustList 减量调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void doStructureOutWithDeduct(MpRollAdjustContextDTO contextDTO,
                                          List<MpAdjustStructureOut> deductAdjustList, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if(PubUtil.isEmpty(deductAdjustList)){
            return;
        }
        //注：实单减量，先扣月计划的已排实单
        Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap = mpProdFinalList.stream().collect(Collectors.groupingBy(item->item.getMaterialCode(),
                Collectors.collectingAndThen(Collectors.toList(),m-> {
                    return m.get(0);
                })));
        FactoryMonthPlanFinalAdjustVo mpFinalVo;
        int reAdjustQty,needDeductQty;
        //1、按结构内调整记录依次匹配月计划定稿表
        for (MpAdjustStructureOut deductAdjust:deductAdjustList){
            mpFinalVo = mpProdFinalMap.get(deductAdjust.getMaterialCode());
            if (mpFinalVo == null){
                continue;
            }
            mpFinalVo.setOriTotalQty(mpFinalVo.getTotalQty());
            //剩余调整量绝对值
            reAdjustQty =  Math.abs(deductAdjust.getConfirmAdjustQty());
            //2、先设置锁定量，再按高到中依次扣减排产量
            //设置锁定量
            setLockQty(contextDTO.getLockEndDay(),mpFinalVo);
            //允许扣减量,实单-锁定量
            int allowDeductQty = calcAllowDeductQty(mpFinalVo);
            needDeductQty = allowDeductQty >= reAdjustQty ? reAdjustQty:allowDeductQty;
            int realBillQty = getRealBillQty(mpFinalVo);
            contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,物料编码:%s,确认调整量:%s,实单量:%s,锁定量:%s,允许扣减量:%s",contextDTO.getStructureName(),deductAdjust.getMaterialCode(),reAdjustQty,realBillQty,mpFinalVo.getLockQty(),allowDeductQty)).append(ApsConstant.DIVISION);
            if (needDeductQty == 0){
                //提示：【减量】物料编码:%s，确认调整量:%s，实单量:%s，锁定量:%s，允许扣减量:%s！
                addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.zeroAdjustDeduct"),deductAdjust.getMaterialCode(),reAdjustQty,realBillQty,mpFinalVo.getLockQty(),allowDeductQty));
                continue;
            }
            doNeedDeductProductionQty(contextDTO,needDeductQty, mpFinalVo);
            //3、遍历31天日排产量，根据实际扣减量依次扣减
            deductScheduleQtyByDay(contextDTO, mpFinalVo);
            //4、重置开始日\结束日\汇总值
            resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),mpFinalVo);
        }
    }

    /**
     * 允许扣减量  = 实单-锁定
     * @param mpFinalVo
     * @return
     */
    private int calcAllowDeductQty(FactoryMonthPlanFinalAdjustVo mpFinalVo){
        //允许扣减量  = 实单-锁定
        int allowDeductQty = getRealBillQty(mpFinalVo);
        allowDeductQty -= mpFinalVo.getLockQty();
        return allowDeductQty < 0 ? 0:allowDeductQty;
    }

    /**
     * 实单 = 高优先级+中优先级+暂缓
     * @param mpFinalVo
     * @return
     */
    private int getRealBillQty(FactoryMonthPlanFinalAdjustVo mpFinalVo){
        return mpFinalVo.getHeightProductionQty() + mpFinalVo.getMidProductionQty() + mpFinalVo.getPostponeProductionQty();
    }

    /**
     * 按需要扣减的量，分别扣减高优先级，再扣减中优先级
     * @param needDeductQty 需要扣减的量
     * @param prodFinal 定额记录
     */
    private void doNeedDeductProductionQty(MpRollAdjustContextDTO contextDTO, int needDeductQty, FactoryMonthPlanFinalAdjustVo prodFinal) {
        int tmpNeedDeductQty = needDeductQty;
        int oriRealOrdQty = prodFinal.getHeightProductionQty() + prodFinal.getMidProductionQty() + prodFinal.getPostponeProductionQty();
        //根据 需要扣减量，从高优先级->中优先级->暂缓
        if (prodFinal.getHeightProductionQty() >= tmpNeedDeductQty) {
            prodFinal.setHeightProductionQty(prodFinal.getHeightProductionQty() - tmpNeedDeductQty);
        } else {
            //高优先级不够，自身清0，继续扣减中优先级
            tmpNeedDeductQty = tmpNeedDeductQty - prodFinal.getHeightProductionQty();
            prodFinal.setHeightProductionQty(0);
            if (prodFinal.getMidProductionQty() >= tmpNeedDeductQty) {
                prodFinal.setMidProductionQty(prodFinal.getMidProductionQty() - tmpNeedDeductQty);
            }else{
                //中优先级不够，自身清0，继续扣减暂缓优先级
                tmpNeedDeductQty = tmpNeedDeductQty - prodFinal.getMidProductionQty();
                prodFinal.setMidProductionQty(0);

                if (prodFinal.getPostponeProductionQty() >= tmpNeedDeductQty) {
                    prodFinal.setPostponeProductionQty(prodFinal.getPostponeProductionQty() - tmpNeedDeductQty);
                }else {
                    prodFinal.setPostponeProductionQty(0);
                }
            }
        }

        int emptyQty = needDeductQty > oriRealOrdQty ? oriRealOrdQty:needDeductQty;
        //将调减量置到实际调整量
        prodFinal.setActualAdjustQty(emptyQty);
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】--扣减各总排产量,物料编码:%s,调减后,高优先级排产量:%s,中优级排产量:%s,暂缓排产量:%s,空出产能:%s",contextDTO.getStructureName(), prodFinal.getMaterialCode(),prodFinal.getHeightProductionQty(),prodFinal.getMidProductionQty(),prodFinal.getPostponeProductionQty(),prodFinal.getActualAdjustQty())).append(ApsConstant.DIVISION);
    }

    /**
     * 设置锁定量
     * @param lockDay 锁定量
     * @param prodFinal  定稿记录
     */
    private void setLockQty(int lockDay, FactoryMonthPlanFinalAdjustVo prodFinal) {
        String dayField;
        int lockQty = 0;
        //汇总1号到锁定日的总计划量
        for (int i = FactoryConstant.MONTH_START_DAY; i<=lockDay; i++) {
            dayField = FactoryConstant.DAY_FIELD + i;
            if (prodFinal.getFieldValueByFieldName(dayField) == null) {
                continue;
            }
            lockQty += (Integer) prodFinal.getFieldValueByFieldName(dayField);
        }
        prodFinal.setLockQty(lockQty);
    }

    /**
     * 按日扣减排产量
     * @param contextDTO
     * @param prodFinal
     */
    private int deductScheduleQtyByDay(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo prodFinal) {
        int dayQty;
        String dayField;
        int iDay = contextDTO.getLockEndDay() + 1;
        int realDeductQty = prodFinal.getActualAdjustQty();
        //实单肯定在前，从后向前扣减
        for (int i = FactoryConstant.MONTH_MAX_DAY; i> contextDTO.getLockEndDay(); i--){
            dayField = FactoryConstant.DAY_FIELD+i;
            if (prodFinal.getFieldValueByFieldName(dayField) == null || (Integer) prodFinal.getFieldValueByFieldName(dayField) == 0){
                continue;
            }
            dayQty = (Integer) prodFinal.getFieldValueByFieldName(dayField);
            if (realDeductQty >= dayQty){
                //若剩余调整量 >= 日排产量，则当日排产量清空
                prodFinal.setFieldValueByFieldName(dayField,null);
                realDeductQty -= dayQty;
            }else{
                //若剩余调整量 < 日排产量，则当日排产量扣减剩余调整量
                prodFinal.setFieldValueByFieldName(dayField,dayQty - realDeductQty);
                realDeductQty = 0;
            }
            if (realDeductQty == 0){
                //剩余调整量=0,退出
                //执行降模排产
                deductMouldProduction(contextDTO,i,prodFinal);
                iDay = i;
                break;
            }
        }
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】--扣减每日排产量,物料编码:%s,需要调整量:%s,剩余调整量:%s,从后向前减到:%s日",contextDTO.getStructureName(),prodFinal.getMaterialCode(),prodFinal.getActualAdjustQty(),realDeductQty,iDay)).append(ApsConstant.DIVISION);
        //提示：物料编码:%s,调减量:%s,从后向前减到:%s日！
        addAdjustProcLog(contextDTO,prodFinal,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.adjustDeduct"),prodFinal.getMaterialCode(),prodFinal.getActualAdjustQty(),iDay));
        return iDay;
    }

    /**
     * 执行降模排产
     * @param contextDTO 周程滚动上下文
     * @param iDay 当前日期
     * @param prodFinal 定稿记录
     */
    private void deductMouldProduction(MpRollAdjustContextDTO contextDTO, int iDay, FactoryMonthPlanFinalAdjustVo prodFinal) {
        String dayField = FactoryConstant.DAY_FIELD+iDay;
        if (prodFinal.getFieldValueByFieldName(dayField) == null ||
                (Integer) prodFinal.getFieldValueByFieldName(dayField) == 0){
            //往前推1天
            iDay -= 1;
            if (iDay <= contextDTO.getLockEndDay()){
                //若往前推1天后，小于等于锁定日，则退出
                return;
            }
            dayField = FactoryConstant.DAY_FIELD+iDay;
            if (prodFinal.getFieldValueByFieldName(dayField) == null ||
                    (Integer) prodFinal.getFieldValueByFieldName(dayField) == 0){
                //前1天为空，退出
                return;
            }
        }
        int dayQty = (Integer) prodFinal.getFieldValueByFieldName(dayField);
        //1、根据计划量测算硫化机台数,有余数加1；
        int dayVulcanizationQty = getDayVulcanizationQty(prodFinal);
        int machines = dayQty / dayVulcanizationQty;
        machines += dayQty % dayVulcanizationQty > 0 ? 1:0;

        //2、执行降模排产
        DeductMouldVo deductMouldVo = new DeductMouldVo();
        deductMouldVo.setMaterialCode(prodFinal.getMaterialCode());
        deductMouldVo.setTotalQty(dayQty);
        deductMouldVo.setRemainingQty(dayQty);
        deductMouldVo.setMachinesAssigned(machines);
        deductMouldVo.setDailyOutputPerMachine(dayVulcanizationQty);
        deductMouldVo.setStartDate(iDay);
        deductMouldVo.setDeadline(contextDTO.getStructureDeadLine());
        //第1天不延续
        deductMouldVo.setFirstDayDelay(false);
        List<DailyScheduleVo> schedules = DeductMouldScheduler.scheduleProduction(deductMouldVo);
        if (PubUtil.isEmpty(schedules)){
            return;
        }
        //3、将降模排产的结果回填
        StringBuffer sb = new StringBuffer();
        for (DailyScheduleVo scheduleVo:schedules){
            dayField = FactoryConstant.DAY_FIELD+iDay;
            prodFinal.setFieldValueByFieldName(dayField,scheduleVo.getSkuQuantity());
            sb.append(scheduleVo.getSkuQuantity()).append(",");
            iDay +=1;
        }
        contextDTO.getLogDetail().append(String.format("结构:%s,【降模排产】,物料编码:%s,降模前的计划量:%s,降模开始日:%s,降模每日计划量:%s",contextDTO.getStructureName(),prodFinal.getMaterialCode(),dayQty,deductMouldVo.getStartDate(),sb.toString())).append(ApsConstant.DIVISION);
    }

    /**
     * 拆出搭配量按日扣减排产量
     * @param contextDTO 周程滚动上下文
     * @param totalMatchQty 总搭配量
     * @param startDay 开始日
     * @param prodFinal 定稿记录
     */
    private void splitMatchQtyByDay(MpRollAdjustContextDTO contextDTO,int totalMatchQty, int startDay, FactoryMonthPlanFinalAdjustVo prodFinal) {
        int dayQty;
        String dayField,matchDayField;
        StringBuffer sb = new StringBuffer();
        int structureDeadline = contextDTO.getStructureDeadLine();
        int oriTotalMatchQty = totalMatchQty;
        //实单肯定在前，从后向前扣减
        for (int i = structureDeadline; i>= startDay; i--){
            dayField = FactoryConstant.DAY_FIELD+i;
            matchDayField = FactoryConstant.MATCH_DAY_FIELD+i;
            if (prodFinal.getFieldValueByFieldName(dayField) == null){
                continue;
            }
            dayQty = (Integer) prodFinal.getFieldValueByFieldName(dayField);
            if (totalMatchQty >= dayQty){
                //若剩余搭配量 >= 日排产量
                prodFinal.setFieldValueByFieldName(matchDayField,dayQty);
                totalMatchQty -= dayQty;
            }else{
                //若剩余搭配量 < 日排产量，则当日排产量扣减剩余调整量
                prodFinal.setFieldValueByFieldName(matchDayField,totalMatchQty);
                totalMatchQty = 0;
            }
            sb.append(prodFinal.getFieldValueByFieldName(matchDayField)).append(",");
            if (totalMatchQty == 0){
                contextDTO.getLogDetail().append(String.format("结构:%s,【拆出搭配量】,物料编码:%s,总搭配量:%s,结构收尾日:%s,搭配开始日:%s,收尾->开始的每日搭配量:%s",contextDTO.getStructureName(), prodFinal.getMaterialCode(),oriTotalMatchQty,structureDeadline,i,sb.toString())).append(ApsConstant.DIVISION);
                //剩余搭配量=0,退出
                break;
            }
        }
    }

    /**
     * 结构内调整：在机SKU增量
     * @param contextDTO 周程滚动调整上下文
     * @param onIncrementAdjustList 在机SKU增量调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void doStructureInWithOnlineInc(MpRollAdjustContextDTO contextDTO,
                                            List<MpAdjustStructureIn> onIncrementAdjustList,
                                            List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if(PubUtil.isEmpty(onIncrementAdjustList)){
            return;
        }
        int lockNextDay = contextDTO.getLockEndDay() + 1;
        //1、排序：在机SKU上机日期早的优先增量排产
        mpProdFinalList.sort(Comparator.comparingInt(FactoryMonthPlanFinalAdjustVo::getBeginDay));
        Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap = convertToMapByMaterial(mpProdFinalList);
        Map<String, MpAdjustStructureIn> mpAdjustStructInMap = onIncrementAdjustList.stream().collect(Collectors.groupingBy(item->item.getMaterialCode(),
                 Collectors.collectingAndThen(Collectors.toList(),m-> {
                     return m.get(0);
                 })));

        Integer newOnLineDay;
        Iterator<FactoryMonthPlanFinalAdjustVo> reLocateProdFinalIter;
        FactoryMonthPlanFinalAdjustVo reLocateFinalVo, mpFinalVo;
        MpAdjustStructureIn adjustStructInVo;
        //reLocateProdFinalList,用于重新定位
        List<FactoryMonthPlanFinalAdjustVo> reLocateProdFinalList = mpProdFinalList.stream().sorted(Comparator.comparing(FactoryMonthPlanFinalAdjustVo::getBeginDay,Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        for (int i=lockNextDay; i<contextDTO.getStructureDeadLine();i++) {
            //2.1、敲定SKU新的上机日期
            newOnLineDay = getNewOnLineDay(contextDTO, i, null);
            if (newOnLineDay == null){
                contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,没有获取到新的上机日期,退出！",contextDTO.getStructureName())).append(ApsConstant.DIVISION);
                break;
            }
            reLocateProdFinalIter = reLocateProdFinalList.iterator();
            while (reLocateProdFinalIter.hasNext()){
                reLocateFinalVo = reLocateProdFinalIter.next();
                mpFinalVo = mpProdFinalMap.get(reLocateFinalVo.getMaterialCode());
                adjustStructInVo = mpAdjustStructInMap.get(reLocateFinalVo.getMaterialCode());
                if (adjustStructInVo == null){
                    reLocateProdFinalIter.remove();
                    continue;
                }
                //2.2、检查SKU二次上机
                if (!checkSecOnline(mpFinalVo,newOnLineDay, contextDTO.getParamMap()) &&
                        !hasPlanByDay(mpFinalVo, newOnLineDay -1)){
                    contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,新的上机日期:%s,不符二次上机条件,退出！", contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),newOnLineDay)).append(ApsConstant.DIVISION);
                    //提示：物料编码:%s,新的上机日期:%s,不符二次上机条件！
                    addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.online.noSecondOnline"),mpFinalVo.getMaterialCode(),newOnLineDay));
                    continue;
                }
                //3、先排实单->自带的搭配
                doStructureInWithOnlineOneSku(contextDTO, mpProdFinalList, lockNextDay,adjustStructInVo ,
                        mpFinalVo,newOnLineDay);
                reLocateProdFinalIter.remove();
            }
        }

    }

    /**
     * 结构内执行在机SKU，单SKU增量调整
     * @param contextDTO 周程滚动上下文
     * @param mpProdFinalList 定稿列表
     * @param lockNextDay 锁定次日
     * @param adjustStructInVo 结构内调整列表
     * @param mpFinalVo 当前定稿Vo
     * @param newOnLineDay 新的上机日
     * @return
     */
    private void doStructureInWithOnlineOneSku(MpRollAdjustContextDTO contextDTO, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList, int lockNextDay,
                                               MpAdjustStructureIn adjustStructInVo, FactoryMonthPlanFinalAdjustVo mpFinalVo, int newOnLineDay) {
        if (adjustStructInVo == null) {
            // 非在机SKU，继续
            return;
        }
        if (mpFinalVo == null){
            return;
        }
        mpFinalVo.setOriTotalQty(mpFinalVo.getTotalQty());
        mpFinalVo.setHasSpecialMaterial(adjustStructInVo.getHasSpecialMaterial());

        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,新上机日:%s,原开始日:%s", contextDTO.getStructureName(), mpFinalVo.getMaterialCode(),newOnLineDay,mpFinalVo.getBeginDay())).append(ApsConstant.DIVISION);
        if(mpFinalVo.getBeginDay() < lockNextDay && !hasPlanByDay(mpFinalVo, lockNextDay -1)){
            // 开始日 < 锁定日 且 锁定前日没有值,则退出
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,新上机日:%s,开始日小于锁定日且锁定日之前没有值,退出！", contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),newOnLineDay)).append(ApsConstant.DIVISION);
            return;
        }
        if (mpFinalVo.getBeginDay() >= lockNextDay && newOnLineDay > mpFinalVo.getBeginDay()){
            // 若在机SKU的开始日大于锁定日，且新的上机日比原开始日大，表示会发生延后，则退出,
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,在机SKU开始日在锁定日之后,新上机日:%s,比原开始日大,退出！", contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),newOnLineDay)).append(ApsConstant.DIVISION);
            //提示：物料编码:%s,在机SKU开始日在锁定日之后,新上机日:%s,比原开始日:%s大
            addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.online.skuDelay"),mpFinalVo.getMaterialCode(),newOnLineDay,mpFinalVo.getBeginDay()));
            return;
        }

        //2.3、计算新需要排产的计划量 = 实单量+自带的搭配量，其中，实单量：待调整量 + 锁定日之后的每日实单排产量
        Integer newPlanQty = getNewPlanQty(contextDTO,adjustStructInVo,mpFinalVo, lockNextDay);
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,新的上机日期:%s,新的排产量:%s", contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty)).append(ApsConstant.DIVISION);

        FactoryMonthPlanFinalAdjustVo bakMpFinalVo = new FactoryMonthPlanFinalAdjustVo();
        BeanUtils.copyProperties(mpFinalVo,bakMpFinalVo);

        //2.4、清空定稿表日计划量
        clearMpFinalDayValue(contextDTO, lockNextDay, mpFinalVo);

        //2.5、增模排产
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】--增模排产,物料编码:%s,开始！", contextDTO.getStructureName(),mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
        int remainPlanQty = incMouldProduction(mpProdFinalList, contextDTO, newOnLineDay, newPlanQty, mpFinalVo,bakMpFinalVo);
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】--增模排产,物料编码:%s,结束！还有剩余排产计划量:%s", contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),remainPlanQty)).append(ApsConstant.DIVISION);
        //提示：物料编码:%s,新上机日:%s,模拟排产结果:{排产计划量(含搭配):%s,已排:%s,剩余未排:%s}！
        addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.online.simulateResult"),mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty,newPlanQty-remainPlanQty,remainPlanQty));
        if (remainPlanQty > mpFinalVo.getConventionProductionQty()){
            // 若剩余量 > 搭配量，说明实单还有剩余
            // 去掉搭配量，将实单量排产
            int newRemainPlanQty = newPlanQty - mpFinalVo.getConventionProductionQty();
            int oriConventionProductionQty = mpFinalVo.getConventionProductionQty();
            // 本身搭配被挤掉，置0
            mpFinalVo.setConventionProductionQty(0);
            //重置开始日\结束日\汇总值
            resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),mpFinalVo);
            int oriTotalQty = mpFinalVo.getTotalQty();
            // 日期向前，依次扣减其他SKU的搭配量，并模拟挤占
            Integer newEndDay = newOnLineDay == lockNextDay ? lockNextDay :newOnLineDay;
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,扣减其他SKU的搭配-开始！", contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            deductMatchOtherSku(contextDTO, lockNextDay,newEndDay,newRemainPlanQty,mpFinalVo, mpProdFinalList);
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,扣减其他SKU的搭配-结束！", contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            //提示：在机的物料编码:%s,排产日:%s,模拟后的剩余未排量:%s,原搭配量:%s,挤搭配前的剩余计划量(扣原搭配量):%s,实际挤占量:%s！
            addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.online.realProduction"),mpFinalVo.getMaterialCode(),newOnLineDay,remainPlanQty,oriConventionProductionQty,newRemainPlanQty,oriTotalQty - mpFinalVo.getTotalQty()));
        }else {
            //提示：在机的物料编码:%s,排产日:%s,计划调整量(含搭配):%s,实际调整量:%s,剩余调整量:%s,原搭配量:%s,剩余搭配量:%s！
            addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.online.realProduction2"),mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty,newPlanQty - remainPlanQty,remainPlanQty,mpFinalVo.getConventionProductionQty(),remainPlanQty));
            if (mpFinalVo.getConventionProductionQty() >= remainPlanQty){
                mpFinalVo.setConventionProductionQty(remainPlanQty);
            }
        }

        //2.5、重置一下搭配排产量标识
        if (mpFinalVo.getConventionProductionQty()>0){
            splitMatchQtyByDay(contextDTO,mpFinalVo.getConventionProductionQty(), lockNextDay,mpFinalVo);
        }
        //3.重置开始日\结束日\汇总值
        resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),mpFinalVo);

        //4.组装消息：SKU原余量未满的消息
        combineMsgWithInLockDayNoFull(contextDTO,mpFinalVo);
    }

    /**
     * 组装消息：SKU原余量未满的消息
     * @param contextDTO 周程滚动上下文
     * @param mpFinalVo 定稿Vo
     */
    private void combineMsgWithInLockDayNoFull(MpRollAdjustContextDTO contextDTO,FactoryMonthPlanFinalAdjustVo mpFinalVo){
        if (YesOrNoEnum.YES.getCode().equals(mpFinalVo.getHasSpecialMaterial())){
            return;
        }
        //50条 <= 不含特殊材料收尾新增的SKU原余量 <= 150条，触发预警；
        //1.从调整次日开始，到锁定截止日，认第1天有值的机台数
        int iAdjustNextDay = contextDTO.getAdjustDay()+1;
        int iLockEndDay = contextDTO.getLockEndDay();
        int dailyQty = getDayVulcanizationQty(mpFinalVo);
        String dayField;
        int dayMachines = 0;
        int totalRemainQty = 0;
        int dayValue;
        for (int i = iAdjustNextDay; i<=iLockEndDay; i++){
            if (hasPlanByDay(mpFinalVo,i)){
                dayField = FactoryConstant.DAY_FIELD+i;
                //第1天有值的机台数
                dayValue = (Integer)mpFinalVo.getFieldValueByFieldName(dayField);
                dayMachines = (int)Math.ceil((double) dayValue / dailyQty);
                totalRemainQty += dayValue;
            }
        }
        //2.预警阀值 X台硫化机 * 50条 * 3天
        int totalQty = dailyQty * dayMachines * (iLockEndDay - iAdjustNextDay + 1);
        if (totalRemainQty < totalQty){
            //提示消息
            if (!StringUtil.isEmptyWithTrim(contextDTO.getMsgTemplateWithRemainQtyNoFull())){
                String strHint = buildMessageContent(contextDTO.getMsgTemplateWithRemainQtyNoFull(),new String[]{contextDTO.getFactoryName(),String.valueOf(contextDTO.getMpYear()),
                        String.valueOf(contextDTO.getMpMonth()),contextDTO.getVersion(),mpFinalVo.getMaterialCode(),String.valueOf(totalRemainQty),String.valueOf(totalQty)});
                contextDTO.getMsgRemainQtyNoFull().append(strHint).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
            }
        }
    }

    /**
     * 转译消息内容
     * @param templateContent 模板内容
     * @param paramValues 模板值
     * @return
     */
    private String buildMessageContent(String templateContent, String[] paramValues) {
        if (StringUtils.isEmpty(templateContent)) {
            return "";
        }

        String msgContent = templateContent;
        if (StringUtils.isNotEmpty(paramValues) && StringUtils.isNotEmpty(msgContent)) {
            for (String oneValue : paramValues) {
                msgContent = msgContent.replaceFirst(Z_K_H, oneValue);
            }
        }
        return msgContent;
    }

    /**
     * List转换Map,按物料
     * @param voList
     * @return
     */
    private Map<String, FactoryMonthPlanFinalAdjustVo> convertToMapByMaterial(List<FactoryMonthPlanFinalAdjustVo> voList) {
        Map<String, FactoryMonthPlanFinalAdjustVo> result = new HashMap<>();
        for (FactoryMonthPlanFinalAdjustVo vo : voList) {
            result.put(vo.getMaterialCode(),vo);
        }
        return result;
    }

    /**
     * 检查总产能限制
     * @param contextDTO 周程滚动上下文
     * @param checkDay 检查日
     * @return true-符合总产能，false-不符合总产能
     */
    private boolean checkTotalCapacityLimit(MpRollAdjustContextDTO contextDTO,Integer checkDay,String materialCode,MpDailyCapacityLimitVo limitVo){
        //Integer dayMaxCapacity = (Integer) contextDTO.getParamMap().get(MonthPlanEnums.DAY_MAX_CAPACITY.getCode());
        DayTotalCapacityChecker dayTotalCapacityChecker = new DayTotalCapacityChecker(contextDTO.getFactoryMonthPlanProdFinalList(),limitVo.getMaxDayProductionQty(),checkDay);
        boolean bCheck = dayTotalCapacityChecker.doCheck();
        String hint = bCheck ? "满足":"不满足,退出！";
        contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,日最大排产量:%s,已排产总计划量:%s,比例:%s,%s！",contextDTO.getStructureName(),materialCode,checkDay,limitVo.getMaxDayProductionQty(),dayTotalCapacityChecker.getTotalPlanQty(),limitVo.getDayProductionRate(),hint)).append(ApsConstant.DIVISION);
        return bCheck;
    }


    /**
     * 检查二次上机
     * @param mpFinalVo 定稿对象vo
     * @param newOnLineDay 新的上机日
     * @param paramMap 参数Map
     * @return true-允许二次上机，false-不允许二次上机
     */
    private boolean checkSecOnline(FactoryMonthPlanFinalAdjustVo mpFinalVo,Integer newOnLineDay,Map<String,Object> paramMap){
        Integer skuSecondDays = (Integer) paramMap.get(MonthPlanEnums.SKU_SECOND_PRODUCTION.getCode());
        SkuSecondChecker skuSecondChecker = new SkuSecondChecker(newOnLineDay,mpFinalVo.getEndDay(),skuSecondDays);
        return skuSecondChecker.doCheck();
    }


    /**
     * 结构调整：在机SKU增量
     * @param contextDTO 周程滚动调整上下文
     * @param onIncrementAdjustList 在机SKU增量调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void doStructureOutWithOnlineInc(MpRollAdjustContextDTO contextDTO,
                                             List<MpAdjustStructureOut> onIncrementAdjustList,
                                             List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if(PubUtil.isEmpty(onIncrementAdjustList)){
            return;
        }
        int lockNextDay = contextDTO.getLockEndDay() + 1;
        //1、排序：在机SKU上机日期早的优先增量排产
        mpProdFinalList.sort(Comparator.comparingInt(FactoryMonthPlanFinalAdjustVo::getBeginDay));
        Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap = convertToMapByMaterial(mpProdFinalList);
        Map<String, MpAdjustStructureOut> mpAdjustStructOutMap = onIncrementAdjustList.stream().collect(Collectors.groupingBy(item->item.getMaterialCode(),
                Collectors.collectingAndThen(Collectors.toList(),m-> {
                    return m.get(0);
                })));

        Integer newOnLineDay;
        Iterator<FactoryMonthPlanFinalAdjustVo> reLocateProdFinalIter;
        FactoryMonthPlanFinalAdjustVo reLocateFinalVo, mpFinalVo;
        MpAdjustStructureOut adjustStructOutVo;
        //reLocateProdFinalList,用于重新定位
        List<FactoryMonthPlanFinalAdjustVo> reLocateProdFinalList = mpProdFinalList.stream().sorted(Comparator.comparing(FactoryMonthPlanFinalAdjustVo::getBeginDay,Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        for (int i=lockNextDay; i<contextDTO.getStructureDeadLine();i++) {
            //2.1、敲定SKU新的上机日期
            newOnLineDay = getNewOnLineDayForStructOut(contextDTO, i);
            if (newOnLineDay == null){
                contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,没有获取到新的上机日期,退出！",contextDTO.getStructureName())).append(ApsConstant.DIVISION);
                break;
            }
            reLocateProdFinalIter = reLocateProdFinalList.iterator();
            while (reLocateProdFinalIter.hasNext()){
                reLocateFinalVo = reLocateProdFinalIter.next();
                mpFinalVo = mpProdFinalMap.get(reLocateFinalVo.getMaterialCode());
                adjustStructOutVo = mpAdjustStructOutMap.get(reLocateFinalVo.getMaterialCode());
                if (adjustStructOutVo == null){
                    reLocateProdFinalIter.remove();
                    continue;
                }
                //2.2、检查SKU二次上机
                if (!checkSecOnline(mpFinalVo,newOnLineDay, contextDTO.getParamMap()) &&
                        !hasPlanByDay(mpFinalVo, newOnLineDay -1)){
                    contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,新的上机日期:%s,不符二次上机条件,退出！", contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),newOnLineDay)).append(ApsConstant.DIVISION);
                    //提示：物料编码:%s,新的上机日期:%s,不符二次上机条件！
                    addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.online.noSecondOnline"),mpFinalVo.getMaterialCode(),newOnLineDay));
                    continue;
                }
                //3、先排实单->自带的搭配
                doStructureOutWithOnlineOneSku(contextDTO, mpProdFinalList, lockNextDay,adjustStructOutVo ,
                        mpFinalVo,newOnLineDay);
                reLocateProdFinalIter.remove();
            }
        }
    }

    /**
     * 结构间 执行在机SKU，单SKU增量调整
     * @param contextDTO 周程滚动上下文
     * @param mpProdFinalList 定稿列表
     * @param lockNextDay 锁定次日
     * @param adjustStructOutVo 结构调整列表
     * @param mpFinalVo 当前定稿Vo
     * @param newOnLineDay 新的上机日
     * @return
     */
    private void doStructureOutWithOnlineOneSku(MpRollAdjustContextDTO contextDTO, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList, int lockNextDay,
                                                MpAdjustStructureOut adjustStructOutVo, FactoryMonthPlanFinalAdjustVo mpFinalVo, int newOnLineDay) {
        if (adjustStructOutVo == null) {
            // 非在机SKU，继续
            return;
        }
        if (mpFinalVo == null) {
            return;
        }
        mpFinalVo.setOriTotalQty(mpFinalVo.getTotalQty());
        mpFinalVo.setHasSpecialMaterial(adjustStructOutVo.getHasSpecialMaterial());
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,新上机日:%s,原开始日:%s", contextDTO.getStructureName(), mpFinalVo.getMaterialCode(),newOnLineDay,mpFinalVo.getBeginDay())).append(ApsConstant.DIVISION);
        if(mpFinalVo.getBeginDay() < lockNextDay && !hasPlanByDay(mpFinalVo,lockNextDay -1)){
            // 开始日 < 锁定日 且 锁定前日没有值,继续
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,新上机日:%s,开始日小于锁定日且锁定日之前没有值,退出！", contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),newOnLineDay)).append(ApsConstant.DIVISION);
            return;
        }

        if (mpFinalVo.getBeginDay() >= lockNextDay && newOnLineDay > mpFinalVo.getBeginDay()){
            // 若在机SKU的开始日大于锁定日，且新的上机日比原开始日大，表示会发生延后，则退出,
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,在机SKU开始日在锁定日之后,新上机日:%s,比原开始日大,退出！", contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),newOnLineDay)).append(ApsConstant.DIVISION);
            //提示：物料编码:%s,在机SKU开始日在锁定日之后,新上机日:%s,比原开始日:%s大
            addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.online.skuDelay"),mpFinalVo.getMaterialCode(),newOnLineDay,mpFinalVo.getBeginDay()));
            return;
        }

        //2.2、计算新需要排产的计划量 = 实单量+自带的搭配量，其中，实单量：待调整量 + 锁定日之后的每日实单排产量
        Integer newPlanQty = getNewPlanQty(contextDTO,adjustStructOutVo,mpFinalVo,lockNextDay);
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,新的上机日期:%s,新的排产量:%s", contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty)).append(ApsConstant.DIVISION);

        FactoryMonthPlanFinalAdjustVo bakMpFinalVo = new FactoryMonthPlanFinalAdjustVo();
        BeanUtils.copyProperties(mpFinalVo,bakMpFinalVo);

        //2.3、清空定稿表日计划量
        clearMpFinalDayValue(contextDTO,lockNextDay, mpFinalVo);

        //2.4、增模排产
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】--增模排产,物料编码:%s,开始！", contextDTO.getStructureName(),mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
        int remainPlanQty = incMouldProduction(mpProdFinalList, contextDTO, newOnLineDay, newPlanQty, mpFinalVo,bakMpFinalVo);
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】--增模排产,物料编码:%s,结束！还有剩余排产计划量:%s", contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),remainPlanQty)).append(ApsConstant.DIVISION);
        //提示：物料编码:%s,新上机日:%s,模拟排产结果:{排产计划量(含搭配):%s,已排:%s,剩余未排:%s}！
        addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.online.simulateResult"),mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty,newPlanQty-remainPlanQty,remainPlanQty));
        if (remainPlanQty > mpFinalVo.getConventionProductionQty()){
            // 若剩余量 > 搭配量，说明实单还有剩余
            // 去掉搭配量，将实单量排产
            int newRemainPlanQty = newPlanQty - mpFinalVo.getConventionProductionQty();
            int oriConventionProductionQty = mpFinalVo.getConventionProductionQty();
            // 本身搭配被挤掉，置0
            mpFinalVo.setConventionProductionQty(0);
            //重置开始日\结束日\汇总值
            resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),mpFinalVo);
            int oriTotalQty = mpFinalVo.getTotalQty();
            // 日期向前，依次扣减其他SKU的搭配量，并模拟挤占
            Integer newEndDay = newOnLineDay == lockNextDay ? lockNextDay:newOnLineDay;
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,扣减其他SKU的搭配-开始！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            deductMatchOtherSku(contextDTO,lockNextDay,newEndDay,newRemainPlanQty,mpFinalVo,mpProdFinalList);
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,扣减其他SKU的搭配-结束！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            //提示：在机的物料编码:%s,排产日:%s,模拟后的剩余未排量:%s,原搭配量:%s,挤搭配前的剩余计划量(扣原搭配量):%s,实际挤占量:%s！
            addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.online.realProduction"),mpFinalVo.getMaterialCode(),newOnLineDay,remainPlanQty,oriConventionProductionQty,newRemainPlanQty,oriTotalQty - mpFinalVo.getTotalQty()));
        }else {
            //提示：在机的物料编码:%s,排产日:%s,计划调整量(含搭配):%s,实际调整量:%s,剩余调整量:%s,原搭配量:%s,剩余搭配量:%s！
            addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.online.realProduction2"),mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty,newPlanQty - remainPlanQty,remainPlanQty,mpFinalVo.getConventionProductionQty(),remainPlanQty));
            if (mpFinalVo.getConventionProductionQty() >= remainPlanQty){
                mpFinalVo.setConventionProductionQty(remainPlanQty);
            }
        }
        //2.5、重置一下搭配排产量标识
        if (mpFinalVo.getConventionProductionQty()>0){
            splitMatchQtyByDay(contextDTO,mpFinalVo.getConventionProductionQty(), lockNextDay,mpFinalVo);
        }

        //3、重置开始日\结束日\汇总值
        resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),mpFinalVo);

        //4、组装消息：SKU原余量未满的消息
        combineMsgWithInLockDayNoFull(contextDTO,mpFinalVo);
    }
    /**
     * 获取新上机日
     * @param contextDTO 周程滚动上下文
     * @param lockNextDay 开始日
     * @param mpFinalVo 定稿Vo
     * @return 新上机日
     */
    private Integer getNewOnLineDay(MpRollAdjustContextDTO contextDTO, int lockNextDay, FactoryMonthPlanFinalAdjustVo mpFinalVo) {
        int endDay = contextDTO.getStructureDeadLine();
        if (mpFinalVo != null && mpFinalVo.getBeginDay() >= lockNextDay){
            //若开始日 >= 锁定日，截止日设为计划开始日，因为已排计划不能往后延
            endDay = mpFinalVo.getBeginDay();
        }

        return new MpAdjustDailyCapacityLimit().getNewOnLineDay(lockNextDay, endDay, contextDTO.getDailyCapacityLimitVoMap());
    }

    /**
     * 获取新上机日 for 结构间调整
     * @param contextDTO 周程滚动上下文
     * @param lockNextDay 开始日
     * @return 新上机日
     */
    private Integer getNewOnLineDayForStructOut(MpRollAdjustContextDTO contextDTO, int lockNextDay) {
        int endDay = contextDTO.getStructureDeadLine();
        return new MpAdjustDailyCapacityLimit().getNewOnLineDay(lockNextDay, endDay, contextDTO.getDailyCapacityLimitVoMap());
    }

    /**
     * 判断某天是否有计划
     * @param mpFinalVo 定稿Vo
     * @param iDay 某天
     * @return true 有计划，false 无计划
     */
    private boolean hasPlanByDay(FactoryMonthPlanFinalAdjustVo mpFinalVo,int iDay){
        String dayField = FactoryConstant.DAY_FIELD+iDay;
        return mpFinalVo.getFieldValueByFieldName(dayField) != null &&
                (Integer)mpFinalVo.getFieldValueByFieldName(dayField) > 0;
    }
    /**
     * 扣减其他SKU的搭配量，并模拟挤占
     * @param lockNextDay 锁定次日
     * @param endDay 结束日（新上机日向前）
     * @param planQty 计划量
     * @param curFinalVo 当前定稿Vo
     * @param mpProdFinalList 定稿列表
     */
    private void deductMatchOtherSku(MpRollAdjustContextDTO contextDTO,int lockNextDay,int endDay,int planQty,FactoryMonthPlanFinalAdjustVo curFinalVo,
                                List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList){
        if (PubUtil.isEmpty(mpProdFinalList)){
            return;
        }
        if (endDay < lockNextDay){
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】-结束日小于锁定次日,退出！",contextDTO.getStructureName(), curFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            //提示：物料编码:%s,挤搭配日:%s 小于 锁定次日:%s,停止！
            addAdjustProcLog(contextDTO,curFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.deductMatch.matchDayLtLockNextDay"),curFinalVo.getMaterialCode(),endDay,lockNextDay));
            return;
        }
        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = contextDTO.getDailyCapacityLimitVoMap();
        //新上机日的产能限制Vo
        MpDailyCapacityLimitVo dailyCapacityLimitVo = dailyCapacityLimitVoMap.get(endDay);
        int startMould = getStartMould(adjustDailyCapacityLimitObj,contextDTO.getParamMap(),endDay,curFinalVo,dailyCapacityLimitVo);
        //获取新的活块数
        int blockQty = getNewTypeBlockQty(contextDTO,curFinalVo,endDay);
        contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产日:%s,获取到新的活块数:%s！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay,blockQty)).append(ApsConstant.DIVISION);
        if (startMould > blockQty){
            // 在机的已排模具数已达到活块数，则退出
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】-在机的计划模具数:%s 已达到活块数:%s,退出！",contextDTO.getStructureName(), curFinalVo.getMaterialCode(),startMould,blockQty)).append(ApsConstant.DIVISION);
            //提示：物料编码:%s,挤搭配日:%s,在机计划模具数:%s 已达到活块数:%s,停止！
            addAdjustProcLog(contextDTO,curFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.deductMatch.onlinePlanMouldGtBlock"),curFinalVo.getMaterialCode(),endDay,startMould,blockQty));
            return;
        }

        adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList,endDay, dailyCapacityLimitVo, contextDTO.getParamMap(), curFinalVo.getMainPattern());
        contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产日:%s,其产能限制信息:%s！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay,dailyCapacityLimitVoMap.get(endDay) == null ? "" : dailyCapacityLimitVoMap.get(endDay).toString())).append(ApsConstant.DIVISION);
        //检查: 当前每日硫化机台数\当前每日胎胚种类数 符合性
        //检查：主花纹向下模具数量(/2转成机台数) 符合性
        int cavityQty = getNewCavityQty(contextDTO,curFinalVo,endDay);
        contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产日:%s,获取到新的型腔数:%s！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay,cavityQty)).append(ApsConstant.DIVISION);
        if (!adjustDailyCapacityLimitObj.preCheckCapacitySatisfy(dailyCapacityLimitVo) ||
                !preCheckMouldSatisfy(dailyCapacityLimitVo,cavityQty)){
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,每日硫化机台数或每日胎胚种类数或型腔数不符合产能限制,退出！",contextDTO.getStructureName(),curFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            //提示：物料编码:%s,挤搭配日:%s,每日硫化机台数【%s】或胎胚种类数【%s】或型腔数【%s】不符合产能限制,停止！
            dailyCapacityLimitVo = ObjectUtils.defaultIfNull(dailyCapacityLimitVo, new MpDailyCapacityLimitVo());
            addAdjustProcLog(contextDTO,curFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.deductMatch.checkCapacityLimit"),curFinalVo.getMaterialCode(),endDay, dailyCapacityLimitVo.getUsedLhMachines(),dailyCapacityLimitVo.getUsedEmbryoTypes(),dailyCapacityLimitVo.getPatternUsedLhMachines()));
            return;
        }

        // 1. 获取某日有搭配量的其他SKU定稿列表
        List<FactoryMonthPlanFinalAdjustVo> newOtherFinalList = getMatchFinalListByDay(endDay, curFinalVo, mpProdFinalList);
        if (PubUtil.isEmpty(newOtherFinalList)){
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产%s日,其他SKU没有搭配量,退出！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay)).append(ApsConstant.DIVISION);
            //提示：物料编码:%s,挤搭配日:%s,其他SKU没有搭配量,停止！
            addAdjustProcLog(contextDTO,curFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.deductMatch.otherSkuNoMatch"),curFinalVo.getMaterialCode(),endDay));
            return;
        }
        FactoryMonthPlanFinalAdjustVo optimalFinalVo,bakMpFinalVo;
        int remainPlanQty,deductMatchQty,oriMatchQty;
        while (newOtherFinalList.size() >0 ){
            // 2.从多个SKU中，匹配其他最优的定稿SKU记录
            optimalFinalVo = getOptimalOtherSku(curFinalVo, newOtherFinalList);
            oriMatchQty = optimalFinalVo.getConventionProductionQty();
            // 3.清空搭配日计划 及扣减搭配总量
            bakMpFinalVo = new FactoryMonthPlanFinalAdjustVo();
            BeanUtils.copyProperties(curFinalVo,bakMpFinalVo);
            clearMpFinalDayValue(contextDTO,endDay,curFinalVo);
            int clearDayValue = clearMpFinalDayValue(contextDTO,endDay,optimalFinalVo);
            if (optimalFinalVo.getConventionProductionQty()>= clearDayValue){
                //若搭配量比清空的值大，即能覆盖，那减少的搭配量 = 清空的值
                deductMatchQty = clearDayValue;
                optimalFinalVo.setConventionProductionQty(optimalFinalVo.getConventionProductionQty() - clearDayValue);
            }else {
                //若搭配量比清空的值小，即不能覆盖，那减少的搭配量 = 搭配量
                deductMatchQty = optimalFinalVo.getConventionProductionQty();
                optimalFinalVo.setConventionProductionQty(0);
            }
            //重置开始日\结束日\汇总值
            resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),optimalFinalVo);
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产%s日,已匹配到最优的有搭配量的物料编码:%s,总搭配量:%s,减少搭配量:%s！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay,optimalFinalVo.getMaterialCode(),oriMatchQty,deductMatchQty)).append(ApsConstant.DIVISION);
            // 4.增模模拟排产
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产%s日,模拟排产-开始！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay)).append(ApsConstant.DIVISION);
            remainPlanQty = incMouldProduction(mpProdFinalList, contextDTO, endDay, planQty, curFinalVo,bakMpFinalVo);
            //提示：物料编码:%s,挤搭配日:%s,挤其他SKU物料编码:%s,挤掉的搭配量:%s,模拟排产计划量:%s,模拟后剩余计划量:%s！
            addAdjustProcLog(contextDTO,curFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.deductMatch.matchResult"),curFinalVo.getMaterialCode(),endDay,optimalFinalVo.getMaterialCode(),deductMatchQty,planQty,remainPlanQty));
            //重置开始日\结束日\汇总值
            resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),curFinalVo);
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产%s日,模拟排产-结束,剩余排产计划量:%s,本次剩余排产计划量:%s！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay,planQty,remainPlanQty)).append(ApsConstant.DIVISION);
            if (remainPlanQty > 0){
                String optimalMaterialCode = optimalFinalVo.getMaterialCode();
                newOtherFinalList.removeIf(item->item.getMaterialCode().equals(optimalMaterialCode));
            }else{
                break;
            }
        }
        // 5.递归，扣减其他SKU的搭配量，并模拟挤占
        contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产%s日,递归-开始！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay-1)).append(ApsConstant.DIVISION);
        deductMatchOtherSku(contextDTO,lockNextDay,endDay-1,planQty,curFinalVo,mpProdFinalList);
        contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产%s日,递归-结束！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay-1)).append(ApsConstant.DIVISION);

    }

    /**
     * 获取某日有搭配量的其他SKU定稿列表
     * @param endDay 某日
     * @param curFinalVo 当前定稿Vo
     * @param mpProdFinalList 定稿列表
     * @return 有搭配量的其他SKU定稿列表
     */
    private List<FactoryMonthPlanFinalAdjustVo> getMatchFinalListByDay(int endDay, FactoryMonthPlanFinalAdjustVo curFinalVo,
                                                                  List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) {
        List<FactoryMonthPlanFinalAdjustVo> newOtherFinalList = new ArrayList<>();
        for (FactoryMonthPlanFinalAdjustVo mpFinalVo: mpProdFinalList){
            if (mpFinalVo.getConventionProductionQty() == null ||
                    mpFinalVo.getConventionProductionQty() <= 0){
                continue;
            }
            if (mpFinalVo.getMaterialCode().equals(curFinalVo.getMaterialCode())){
                // 略过当前SKU
                continue;
            }
            String matchDayField = FactoryConstant.MATCH_DAY_FIELD + endDay;
            if (mpFinalVo.getFieldValueByFieldName(matchDayField) != null &&
                    (Integer)mpFinalVo.getFieldValueByFieldName(matchDayField) > 0){
                newOtherFinalList.add(mpFinalVo);
            }
        }
        return newOtherFinalList;
    }

    /**
     * 从多个SKU中，匹配其他最优的定稿SKU记录
     * @param curFinalVo 当前定稿记录
     * @param newOtherFinalList 定稿其他SKU列表
     * @return 最优的定稿SKU记录
     */
    private FactoryMonthPlanFinalAdjustVo getOptimalOtherSku(FactoryMonthPlanFinalAdjustVo curFinalVo, List<FactoryMonthPlanFinalAdjustVo> newOtherFinalList) {
        FactoryMonthPlanFinalAdjustVo sameSpec2PatternVo = null;
        FactoryMonthPlanFinalAdjustVo sameEmbryo2MainPatternVo = null;
        FactoryMonthPlanFinalAdjustVo minMatchQtyVo = null;
        int minMatchQty = newOtherFinalList.get(0).getConventionProductionQty();
        for (FactoryMonthPlanFinalAdjustVo tFinalVo: newOtherFinalList){
            //若有多个SKU，优先匹配同规格同花纹、同胎胚同模具的SKU，其次匹配搭配量少的SKU
            //同规格同花纹：定稿表.规格相同 AND 定稿表.花纹相同
            //同胎胚同模具：定稿表.胎胚相同 AND 定稿表.主花纹相同
            if (curFinalVo.getSpecifications().equals(tFinalVo.getSpecifications()) &&
                    curFinalVo.getPattern().equals(tFinalVo.getPattern())){
                sameSpec2PatternVo = tFinalVo;
            }
            if (curFinalVo.getMainMaterialDesc().equals(tFinalVo.getMainMaterialDesc()) &&
                    curFinalVo.getMainPattern().equals(tFinalVo.getMainPattern())){
                sameEmbryo2MainPatternVo = tFinalVo;
            }
            if (minMatchQty >= tFinalVo.getConventionProductionQty() ){
                minMatchQtyVo = tFinalVo;
                minMatchQty = tFinalVo.getConventionProductionQty();
            }
        }
        if (sameSpec2PatternVo != null){
            return sameSpec2PatternVo;
        }
        if (sameEmbryo2MainPatternVo != null){
            return sameEmbryo2MainPatternVo;
        }
        return minMatchQtyVo;
    }

    /**
     * 增模排产
     * @param mpProdFinalList 定稿列表
     * @param contextDTO 周程滚动上下文
     * @param newOnLineDay 新的上机日期
     * @param newPlanQty 新的计划量
     * @param mpFinalVo 当前定稿记录
     */
    private synchronized int incMouldProduction(List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                    MpRollAdjustContextDTO contextDTO,
                                    Integer newOnLineDay, Integer newPlanQty, FactoryMonthPlanFinalAdjustVo mpFinalVo,FactoryMonthPlanFinalAdjustVo bakMpFinalVo) {
        String dayField;
        int dayValue;

        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = contextDTO.getDailyCapacityLimitVoMap();
        int structureDeadLine = contextDTO.getStructureDeadLine();
        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
        int otherTotalQty = getOtherTotalQtyForSpecMaterial(mpProdFinalList,mpFinalVo);
        //新上机日的产能限制Vo
        MpDailyCapacityLimitVo dailyCapacityLimitVo = contextDTO.getDailyCapacityLimitVoMap().get(newOnLineDay);
        int startMould = getStartMould(adjustDailyCapacityLimitObj,contextDTO.getParamMap(),newOnLineDay,mpFinalVo,dailyCapacityLimitVo);
        int firstStartMould = startMould;
        int dailyQty = getDayVulcanizationQty(mpFinalVo);
        int blockQty,cavityQty,diffQty;
        Integer dayVulcanizationQty;
        boolean bFirstAddMould = true;
        //量试标识
        boolean bTrailProductionFlag = ConstructionStageEnum.TRIAL_PRODUCTION.getStage().equals(mpFinalVo.getConstructionStage());
        while (newPlanQty > 0){
            //已有排产标识，防止中间断开
            boolean bHasProduction = false;
            contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,尝试增模具数:%s！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),startMould)).append(ApsConstant.DIVISION);
            for (int i = newOnLineDay; i<= structureDeadLine; i++){
                //SKU的模具数限制：SKU的模具数<=SKU活块的数量
                blockQty = getNewTypeBlockQty(contextDTO,mpFinalVo,i);
                contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,获取到新的活块数:%s！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),i,blockQty)).append(ApsConstant.DIVISION);
                if (startMould > blockQty){
                    contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,SKU增模后的模具数:%s 大于SKU活块的数量:%s！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),i,startMould,blockQty)).append(ApsConstant.DIVISION);
                    return newPlanQty < 0 ? 0:newPlanQty;
                }
                if (dailyCapacityLimitVoMap.get(i) == null){
                    continue;
                }
                if (bTrailProductionFlag && getTrialNewOnlineDay(contextDTO,i,i, mpProdFinalList) == null){
                    //若是量试，但该日不能排，则继续
                    continue;
                }
                //检查总产能限制(允许上下波动)
                if (!checkTotalCapacityLimit(contextDTO,i,mpFinalVo.getMaterialCode(),dailyCapacityLimitVoMap.get(i))){
                    if (bHasProduction && YesOrNoEnum.YES.getCode().equals(dailyCapacityLimitVoMap.get(i).getDayOpenCloseFlag())){
                        break;
                    }
                    continue;
                }
                dayField = FactoryConstant.DAY_FIELD + i;
                dayValue = mpFinalVo.getFieldValueByFieldName(dayField) == null ? 0 : (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
                dayVulcanizationQty = dailyQty;
                if (i == newOnLineDay){
                    //上机日，要考虑衔接
                    if (isIncMouldFirstDay(mpFinalVo,newOnLineDay-1,dayValue,dailyQty)){
                        //增模首日
                        dayVulcanizationQty = adjustDailyCapacityLimitObj.getFirstDayQty(mpProdFinalList,newOnLineDay, dailyCapacityLimitVoMap.get(newOnLineDay), contextDTO.getParamMap(), mpFinalVo.getMainPattern());
                    }
                }else{
                    if (bFirstAddMould){
                        dayVulcanizationQty = adjustDailyCapacityLimitObj.getFirstDayQty(mpProdFinalList,i, dailyCapacityLimitVoMap.get(newOnLineDay), contextDTO.getParamMap(), mpFinalVo.getMainPattern());
                    }
                }
                if(dayVulcanizationQty == null){
                    //若获取到首日量是空值,表示主花纹向下，当日不让增模或换活字块
                    contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,获取到的首日计划量为空,表示当前排产日主花纹下不允许换活块！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),i)).append(ApsConstant.DIVISION);
                    if (bHasProduction && YesOrNoEnum.YES.getCode().equals(dailyCapacityLimitVoMap.get(i).getDayOpenCloseFlag())){
                        break;
                    }
                    continue;
                }

                //检查是否自动补量
                boolean isAutoReplenishment = checkAutoReplenishment(contextDTO.getParamMap(),i,mpFinalVo);
                contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,自动补量标识:%s！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),i,isAutoReplenishment ? "是":"否")).append(ApsConstant.DIVISION);
                if (!isAutoReplenishment){
                    //若剩余计划量 < 日硫化量，则按剩余计划量累加
                    dayVulcanizationQty = newPlanQty < dayVulcanizationQty ? newPlanQty : dayVulcanizationQty;
                }
                //若是开产日，计划量全部等比例减少
                dayVulcanizationQty = getPlanQtyForOpenProductionFirstDay(contextDTO,adjustDailyCapacityLimitObj,dailyCapacityLimitVoMap.get(i),dayVulcanizationQty);
                //若是特殊结构，其调整计划量不能超过原月计划总量
                if (YesOrNoEnum.YES.getCode().equals(mpFinalVo.getHasSpecialMaterial())){
                    diffQty = getDiffQtyForSpecMaterial(contextDTO,mpFinalVo,otherTotalQty,dayVulcanizationQty,i);
                    if (diffQty == 0){
                        return newPlanQty < 0 ? 0:newPlanQty;
                    }
                    dayVulcanizationQty = diffQty > dayVulcanizationQty ? dayVulcanizationQty:diffQty;
                }
                dayValue += dayVulcanizationQty;
                mpFinalVo.setFieldValueByFieldName(dayField,dayValue);
                adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList,i, dailyCapacityLimitVoMap.get(i), contextDTO.getParamMap(), mpFinalVo.getMainPattern());
                contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,其产能限制信息:%s！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),i,dailyCapacityLimitVoMap.get(i).toString())).append(ApsConstant.DIVISION);
                //检查: 当前每日硫化机台数\当前每日胎胚种类数 符合性
                if (!adjustDailyCapacityLimitObj.checkCapacitySatisfy(dailyCapacityLimitVoMap.get(i))){
                    // 将值还原，并退出，继续加模
                    if (i == newOnLineDay && startMould == firstStartMould){
                        //若是新上机日就不符要求，将整个vo还原；因为在其他SKU移动中，会提前清
                        BeanUtils.copyProperties(bakMpFinalVo,mpFinalVo);
                        startMould = getStartMould(adjustDailyCapacityLimitObj,contextDTO.getParamMap(),i+1,mpFinalVo,dailyCapacityLimitVo);
                    }else{
                        dayValue -= dayVulcanizationQty;
                        mpFinalVo.setFieldValueByFieldName(dayField,dayValue == 0 ? null:dayValue);
                    }
                    contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,每日硫化机台数或每日胎胚种类数不符合产能限制,退出！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),i)).append(ApsConstant.DIVISION);
                    if (bHasProduction && YesOrNoEnum.YES.getCode().equals(dailyCapacityLimitVoMap.get(i).getDayOpenCloseFlag())){
                        break;
                    }
                    continue;
                }
                //检查：主花纹向下模具数量(/2转成机台数) 符合性
                cavityQty = getNewCavityQty(contextDTO,mpFinalVo,i);
                contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,获取到新的型腔数:%s！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),i,cavityQty)).append(ApsConstant.DIVISION);
                if (!checkMouldSatisfy(dailyCapacityLimitVoMap.get(i),cavityQty)){
                    // 将值还原，并退出 外循环
                    if (i == newOnLineDay && startMould == firstStartMould){
                        //若是新上机日就不符要求，将整个vo还原；因为在其他SKU移动中，会提前清
                        BeanUtils.copyProperties(bakMpFinalVo,mpFinalVo);
                    }else{
                        dayValue -= dayVulcanizationQty;
                        mpFinalVo.setFieldValueByFieldName(dayField,dayValue==0?null:dayValue);
                    }
                    contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,主花纹:%s,其主花纹模具数不符合产能限制,退出！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),i,mpFinalVo.getMainPattern())).append(ApsConstant.DIVISION);
                    return newPlanQty < 0 ? 0:newPlanQty;
                }
                newPlanQty -= dayVulcanizationQty;
                if (newPlanQty <=0 && !isAutoReplenishment){
                    return 0;
                }
                bFirstAddMould = false;
                if (mpFinalVo.getFieldValueByFieldName(dayField) !=null && (Integer) mpFinalVo.getFieldValueByFieldName(dayField) !=0){
                    bHasProduction = true;
                }
            }

            startMould += 2;
            bFirstAddMould = true;
        }
        return newPlanQty < 0 ? 0:newPlanQty;
    }

    /**
     * 获取新的活块数量
     * @param contextDTO 周程滚动上下文
     * @param mpFinalVo 定稿Vo
     * @param iDay 当前天
     * @return 活块数量
     */
    private int getNewTypeBlockQty(MpRollAdjustContextDTO contextDTO,FactoryMonthPlanFinalAdjustVo mpFinalVo,int iDay){
        DailyMouldAvailabilityResult cavity2BlockVo = contextDTO.getCavity2BlockMap().get(iDay);
        if (cavity2BlockVo != null && cavity2BlockVo.getInsertResults() != null){
            Integer blockQty = cavity2BlockVo.getInsertResults().get(mpFinalVo.getMaterialDesc());
            return blockQty != null ? blockQty:mpFinalVo.getTypeBlockQty();
        }
        return mpFinalVo.getTypeBlockQty();
    }

    /**
     * 获取新的型腔数量
     * @param contextDTO 周程滚动上下文
     * @param mpFinalVo 定稿Vo
     * @param iDay 当前天
     * @return 型腔数量
     */
    private int getNewCavityQty(MpRollAdjustContextDTO contextDTO,FactoryMonthPlanFinalAdjustVo mpFinalVo,int iDay){
        DailyMouldAvailabilityResult cavity2BlockVo = contextDTO.getCavity2BlockMap().get(iDay);
        if (cavity2BlockVo != null && cavity2BlockVo.getCavityResults() != null){
            Integer cavityQty = cavity2BlockVo.getCavityResults().get(mpFinalVo.getStructureName()+mpFinalVo.getMainPattern());
            return cavityQty != null ? cavityQty:mpFinalVo.getMouldCavityQty();
        }
        return mpFinalVo.getMouldCavityQty();
    }

    /**
     * 特殊结构：获取其他SKU总的汇总值
     * @param mpProdFinalList 定稿Vo列表
     * @param mpFinalVo 当前定稿Vo
     * @return 其他SKU总的汇总值
     */
    private int getOtherTotalQtyForSpecMaterial(List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,FactoryMonthPlanFinalAdjustVo mpFinalVo){
        if (YesOrNoEnum.YES.getCode().equals(mpFinalVo.getHasSpecialMaterial())){
            return mpProdFinalList.stream().filter(x->!x.getMaterialCode().equals(mpFinalVo.getMaterialCode())).mapToInt(x-> {
                return x.getTotalQty() == null ? 0: x.getTotalQty();
            }).sum();
        }
        return 0;
    }

    /**
     * 获取开产首日计划量
     * @param contextDTO 周程滚动上下文
     * @param dailyCapacityLimitVo 日产限制Vo
     * @param dayVulcanizationQty 增模计划量
     * @return 开产首日计划量
     */
    private int getPlanQtyForOpenProductionFirstDay(MpRollAdjustContextDTO contextDTO,MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj,
                                                    MpDailyCapacityLimitVo dailyCapacityLimitVo, int dayVulcanizationQty){
        if (dailyCapacityLimitVo.isOpenProductionFirstDay()){
            //若是开产首日，除了换模8，全部按等比例
            if (dayVulcanizationQty == (Integer) contextDTO.getParamMap().get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode())){
                return dayVulcanizationQty;
            }
            //新计划量 = 增模计划量 * 比例/100,若是奇数,+1
            return adjustDailyCapacityLimitObj.getProportionalDeductQty(dailyCapacityLimitVo,dayVulcanizationQty);
        }
        return dayVulcanizationQty;
    }


    /**
     * 特殊结构，获取允许排的计划量
     * @param contextDTO 周程滚动计划量
     * @param mpFinalVo 当前定稿Vo
     * @param otherTotalQty 其他SKU总计划量
     * @param dayVulcanizationQty 当前要排计划量
     * @param iDay 当日
     * @return 实际排的计划量
     */
    private int getDiffQtyForSpecMaterial(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo mpFinalVo,
                                          int otherTotalQty, int dayVulcanizationQty, int iDay){
        String dayField;
        int curTotalQty = 0;
        for (int i=contextDTO.getStartDay();i<=contextDTO.getEndDay();i++){
            dayField = FactoryConstant.DAY_FIELD + i;
            if (mpFinalVo.getFieldValueByFieldName(dayField) != null){
                curTotalQty += (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
            }
        }
        // 差异数量 = 特殊结构总计划量 - 其他SKU汇总 - 当前SKU的已排汇总值
        int specTotalQty = contextDTO.getSpecStructureTotalQty() == null ? 0: contextDTO.getSpecStructureTotalQty();
        int diffQty = specTotalQty - otherTotalQty - curTotalQty;
        diffQty = diffQty < 0 ? 0 : diffQty;
        contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,特殊标识：是,特殊结构原总计划量:%s,其他SKU总计划量:%s,当前SKU已排计划量:%s,允许再排计划量:%s！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),iDay,contextDTO.getSpecStructureTotalQty(),otherTotalQty,curTotalQty,diffQty)).append(ApsConstant.DIVISION);
        return diffQty;
    }

    /**
     * 增模首日
     * @param mpFinalVo 定稿Vo
     * @param preNewOnLineDay 上机日前日
     * @param dayValue 上机日
     * @param dailyValue 日硫化量
     * @return true-增模首日，false-非增模首日
     */
    private boolean isIncMouldFirstDay(FactoryMonthPlanFinalAdjustVo mpFinalVo,int preNewOnLineDay,int dayValue,int dailyValue){
        if (preNewOnLineDay < FactoryConstant.MONTH_START_DAY){
            return true;
        }
        String preDayField = FactoryConstant.DAY_FIELD + preNewOnLineDay;
        int preDayValue = mpFinalVo.getFieldValueByFieldName(preDayField) == null ? 0 : (Integer) mpFinalVo.getFieldValueByFieldName(preDayField);
        //上机日的前日机台数
        int preMachines = (int)Math.ceil((double) preDayValue / dailyValue);
        //今日已有机台数
        int dayMachines = (int)Math.ceil((double) dayValue / dailyValue);

        return dayMachines >= preMachines;
    }

    /**
     * 获取初始模具
     * @param newOnLineDay 新的上机日
     * @param mpFinalVo 定稿Vo
     * @return
     */
    private int getStartMould(MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj,Map<String,Object> paramMap,Integer newOnLineDay, FactoryMonthPlanFinalAdjustVo mpFinalVo,MpDailyCapacityLimitVo dailyCapacityLimitVo){
        int startMould = 2;
        String dayField = FactoryConstant.DAY_FIELD + newOnLineDay;
        if (mpFinalVo.getFieldValueByFieldName(dayField) == null || (Integer) mpFinalVo.getFieldValueByFieldName(dayField) == 0){
            return startMould;
        }
        // 原模具数据+新增2副模
        return getMouldByDay(adjustDailyCapacityLimitObj,paramMap,newOnLineDay,mpFinalVo,dailyCapacityLimitVo) + startMould;
    }

    /**
     * 按日获取模具数
     * @param adjustDailyCapacityLimitObj 日产能限制实例
     * @param paramMap 参数Map
     * @param iDay 每日
     * @param mpFinalVo 定稿对象
     * @param dailyCapacityLimitVo 日产能限制Vo
     * @return 日模具数
     */
    private int getMouldByDay(MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj,Map<String,Object> paramMap,Integer iDay, FactoryMonthPlanFinalAdjustVo mpFinalVo,MpDailyCapacityLimitVo dailyCapacityLimitVo){
        String dayField = FactoryConstant.DAY_FIELD + iDay;
        String day1Field = FactoryConstant.DAY_FIELD + (iDay -1 < FactoryConstant.MONTH_START_DAY ? FactoryConstant.MONTH_START_DAY:iDay -1);
        String day2Field = FactoryConstant.DAY_FIELD + (iDay +1 > FactoryConstant.MONTH_MAX_DAY ? FactoryConstant.MONTH_MAX_DAY:iDay +1);
        int dailyLhQty = getDayVulcanizationQty(mpFinalVo);
        if (dailyCapacityLimitVo.isOpenProductionFirstDay()){
            //若开产首日，将日硫化量等比例减，奇数+1
            dailyLhQty = adjustDailyCapacityLimitObj.getProportionalDeductQty(dailyCapacityLimitVo,dailyLhQty);
        }
        int dayPlanQty = (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
        int fullMachines = dayPlanQty / dailyLhQty;
        int otherMachines;
        if (adjustDailyCapacityLimitObj.isDecMould(mpFinalVo,dayField,day1Field)){
            // 统计有余数的SKU个数
            otherMachines = dayPlanQty % dailyLhQty > 0 ? 1:0;
        }else{
            //增模
            int[]addMouldArr = adjustDailyCapacityLimitObj.getAddMouldMachines(mpFinalVo,dailyLhQty,paramMap,dayField,day2Field);
            otherMachines = addMouldArr[0];
        }
        return (fullMachines+otherMachines)*2;
    }

    /**
     * 设置模具变化信息
     * @param adjustDailyCapacityLimitObj 日产能限制实例
     * @param paramMap 参数Map
     * @param startDay 开始日
     * @param mpFinalVo 定稿对象
     * @param dailyCapacityMap 日产能限制Map
     * @return 模具变化信息
     */
    public void setMouldChangeInfo(MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj,Map<String,Object> paramMap,Integer startDay, FactoryMonthPlanFinalAdjustVo mpFinalVo,Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap){
        int dayMouldValue;
        int preDayMouldValue = 0;
        StringBuilder sb = new StringBuilder();
        for (int i=startDay;i<=FactoryConstant.MONTH_MAX_DAY;i++){
            if (!hasPlanByDay(mpFinalVo,i)){
                continue;
            }
            if (dailyCapacityMap.get(i) == null){
                continue;
            }
            //按日获取模具信息
            dayMouldValue = getMouldByDay(adjustDailyCapacityLimitObj,paramMap,i,mpFinalVo,dailyCapacityMap.get(i));
            if (dayMouldValue != preDayMouldValue){
                if (preDayMouldValue != 0){
                    //若不是第1笔
                    sb.append("-").append(dayMouldValue);
                }else {
                    sb.append(dayMouldValue);
                }
                preDayMouldValue = dayMouldValue;
            }
        }
        mpFinalVo.setMouldChangeInfo(sb.toString());
    }
    /**
     * 检查模具满足情况
     *
     * @param dailyCapacityLimitVo 产能限制Vo
     * @param cavityQty 型腔数
     * @return true-满足，false-不满足
     */
    private boolean checkMouldSatisfy(MpDailyCapacityLimitVo dailyCapacityLimitVo,int cavityQty){
        //型腔台数
        int patternCount = cavityQty /2;
        //主花纹向下所有SKU的模具数量 <= 主花纹.型腔数量
        return dailyCapacityLimitVo.getPatternUsedLhMachines() <= patternCount;
    }

    /**
     * 预检查 模具满足情况
     *
     * @param dailyCapacityLimitVo 产能限制Vo
     * @param cavityQty 型腔数
     * @return true-满足，false-不满足
     */
    private boolean preCheckMouldSatisfy(MpDailyCapacityLimitVo dailyCapacityLimitVo,int cavityQty){
        //型腔台数
        int patternCount = cavityQty /2;
        //主花纹向下所有SKU的模具数量 <= 主花纹.型腔数量
        return dailyCapacityLimitVo.getPatternUsedLhMachines() < patternCount;
    }
    /**
     * 获取日硫化量
     * @param mpFinalVo 定稿Vo
     * @return 日硫化量
     */
    private Integer getDayVulcanizationQty(FactoryMonthPlanFinalAdjustVo mpFinalVo) {
        // 日硫化量 = 单模硫化量 * 2；
        return mpFinalVo.getDayVulcanizationQty() * 2;
    }

    /**
     * 清空定稿表日计划量
     * @param lockNextDay 锁定次日
     * @param prodFinal 定稿表计划Vo
     */
    private int clearMpFinalDayValue(MpRollAdjustContextDTO contextDTO,int lockNextDay,FactoryMonthPlanFinalAdjustVo prodFinal){
        if (prodFinal == null){
            return 0;
        }
        int clearDayValue = 0;
        String dayField,matchDayField;
        for (int i = lockNextDay; i<=contextDTO.getStructureDeadLine(); i++) {
            dayField = FactoryConstant.DAY_FIELD + i;
            if (prodFinal.getFieldValueByFieldName(dayField) == null){
                continue;
            }
            clearDayValue += (Integer) prodFinal.getFieldValueByFieldName(dayField);
            prodFinal.setFieldValueByFieldName(dayField,null);
            matchDayField = FactoryConstant.MATCH_DAY_FIELD + i;
            prodFinal.setFieldValueByFieldName(matchDayField,null);
        }
        return clearDayValue;
    }

    /**
     * 获取的排产计划量（实单量+自带的搭配量）
     * @param contextDTO 周程滚动上下文
     * @param adjustStructInVo 结构内调整Vo
     * @param mpFinalVo 定稿Vo
     * @return 新的排产计划量
     */
    private int getNewPlanQty(MpRollAdjustContextDTO contextDTO,MpAdjustStructureIn adjustStructInVo,
                              FactoryMonthPlanFinalAdjustVo mpFinalVo,int lockNextDay){
        String dayField,matchDayField;
        // 锁定日之后的实单每日排产量;
        int iRealQty = 0;
        for (int i = lockNextDay; i<=contextDTO.getStructureDeadLine();i++){
            dayField = FactoryConstant.DAY_FIELD+i;
            if (mpFinalVo.getFieldValueByFieldName(dayField) == null){
                //从锁定次日开始，若天的值为空，直接退
                continue;
            }
            matchDayField = FactoryConstant.MATCH_DAY_FIELD+i;
            if (mpFinalVo.getFieldValueByFieldName(matchDayField) != null){
                //若搭配天的值不为空，直接退
                if ((Integer) mpFinalVo.getFieldValueByFieldName(dayField) >
                        (Integer) mpFinalVo.getFieldValueByFieldName(matchDayField)){
                    iRealQty +=  (Integer) mpFinalVo.getFieldValueByFieldName(dayField) - (Integer) mpFinalVo.getFieldValueByFieldName(matchDayField);
                }
                break;
            }
            iRealQty += (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
        }
        //实单：待调整量+ 锁定日之后的每日排产量
        iRealQty +=  adjustStructInVo.getConfirmAdjustQty();
        //实单+搭配量
        return  iRealQty + mpFinalVo.getConventionProductionQty();
    }

    /**
     * 获取的排产计划量（实单量+自带的搭配量）
     * @param contextDTO 周程滚动上下文
     * @param adjustStructOutVo 结构调整Vo
     * @param mpFinalVo 定稿Vo
     * @return 新的排产计划量
     */
    private int getNewPlanQty(MpRollAdjustContextDTO contextDTO,MpAdjustStructureOut adjustStructOutVo,
                              FactoryMonthPlanFinalAdjustVo mpFinalVo,int lockNextDay){
        String dayField,matchDayField;
        // 锁定日之后的实单每日排产量;
        int iRealQty = 0;
        for (int i = lockNextDay; i<=contextDTO.getStructureDeadLine();i++){
            dayField = FactoryConstant.DAY_FIELD+i;
            if (mpFinalVo.getFieldValueByFieldName(dayField) == null){
                //从锁定次日开始，若天的值为空，直接退
                continue;
            }
            matchDayField = FactoryConstant.MATCH_DAY_FIELD+i;
            if (mpFinalVo.getFieldValueByFieldName(matchDayField) != null){
                //若搭配天的值不为空，直接退
                break;
            }
            iRealQty += (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
        }
        //实单：待调整量+ 锁定日之后的每日排产量
        iRealQty +=  adjustStructOutVo.getConfirmAdjustQty();
        //实单+搭配量
        return  iRealQty + mpFinalVo.getConventionProductionQty();
    }

    /**
     * 结构内调整：新增SKU
     * @param incrementAdjustList 新增SKU调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void doStructureInWithNewSku(MpRollAdjustContextDTO contextDTO,
                                         List<MpAdjustStructureIn> incrementAdjustList,
                                         List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if(PubUtil.isEmpty(incrementAdjustList)){
            return;
        }
        //1.排序(量试->正式),量试中按紧急程度升序，正式中按用户调整优先级升序
        List<MpAdjustStructureIn> incAdjustBatchTrailList = incrementAdjustList.stream().filter(x->ConstructionStageEnum.TRIAL_PRODUCTION.getStage().equals(x.getConstructionStage()))
                .sorted(Comparator.comparing(MpAdjustStructureIn::getUrgencyType,Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        List<MpAdjustStructureIn> incAdjustFormalList = incrementAdjustList.stream().filter(x->ConstructionStageEnum.FORMAL_PRODUCTION.getStage().equals(x.getConstructionStage()))
                .sorted(Comparator.comparing(MpAdjustStructureIn::getAdjustPriority,Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());

        int lockNextDay = contextDTO.getLockEndDay() + 1;
        //2、排实单
        Integer newOnLineDay;
        Iterator<MpAdjustStructureIn> incAdjustFormalIter,incAdjustBatchTrailIter;
        for (int i=lockNextDay; i<=contextDTO.getStructureDeadLine();i++){
            //2.1、敲定SKU新的上机日期
            newOnLineDay = getNewOnLineDay(contextDTO, i, null);
            if (newOnLineDay == null){
                contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,新的上机日期:%s不存在,继续找下一天！", contextDTO.getStructureName(),newOnLineDay)).append(ApsConstant.DIVISION);
                continue;
            }
            //2.2、若有量试列表，优先排产
            if (PubUtil.isNotEmpty(incAdjustBatchTrailList) && getTrialNewOnlineDay(contextDTO,newOnLineDay,newOnLineDay, mpProdFinalList) != null){
                incAdjustBatchTrailIter = incAdjustBatchTrailList.iterator();
                while (incAdjustBatchTrailIter.hasNext()){
                    if (doStructureInWithNewSkuForOne(contextDTO,mpProdFinalList,incAdjustBatchTrailIter.next(),lockNextDay,newOnLineDay)){
                        incAdjustBatchTrailIter.remove();
                    }
                }
            }
            //2.3、排产正式列表
            if (PubUtil.isNotEmpty(incAdjustFormalList)){
                incAdjustFormalIter = incAdjustFormalList.iterator();
                while (incAdjustFormalIter.hasNext()){
                    if (doStructureInWithNewSkuForOne(contextDTO,mpProdFinalList,incAdjustFormalIter.next(),lockNextDay,newOnLineDay)){
                        incAdjustFormalIter.remove();
                    }
                }
            }
        }
    }

    /**
     * 新增调整记录
     * @param contextDTO 周程滚动上下文
     * @param mpProdFinalList 定稿列表
     * @param adjustStructInVo 结构内调整记录
     * @param lockNextDay 锁定次日
     * @param newOnLineDay 新的上机日
     */
    private boolean doStructureInWithNewSkuForOne(MpRollAdjustContextDTO contextDTO, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList, MpAdjustStructureIn adjustStructInVo, int lockNextDay, int newOnLineDay) {
        FactoryMonthPlanFinalAdjustVo mpFinalVo = createMpFinalAdjustVo(contextDTO, adjustStructInVo);
        //2.2、将新增的SKU纳入定稿列表(因在模拟排产时需要实时判断模数，后面没有排上，再移除)
        mpProdFinalList.add(mpFinalVo);
        contextDTO.getFactoryMonthPlanProdFinalList().add(mpFinalVo);

        String constructionStage = ConstructionStageEnum.getInstance(adjustStructInVo.getConstructionStage()).getDesc();
        //2.3、计算新需要排产的计划量 = 实单量，其中，实单量：待调整量
        Integer newPlanQty = adjustStructInVo.getConfirmAdjustQty();
        contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,施工:%s,物料编码:%s,新的上机日期:%s,新的排产量:%s", contextDTO.getStructureName(),constructionStage,mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty)).append(ApsConstant.DIVISION);

        FactoryMonthPlanFinalAdjustVo bakMpFinalVo = new FactoryMonthPlanFinalAdjustVo();
        BeanUtils.copyProperties(mpFinalVo,bakMpFinalVo);

        //2.4、增模排产,挤占空产能
        contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,施工:%s,--增模排产,物料编码:%s,开始！", contextDTO.getStructureName(),constructionStage,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
        int remainPlanQty = incMouldProduction(mpProdFinalList, contextDTO, newOnLineDay, newPlanQty, mpFinalVo,bakMpFinalVo);
        contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,施工:%s,--增模排产,物料编码:%s,结束！还有剩余排产计划量:%s", contextDTO.getStructureName(),constructionStage,mpFinalVo.getMaterialCode(),remainPlanQty)).append(ApsConstant.DIVISION);
        //提示：物料编码:%s,新上机日:%s,模拟排产结果:{排产计划量:%s,剩余未排:%s}！
        addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.newSku.simulateResult"),mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty,newPlanQty-remainPlanQty,remainPlanQty));
        //2.5、若还有剩余，向前挤占其他SKU的搭配量
        if (remainPlanQty > 0){
            // 若剩余量 > 0，说明实单还有剩余
            // 日期向前，依次扣减其他SKU的搭配量，并模拟挤占
            Integer newEndDay = newOnLineDay == lockNextDay ? lockNextDay :newOnLineDay-1;
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,施工:%s,物料编码:%s,扣减其他SKU的搭配-开始！", contextDTO.getStructureName(),constructionStage, mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            deductMatchOtherSku(contextDTO, lockNextDay,newEndDay,newPlanQty,mpFinalVo, mpProdFinalList);
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,施工:%s,物料编码:%s,扣减其他SKU的搭配-结束！", contextDTO.getStructureName(),constructionStage, mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
        }

        //2.6、若当前SKU没有排上，则移除
        int productionQty = getProductionQty(newOnLineDay, contextDTO.getStructureDeadLine(),mpFinalVo);
        if (productionQty <=0){
            mpProdFinalList.removeIf(item -> item.getMaterialCode().equals(mpFinalVo.getMaterialCode()));
            contextDTO.getFactoryMonthPlanProdFinalList().removeIf(item -> item.getMaterialCode().equals(mpFinalVo.getMaterialCode()));
            //提示：新增的物料编码:%s,施工:%s,排产日:%s,计划调整量:%s,因产能限制,没有排产上！
            addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.newSku.noProduction"),mpFinalVo.getMaterialCode(),constructionStage,newOnLineDay,newPlanQty));
            return false;
        }else{
            //3.重置各优先级总排产量
            productionQty += getProductionQty(lockNextDay, newOnLineDay-1,mpFinalVo);
            resetTotalProductionQty(adjustStructInVo,mpFinalVo,productionQty);
            //4.重置开始日\结束日\汇总值
            resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),mpFinalVo);
            //提示：新增的物料编码:%s,施工:%s,排产日:%s,计划调整量:%s,实际调整量:%s！
            addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.newSku.realProduction"),mpFinalVo.getMaterialCode(),constructionStage,newOnLineDay,newPlanQty,productionQty));
        }
        return true;
    }

    /**
     * 重置各优先级总排产量
     * @param adjustStructInVo 调整记录
     * @param mpFinalVo 定稿记录
     * @param productionQty 排产量
     */
    private void resetTotalProductionQty(MpAdjustStructureIn adjustStructInVo, FactoryMonthPlanFinalAdjustVo mpFinalVo, int productionQty){

        if (ConstructionStageEnum.TRIAL_PRODUCTION.getStage().equals(adjustStructInVo.getConstructionStage())){
            mpFinalVo.setTrialProductionQty(productionQty);
        }else{

            int bQty;
            if (adjustStructInVo.getPostponeQty()>0 && productionQty >0){
                //有暂缓需求 且 有设调整优先级
                if (adjustStructInVo.getAdjustPriority() != null && adjustStructInVo.getAdjustPriority()>0){
                    mpFinalVo.setPostponeProductionQty(adjustStructInVo.getPostponeQty());
                    productionQty -= adjustStructInVo.getPostponeQty();
                }
            }
            if (adjustStructInVo.getHeightQty()>0 && productionQty >0){
                //有高优先级需求
                bQty = productionQty >= adjustStructInVo.getHeightQty() ? adjustStructInVo.getHeightQty():productionQty;
                mpFinalVo.setHeightProductionQty(bQty);
                productionQty -= bQty;
            }
            if (adjustStructInVo.getCycleReserveQty()>0 && productionQty >0){
                //有周期性需求
                bQty = productionQty >= adjustStructInVo.getCycleReserveQty() ? adjustStructInVo.getCycleReserveQty():productionQty;
                mpFinalVo.setCycleProductionQty(bQty);
                productionQty -= bQty;
            }
            //其他全归到 中优先级需求
            if (productionQty >0){
                mpFinalVo.setMidProductionQty(productionQty);
            }
        }
    }

    /**
     * 重置各优先级总排产量
     * @param adjustStructOutVo 调整记录
     * @param mpFinalVo 定稿记录
     * @param productionQty 排产量
     */
    private void resetTotalProductionQty(MpAdjustStructureOut adjustStructOutVo, FactoryMonthPlanFinalAdjustVo mpFinalVo, int productionQty){

        if (ConstructionStageEnum.TRIAL_PRODUCTION.getStage().equals(adjustStructOutVo.getConstructionStage())){
            mpFinalVo.setTrialProductionQty(productionQty);
        }else{

            int bQty;
            if (adjustStructOutVo.getPostponeQty()>0 && productionQty >0){
                //有暂缓需求 且 有设调整优先级
                if (adjustStructOutVo.getAdjustPriority() != null && adjustStructOutVo.getAdjustPriority()>0){
                    mpFinalVo.setPostponeProductionQty(adjustStructOutVo.getPostponeQty());
                    productionQty -= adjustStructOutVo.getPostponeQty();
                }
            }
            if (adjustStructOutVo.getHeightQty()>0 && productionQty >0){
                //有高优先级需求
                bQty = productionQty >= adjustStructOutVo.getHeightQty() ? adjustStructOutVo.getHeightQty():productionQty;
                mpFinalVo.setHeightProductionQty(bQty);
                productionQty -= bQty;
            }
            if (adjustStructOutVo.getCycleReserveQty()>0 && productionQty >0){
                //有周期性需求
                bQty = productionQty >= adjustStructOutVo.getCycleReserveQty() ? adjustStructOutVo.getCycleReserveQty():productionQty;
                mpFinalVo.setCycleProductionQty(bQty);
                productionQty -= bQty;
            }
            //其他全归到 中优先级需求
            if (productionQty >0){
                mpFinalVo.setMidProductionQty(productionQty);
            }
        }
    }

    /**
     * 检查是否有排产
     * @param startDay 开始日
     * @param endDay 开始日
     * @param mpFinalVo 定稿Vo
     * @return
     */
    private int getProductionQty(int startDay,int endDay,FactoryMonthPlanFinalAdjustVo mpFinalVo){
        String dayField;
        int productionQty = 0;
        for (int i = startDay; i <= endDay; i++){
            dayField = FactoryConstant.DAY_FIELD + i;
            if (mpFinalVo.getFieldValueByFieldName(dayField) != null &&
                    (Integer) mpFinalVo.getFieldValueByFieldName(dayField) != 0){
                productionQty += (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
            }
        }
        return productionQty;
    }

    /**
     * 重置开始/结束日期
     * @param startDay 开始日
     * @param endDay 开始日
     * @param mpProdFinalList 定稿Vo列表
     * @return
     */
    /*private void resetBegin2EndDay2TotalQty(int startDay, int endDay, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList){
        String dayField;
        int accTotalQty;
        for (FactoryMonthPlanFinalAdjustVo mpFinalVo:mpProdFinalList){
            int realBeginDay = FactoryConstant.MONTH_MAX_DAY+1;
            int realEndDay = 0;
            accTotalQty = 0;
            for (int i = startDay; i <= endDay; i++){
                dayField = FactoryConstant.DAY_FIELD + i;
                if (mpFinalVo.getFieldValueByFieldName(dayField) != null &&
                        (Integer) mpFinalVo.getFieldValueByFieldName(dayField) != 0){
                    if (realBeginDay > i){
                        realBeginDay = i;
                    }
                    if (realEndDay < i){
                        realEndDay = i;
                    }
                    accTotalQty += (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
                }
            }
            mpFinalVo.setBeginDay(realBeginDay==FactoryConstant.MONTH_MAX_DAY+1 ? 0:realBeginDay);
            mpFinalVo.setEndDay(realEndDay);
            mpFinalVo.setTotalQty(accTotalQty);
            //实际调整量 = 累计排产量 - 原实际排产量
            int oriTotalQty = mpFinalVo.getOriTotalQty()== null ? 0:mpFinalVo.getOriTotalQty();
            mpFinalVo.setActualAdjustQty(accTotalQty - oriTotalQty);
        }
    }*/

    /**
     * 重置开始/结束日期/计划量
     * @param endDay 结构收尾日
     * @param mpFinalVo 定稿Vo
     * @return
     */
    private void resetBegin2EndDay2TotalQty(int startDay, int endDay, FactoryMonthPlanFinalAdjustVo mpFinalVo){
        String dayField;
        int accTotalQty = 0;
        int realBeginDay = FactoryConstant.MONTH_MAX_DAY+1;
        int realEndDay = 0;
        for (int i = startDay; i <= endDay; i++){
            dayField = FactoryConstant.DAY_FIELD + i;
            if (mpFinalVo.getFieldValueByFieldName(dayField) != null &&
                    (Integer) mpFinalVo.getFieldValueByFieldName(dayField) != 0){
                if (realBeginDay > i){
                    realBeginDay = i;
                }
                if (realEndDay < i){
                    realEndDay = i;
                }
                accTotalQty += (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
            }
        }
        mpFinalVo.setBeginDay(realBeginDay==FactoryConstant.MONTH_MAX_DAY+1 ? 0:realBeginDay);
        mpFinalVo.setEndDay(realEndDay);
        mpFinalVo.setTotalQty(accTotalQty);
        //实际调整量 = 累计排产量 - 原实际排产量
        int oriTotalQty = mpFinalVo.getOriTotalQty()== null ? 0:mpFinalVo.getOriTotalQty();
        mpFinalVo.setActualAdjustQty(accTotalQty - oriTotalQty);
    }

    /**
     * 结构调整：新增SKU
     * @param incrementAdjustList 新增SKU调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void doStructureOutWithNewSku(MpRollAdjustContextDTO contextDTO,
                                          List<MpAdjustStructureOut> incrementAdjustList,
                                          List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if(PubUtil.isEmpty(incrementAdjustList)){
            return;
        }
        //1、排序
        incrementAdjustList = incrementAdjustList.stream().sorted(Comparator.comparing(MpAdjustStructureOut::getAdjustPriority,Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        int lockNextDay = contextDTO.getLockEndDay() + 1;
        Integer newOnLineDay,newPlanQty,newEndDay;
        FactoryMonthPlanFinalAdjustVo mpFinalVo,bakMpFinalVo;
        //2、排实单
        int iOrder = 0;
        for (MpAdjustStructureOut adjustStructOutVo:incrementAdjustList){
            mpFinalVo = createMpFinalAdjustVo(contextDTO, adjustStructOutVo);
            iOrder += 1;
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,排序:%s,物料编码:%s,开始日:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),mpFinalVo.getBeginDay())).append(ApsConstant.DIVISION);
            //2.1、敲定在机SKU新的上机日期
            newOnLineDay = getNewOnLineDayForStructOut(contextDTO, lockNextDay);
            if (newOnLineDay == null){
                contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,排序:%s,物料编码:%s,没有获取到新的上机日期,有可能上机日与结构收尾日重叠,退出！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                continue;
            }
            //2.2、将新增的SKU纳入定稿列表(因在模拟排产时需要实时判断模数，后面没有排上，再移除)
            mpProdFinalList.add(mpFinalVo);
            contextDTO.getFactoryMonthPlanProdFinalList().add(mpFinalVo);

            //2.3、计算新需要排产的计划量 = 实单量，其中，实单量：待调整量
            newPlanQty = adjustStructOutVo.getConfirmAdjustQty();
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,排序:%s,物料编码:%s,新的上机日期:%s,新的排产量:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty)).append(ApsConstant.DIVISION);
            bakMpFinalVo = new FactoryMonthPlanFinalAdjustVo();
            BeanUtils.copyProperties(mpFinalVo,bakMpFinalVo);
            //2.4、增模排产,挤占空产能
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】--增模排产,排序:%s,物料编码:%s,开始！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            int remainPlanQty = incMouldProduction(mpProdFinalList, contextDTO, newOnLineDay, newPlanQty, mpFinalVo,bakMpFinalVo);
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】--增模排产,排序:%s,物料编码:%s,结束！还有剩余排产计划量:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),remainPlanQty)).append(ApsConstant.DIVISION);
            //提示：物料编码:%s,新上机日:%s,模拟排产结果:{排产计划量:%s,剩余未排:%s}！
            addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.newSku.simulateResult"),mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty,newPlanQty-remainPlanQty,remainPlanQty));
            //2.5、若还有剩余，向前挤占其他SKU的搭配量
            if (remainPlanQty > 0){
                // 若剩余量 > 0，说明实单还有剩余
                // 日期向前，依次扣减其他SKU的搭配量，并模拟挤占
                newEndDay = newOnLineDay == lockNextDay ? lockNextDay:newOnLineDay-1;
                contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,物料编码:%s,扣减其他SKU的搭配-开始！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                deductMatchOtherSku(contextDTO,lockNextDay,newEndDay,newPlanQty,mpFinalVo,mpProdFinalList);
                contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,物料编码:%s,扣减其他SKU的搭配-结束！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            }

            //2.6、若当前SKU没有排上，则移除
            int productionQty = getProductionQty(newOnLineDay, contextDTO.getStructureDeadLine(),mpFinalVo);
            if (productionQty <=0){
                mpProdFinalList.removeIf(item -> item.getMaterialCode().equals(adjustStructOutVo.getMaterialCode()));
                contextDTO.getFactoryMonthPlanProdFinalList().removeIf(item -> item.getMaterialCode().equals(adjustStructOutVo.getMaterialCode()));
                //提示：新增的物料编码:%s,施工:%s,排产日:%s,计划调整量:%s,因产能限制,没有排产上！
                addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.newSku.noProduction"),mpFinalVo.getMaterialCode(),"正式",newOnLineDay,newPlanQty));
            }else{
                //重置各优先级总排产量
                productionQty += getProductionQty(lockNextDay, newOnLineDay-1,mpFinalVo);
                resetTotalProductionQty(adjustStructOutVo,mpFinalVo,productionQty);
                //重置开始日\结束日\汇总值
                resetBegin2EndDay2TotalQty(contextDTO.getStructureStartDay(),contextDTO.getStructureDeadLine(),mpFinalVo);
                //提示：新增的物料编码:%s,施工:%s,排产日:%s,计划调整量:%s,实际调整量:%s！
                addAdjustProcLog(contextDTO,mpFinalVo,String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.log.newSku.realProduction"),mpFinalVo.getMaterialCode(),"正式",newOnLineDay,newPlanQty,productionQty));
            }

        }
    }

    /**
     * 创建定稿记录对象
     * @param contextDTO 周程滚动上下文
     * @param adjustStructInVo 结构内调整Vo
     * @return 定稿记录对象
     */
    private FactoryMonthPlanFinalAdjustVo createMpFinalAdjustVo(MpRollAdjustContextDTO contextDTO, MpAdjustStructureIn adjustStructInVo) {
        FactoryMonthPlanFinalAdjustVo mpFinalVo = new FactoryMonthPlanFinalAdjustVo();
        mpFinalVo.setFactoryCode(contextDTO.getFactoryCode());
        mpFinalVo.setYear(contextDTO.getMpYear());
        mpFinalVo.setMonth(contextDTO.getMpMonth());
        String yearAndMonth = String.format("%s%02d", contextDTO.getMpYear(), contextDTO.getMpMonth());
        mpFinalVo.setYearMonth(Integer.valueOf(yearAndMonth));
        mpFinalVo.setMonthPlanVersion(adjustStructInVo.getMonthPlanVersion());
        mpFinalVo.setProductionVersion(adjustStructInVo.getProductionVersion());
        mpFinalVo.setLastMonthPlanVersion(adjustStructInVo.getVersion());
        mpFinalVo.setStructureName(adjustStructInVo.getStructureName());
        mpFinalVo.setCxMachineCode(adjustStructInVo.getScheduledMachines());
        mpFinalVo.setProductTypeCode(adjustStructInVo.getProductTypeCode());
        mpFinalVo.setProductionType(adjustStructInVo.getProductionType());
        mpFinalVo.setProductStatus(adjustStructInVo.getProductStatus());
        mpFinalVo.setMainMaterialDesc(adjustStructInVo.getMainMaterialDesc());
        mpFinalVo.setMesMaterialCode(adjustStructInVo.getMesMaterialCode());
        mpFinalVo.setMaterialCode(adjustStructInVo.getMaterialCode());
        mpFinalVo.setMaterialDesc(adjustStructInVo.getMaterialDesc());
        mpFinalVo.setConstructionStage(adjustStructInVo.getConstructionStage());
        mpFinalVo.setBrand(adjustStructInVo.getBrand());
        mpFinalVo.setProSize(adjustStructInVo.getProSize());
        mpFinalVo.setSpecifications(adjustStructInVo.getSpecifications());
        mpFinalVo.setMainPattern(adjustStructInVo.getMainPattern());
        mpFinalVo.setPattern(adjustStructInVo.getPattern());
        mpFinalVo.setMouldCavityQty(adjustStructInVo.getMouldCavityQty());
        mpFinalVo.setTypeBlockQty(adjustStructInVo.getTypeBlockQty());
        mpFinalVo.setDayVulcanizationQty(adjustStructInVo.getDayVulcanizationQty());
        mpFinalVo.setCuringTime(adjustStructInVo.getCuringTime());
        mpFinalVo.setHasSpecialMaterial(adjustStructInVo.getHasSpecialMaterial());

        mpFinalVo.setPostponeProductionQty( 0);
        mpFinalVo.setHeightProductionQty(0);
        mpFinalVo.setCycleProductionQty(0);
        mpFinalVo.setMidProductionQty(0);
        mpFinalVo.setConventionProductionQty(0);
        mpFinalVo.setTrialProductionQty(0);
        mpFinalVo.setTotalQty(0);
        mpFinalVo.setOriTotalQty(0);
        mpFinalVo.setAdjustDetail(new StringBuilder());
        mpFinalVo.setAdjustDetailId(Convert.toStr(adjustStructInVo.getId(), null));
        return mpFinalVo;
    }
    /**
     * 创建定稿记录对象 for 结构调整
     * @param contextDTO 周程滚动上下文
     * @param adjustStructOutVo 结构调整Vo
     * @return 定稿记录对象
     */
    private FactoryMonthPlanFinalAdjustVo createMpFinalAdjustVo(MpRollAdjustContextDTO contextDTO, MpAdjustStructureOut adjustStructOutVo) {
        FactoryMonthPlanFinalAdjustVo mpFinalVo = new FactoryMonthPlanFinalAdjustVo();
        mpFinalVo.setFactoryCode(contextDTO.getFactoryCode());
        mpFinalVo.setYear(contextDTO.getMpYear());
        mpFinalVo.setMonth(contextDTO.getMpMonth());
        String yearAndMonth = String.format("%s%02d", contextDTO.getMpYear(), contextDTO.getMpMonth());
        mpFinalVo.setYearMonth(Integer.valueOf(yearAndMonth));
        mpFinalVo.setMonthPlanVersion(adjustStructOutVo.getMonthPlanVersion());
        mpFinalVo.setProductionVersion(adjustStructOutVo.getProductionVersion());
        mpFinalVo.setLastMonthPlanVersion(adjustStructOutVo.getVersion());
        mpFinalVo.setStructureName(adjustStructOutVo.getStructureName());
        mpFinalVo.setCxMachineCode(adjustStructOutVo.getScheduledMachines());
        mpFinalVo.setProductTypeCode(adjustStructOutVo.getProductTypeCode());
        mpFinalVo.setProductionType(adjustStructOutVo.getProductionType());
        mpFinalVo.setProductStatus(adjustStructOutVo.getProductStatus());
        mpFinalVo.setMainMaterialDesc(adjustStructOutVo.getMainMaterialDesc());
        mpFinalVo.setMesMaterialCode(adjustStructOutVo.getMesMaterialCode());
        mpFinalVo.setMaterialCode(adjustStructOutVo.getMaterialCode());
        mpFinalVo.setMaterialDesc(adjustStructOutVo.getMaterialDesc());
        mpFinalVo.setConstructionStage(adjustStructOutVo.getConstructionStage());
        mpFinalVo.setBrand(adjustStructOutVo.getBrand());
        mpFinalVo.setProSize(adjustStructOutVo.getProSize());
        mpFinalVo.setSpecifications(adjustStructOutVo.getSpecifications());
        mpFinalVo.setMainPattern(adjustStructOutVo.getMainPattern());
        mpFinalVo.setPattern(adjustStructOutVo.getPattern());
        mpFinalVo.setMouldCavityQty(adjustStructOutVo.getMouldCavityQty());
        mpFinalVo.setTypeBlockQty(adjustStructOutVo.getTypeBlockQty());
        mpFinalVo.setDayVulcanizationQty(adjustStructOutVo.getDayVulcanizationQty());
        mpFinalVo.setCuringTime(adjustStructOutVo.getCuringTime());
        mpFinalVo.setHasSpecialMaterial(adjustStructOutVo.getHasSpecialMaterial());

        mpFinalVo.setPostponeProductionQty( 0);
        mpFinalVo.setHeightProductionQty(0);
        mpFinalVo.setCycleProductionQty(0);
        mpFinalVo.setMidProductionQty(0);
        mpFinalVo.setConventionProductionQty(0);
        mpFinalVo.setTotalQty(0);
        mpFinalVo.setOriTotalQty(0);
        mpFinalVo.setAdjustDetail(new StringBuilder());
        mpFinalVo.setAdjustDetailId(Convert.toStr(adjustStructOutVo.getId(), null));
        return mpFinalVo;
    }
}
