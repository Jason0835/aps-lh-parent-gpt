package com.zlt.aps.monthplan.adjust.engine;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.capacity.MpAdjustDailyCapacityLimit;
import com.zlt.aps.factory.deduct.DeductMouldScheduler;
import com.zlt.aps.monthplan.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.monthplan.api.domain.deduct.DailyScheduleVo;
import com.zlt.aps.monthplan.api.domain.deduct.DeductMouldVo;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureOut;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.common.utils.PubUtil;
import com.zlt.aps.monthplan.common.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
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


    /**
     * 结构内调整，按结构分别调整
     * @param contextDTO 周程滚动调整上下文
     * @param mpAdjustStructureInList 结构内调整记录列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    public void structureInAdjustForOne(MpRollAdjustContextDTO contextDTO,List<MpAdjustStructureIn> mpAdjustStructureInList,List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        //1.解析出关单/减量、在产SKU、新增SKU以及暂缓
        List<MpAdjustStructureIn> deductAdjustList = new ArrayList<>();
        List<MpAdjustStructureIn> onIncrementAdjustList = new ArrayList<>();
        List<MpAdjustStructureIn> incrementAdjustList = new ArrayList<>();
        List<String> onMaterialCodeList = mpProdFinalList.stream().map(x->x.getMaterialCode()).collect(Collectors.toList());
        Date startTime,endTime;
        StringBuffer sbError = new StringBuffer();
        for (MpAdjustStructureIn adjustStructureIn:mpAdjustStructureInList){
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
            //4.1 检查日硫化量
            checkDayLhQty(sbError,adjustStructureIn);
        }
        if (!StringUtil.isEmptyWithTrim(sbError.toString())){
            throw new BusinessException(sbError.toString());
        }
        //2.减量调整
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        structureInAdjustWithDeduct(contextDTO,deductAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //3、初始日产能限制
        // 锁定次日 作为 可开始日
        int lockNextDay = contextDTO.getLockEndDay() + 1;
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = new MpAdjustDailyCapacityLimit().getDailyCapacityLimitMap(lockNextDay,mpProdFinalList,contextDTO.getOneStructureAllocationList());
        contextDTO.setDailyCapacityLimitVoMap(dailyCapacityLimitVoMap);
        //4、拆出搭配量，用于快速判断是否搭配
        mpProdFinalList.stream().forEach(x->{
            if (x.getConventionProductionQty() >0) {
                splitMatchQtyByDay(contextDTO,x.getConventionProductionQty(), lockNextDay,x);
            }
        });

        //5.在机SKU增量
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        structureInAdjustWithOnIncrement(contextDTO,onIncrementAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //6.新增SKU
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        incrementAdjustList = incrementAdjustList.stream().sorted(Comparator.comparing(MpAdjustStructureIn::getAdjustPriority,Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        structureInAdjustWithIncrement(contextDTO,incrementAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //7.优化：其他SKU往前移动
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        otherSkuForwardMove(contextDTO,lockNextDay,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);
    }

    /**
     * 结构调整-结构缩短/延长
     * @param contextDTO 周程滚动调整上下文
     * @param mpAdjustStructureOutList 结构调整记录列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    public void structureOutAdjustForOne(MpRollAdjustContextDTO contextDTO, List<MpAdjustStructureOut> mpAdjustStructureOutList, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        //1.解析出关单/减量、在产SKU、新增SKU以及暂缓
        List<MpAdjustStructureOut> deductAdjustList = new ArrayList<>();
        List<MpAdjustStructureOut> onIncrementAdjustList = new ArrayList<>();
        List<MpAdjustStructureOut> incrementAdjustList = new ArrayList<>();
        List<String> onMaterialCodeList = mpProdFinalList.stream().map(x->x.getMaterialCode()).collect(Collectors.toList());
        Date startTime,endTime;
        StringBuffer sbError = new StringBuffer();
        for (MpAdjustStructureOut adjustStructureOut:mpAdjustStructureOutList){
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
            //4.1 检查日硫化量
            checkDayLhQty(sbError,adjustStructureOut);
        }
        if (!StringUtil.isEmptyWithTrim(sbError.toString())){
            throw new BusinessException(sbError.toString());
        }
        //2.减量调整
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        structureOutAdjustWithDeduct(contextDTO,deductAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //3、初始日产能限制
        // 锁定次日 作为 可开始日
        int lockNextDay = contextDTO.getLockEndDay() + 1;
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = new MpAdjustDailyCapacityLimit().getDailyCapacityLimitMap(lockNextDay,mpProdFinalList,contextDTO.getOneStructureAllocationList());
        contextDTO.setDailyCapacityLimitVoMap(dailyCapacityLimitVoMap);
        //4、拆出搭配量，用于快速判断是否搭配
        mpProdFinalList.stream().forEach(x->{
            if (x.getConventionProductionQty() >0) {
                splitMatchQtyByDay(contextDTO,x.getConventionProductionQty(), lockNextDay,x);
            }
        });

        //5.在机SKU增量
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        structureOutAdjustWithOnIncrement(contextDTO,onIncrementAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //6.新增SKU
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        incrementAdjustList = incrementAdjustList.stream().sorted(Comparator.comparing(MpAdjustStructureOut::getAdjustPriority,Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        structureOutAdjustWithIncrement(contextDTO,incrementAdjustList,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //7.优化：其他SKU往前移动
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        otherSkuForwardMove(contextDTO,lockNextDay,mpProdFinalList);
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);

        //8.若需要平移，其他
    }

    /**
     * 检查日硫化量是否为空
     * @param sbError
     * @param structureIn
     */
    private void checkDayLhQty(StringBuffer sbError, MpAdjustStructureIn structureIn){
        if (structureIn.getDayVulcanizationQty() == null || structureIn.getDayVulcanizationQty() == 0){
            sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecord.notDayLhQty"),
                    structureIn.getMaterialCode()));
        }
    }

    /**
     * 检查日硫化量是否为空
     * @param sbError
     * @param structureOut
     */
    private void checkDayLhQty(StringBuffer sbError, MpAdjustStructureOut structureOut){
        if (structureOut.getDayVulcanizationQty() == null || structureOut.getDayVulcanizationQty() == 0){
            sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecord.notDayLhQty"),
                    structureOut.getMaterialCode()));
        }
    }

    /**
     * 优化：其他SKU往前移动
     * @param contextDTO 调整上下文
     * @param lockNextDay 锁定次日
     * @param mpProdFinalList 定稿列表
     */
    private void otherSkuForwardMove(MpRollAdjustContextDTO contextDTO,int lockNextDay,
                                     List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList){
        //1、从锁定次日向后遍历排产计划，检测每日硫化机台数不超限制数且每日胎胚种类数不超限制数的日期，记为有空间的日期；
        //2、在有空间的日期向后依次找SKU，越靠近的SKU优先移动；
        //3、将SKU整体模拟往前移动到空间日期，并向后再次检测每日硫化机台数、胎胚种类数的符合性，若符合，则可以移动，否则不能移动，继续找下一个SKU；
        int secStartDay;
        List<FactoryMonthPlanFinalAdjustVo> canMoveFinalList;
        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
        //从锁定次日到月底次日，依次遍历
        for (int i = lockNextDay; i<= contextDTO.getStructureDeadLine(); i++){
            adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList,i, contextDTO.getDailyCapacityLimitVoMap().get(i), null);
            contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,排产日:%s,其产能限制信息:%s！",contextDTO.getStructureName(),i,contextDTO.getDailyCapacityLimitVoMap().get(i).toString())).append(ApsConstant.DIVISION);
            //1、检查: 当前每日硫化机台数\当前每日胎胚种类数 符合性
            if (!adjustDailyCapacityLimitObj.checkCapacitySatisfy(contextDTO.getDailyCapacityLimitVoMap().get(i))){
                contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,排产日:%s,每日硫化机台数或每日胎胚种类数不符合产能限制,退出！",contextDTO.getStructureName(),i)).append(ApsConstant.DIVISION);
                continue;
            }
            //2、从第2天开始查找SKU
            secStartDay = i+1;
            canMoveFinalList = findCanMoveSkuList(mpProdFinalList, secStartDay);
            if (PubUtil.isEmpty(canMoveFinalList)){
                //若没有可以移动的列表，则退出
                contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】,排产日:%s,排产次日:%s查找可移动的SKU,未找到退出！",contextDTO.getStructureName(),i,secStartDay)).append(ApsConstant.DIVISION);
                break;
            }
            //3、移动SKU列表，直到第I天没有剩余空间
            for (FactoryMonthPlanFinalAdjustVo finalVo:canMoveFinalList){
                //3.3、清空定稿表日计划量
                clearMpFinalDayValue(contextDTO,secStartDay, finalVo);

                //3.4、增模排产
                contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】--增模排产,排产日:%s,物料编码:%s,开始！",contextDTO.getStructureName(), i,finalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                incMouldProduction(mpProdFinalList, contextDTO, i, finalVo.getTotalQty(), finalVo);
                contextDTO.getLogDetail().append(String.format("结构:%s,【其他SKU向前移动】--增模排产,排产日:%s,物料编码:%s,结束！",contextDTO.getStructureName(), i,finalVo.getMaterialCode())).append(ApsConstant.DIVISION);

                //3.5、检查是否还有剩余空间，若没有，则退出
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
        List<FactoryMonthPlanFinalAdjustVo> finalVoList = mpProdFinalList.stream()
                .filter(x->secStartDay == x.getBeginDay()).sorted((o1, o2) -> {
                    // 自定义比较逻辑(总的已排实单量)
                    int totalQty1 = o1.getHeightProductionQty() + o1.getMidProductionQty();
                    int totalQty2 = o2.getHeightProductionQty() + o2.getMidProductionQty();
                    return Integer.compare(totalQty2,totalQty1);
            }).collect(Collectors.toList());
        if (PubUtil.isNotEmpty(finalVoList)){
            return finalVoList;
        }
        if (secStartDay == FactoryConstant.MONTH_MAX_DAY){
            //若第2天可开始日 已到月底最后1天，则退出
            return null;
        }
        // 加1天，递归查找
        return findCanMoveSkuList(mpProdFinalList,secStartDay+1);
    }

    /**
     * 结构内调整：减量
     * @param contextDTO 周程滚动调整上下文
     * @param deductAdjustList 减量调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void structureInAdjustWithDeduct(MpRollAdjustContextDTO contextDTO,
                                             List<MpAdjustStructureIn> deductAdjustList,List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
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
            //剩余调整量绝对值
            reAdjustQty =  Math.abs(deductAdjust.getConfirmAdjustQty());
            //2、先设置锁定量，再按高到中依次扣减排产量
            //设置锁定量
            setLockQty(contextDTO.getLockEndDay(),mpFinalVo);
            //允许扣减量,实单-锁定量
            int allowDeductQty = calcAllowDeductQty(mpFinalVo);
            needDeductQty = allowDeductQty >= reAdjustQty ? reAdjustQty:allowDeductQty;
            contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,物料编码:%s,确认调整量:%s,实单量:%s,锁定量:%s,允许扣减量:%s",contextDTO.getStructureName(),deductAdjust.getMaterialCode(),reAdjustQty,getRealBillQty(mpFinalVo),mpFinalVo.getLockQty(),allowDeductQty)).append(ApsConstant.DIVISION);
            needDeductProductionQty(contextDTO,needDeductQty, mpFinalVo);
            //3、遍历31天日排产量，根据实际扣减量依次扣减
            deductScheduleQtyByDay(contextDTO, mpFinalVo);
        }
    }

    /**
     * 结构调整：减量
     * @param contextDTO 周程滚动调整上下文
     * @param deductAdjustList 减量调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void structureOutAdjustWithDeduct(MpRollAdjustContextDTO contextDTO,
                                             List<MpAdjustStructureOut> deductAdjustList,List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
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
            //剩余调整量绝对值
            reAdjustQty =  Math.abs(deductAdjust.getConfirmAdjustQty());
            //2、先设置锁定量，再按高到中依次扣减排产量
            //设置锁定量
            setLockQty(contextDTO.getLockEndDay(),mpFinalVo);
            //允许扣减量,实单-锁定量
            int allowDeductQty = calcAllowDeductQty(mpFinalVo);
            needDeductQty = allowDeductQty >= reAdjustQty ? reAdjustQty:allowDeductQty;
            contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】,物料编码:%s,确认调整量:%s,实单量:%s,锁定量:%s,允许扣减量:%s",contextDTO.getStructureName(),deductAdjust.getMaterialCode(),reAdjustQty,getRealBillQty(mpFinalVo),mpFinalVo.getLockQty(),allowDeductQty)).append(ApsConstant.DIVISION);
            needDeductProductionQty(contextDTO,needDeductQty, mpFinalVo);
            //3、遍历31天日排产量，根据实际扣减量依次扣减
            deductScheduleQtyByDay(contextDTO, mpFinalVo);
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
    private void needDeductProductionQty(MpRollAdjustContextDTO contextDTO,int needDeductQty, FactoryMonthPlanFinalAdjustVo prodFinal) {
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
        prodFinal.setTotalQty(prodFinal.getTotalQty() - emptyQty);
        //将调减量置到空产能
        prodFinal.setEmptyQty(emptyQty);
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】--扣减各总排产量,物料编码:%s,调减后,高优先级排产量:%s,中优级排产量:%s,暂缓排产量:%s,空出产能:%s",contextDTO.getStructureName(), prodFinal.getMaterialCode(),prodFinal.getHeightProductionQty(),prodFinal.getMidProductionQty(),prodFinal.getPostponeProductionQty(),prodFinal.getEmptyQty())).append(ApsConstant.DIVISION);
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
        int realDeductQty = prodFinal.getEmptyQty();
        //实单肯定在前，从后向前扣减
        for (int i = FactoryConstant.MONTH_MAX_DAY; i> contextDTO.getLockEndDay(); i--){
            dayField = FactoryConstant.DAY_FIELD+i;
            if (prodFinal.getFieldValueByFieldName(dayField) == null){
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
        contextDTO.getLogDetail().append(String.format("结构:%s,【减量调整】--扣减每日排产量,物料编码:%s,需要调整量:%s,剩余调整量:%s,从后向前扣到:%s日",contextDTO.getStructureName(),prodFinal.getMaterialCode(),prodFinal.getEmptyQty(),realDeductQty,iDay)).append(ApsConstant.DIVISION);
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
            sb.append(scheduleVo.getSkuQuantity());
            iDay +=1;
        }
        contextDTO.getLogDetail().append(String.format("结构:%s,【降模排产】,物料编码:%s,降模前的计划量:%s,降模开始日:%s,降模每日计划量:%s",contextDTO.getStructureName(),prodFinal.getMaterialCode(),dayQty,iDay-1,sb.toString())).append(ApsConstant.DIVISION);
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
                prodFinal.setFieldValueByFieldName(matchDayField,dayQty - totalMatchQty);
                totalMatchQty = 0;
            }
            sb.append(prodFinal.getFieldValueByFieldName(matchDayField)).append(",");
            if (totalMatchQty == 0){
                contextDTO.getLogDetail().append(String.format("结构:%s,【拆出搭配量】,总搭配量:%s,结构收尾日:%s,搭配开始日:%s,每日搭配量:%s",contextDTO.getStructureName(), totalMatchQty,structureDeadline,i,sb.toString())).append(ApsConstant.DIVISION);
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
    private void structureInAdjustWithOnIncrement(MpRollAdjustContextDTO contextDTO,
                                                  List<MpAdjustStructureIn> onIncrementAdjustList,
                                                  List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if(PubUtil.isEmpty(onIncrementAdjustList)){
            return;
        }

        int lockNextDay = contextDTO.getLockEndDay() + 1;
        //1、排序：在机SKU上机日期早的优先增量排产
        mpProdFinalList.sort(Comparator.comparingInt(FactoryMonthPlanFinalAdjustVo::getBeginDay));
        Map<String, MpAdjustStructureIn> mpAdjustStructInMap = onIncrementAdjustList.stream().collect(Collectors.groupingBy(item->item.getMaterialCode(),
                 Collectors.collectingAndThen(Collectors.toList(),m-> {
                     return m.get(0);
                 })));

        MpAdjustStructureIn adjustStructInVo;
        Integer newOnLineDay,newPlanQty,newEndDay;

        //2、先排实单->自带的搭配
        int iOrder = 0;
        for (FactoryMonthPlanFinalAdjustVo mpFinalVo:mpProdFinalList) {
            adjustStructInVo = mpAdjustStructInMap.get(mpFinalVo.getMaterialCode());
            if (adjustStructInVo == null) {
                // 非在机SKU，继续
                continue;
            }
            iOrder += 1;
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,排序:%s,物料编码:%s,开始日:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),mpFinalVo.getBeginDay())).append(ApsConstant.DIVISION);
            if(mpFinalVo.getBeginDay() < lockNextDay && !hasPlanByDay(mpFinalVo,lockNextDay -1)){
                // 开始日 < 锁定日 且 锁定前日没有值,继续
                contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,排序:%s,物料编码:%s,开始日小于锁定日且锁定日之前没有值,退出！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                continue;
            }
            //2.1、敲定在机SKU新的上机日期
            newOnLineDay = getNewOnLineDay(contextDTO, lockNextDay, mpFinalVo);
            if (newOnLineDay == null){
                contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,排序:%s,物料编码:%s,没有获取到新的上机日期,退出！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                continue;
            }
            //2.2、计算新需要排产的计划量 = 实单量+自带的搭配量，其中，实单量：待调整量 + 锁定日之后的每日实单排产量
            newPlanQty = getNewPlanQty(contextDTO,adjustStructInVo,mpFinalVo,lockNextDay);
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,排序:%s,物料编码:%s,新的上机日期:%s,新的排产量:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty)).append(ApsConstant.DIVISION);
            //2.3、清空定稿表日计划量
            clearMpFinalDayValue(contextDTO,lockNextDay, mpFinalVo);

            //2.4、增模排产
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】--增模排产,排序:%s,物料编码:%s,开始！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            int remainPlanQty = incMouldProduction(mpProdFinalList, contextDTO, newOnLineDay, newPlanQty, mpFinalVo);
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】--增模排产,排序:%s,物料编码:%s,结束！还有剩余排产计划量:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),remainPlanQty)).append(ApsConstant.DIVISION);
            if (remainPlanQty > mpFinalVo.getConventionProductionQty()){
                // 若剩余量 > 搭配量，说明实单还有剩余
                // 实单剩余  = 剩余量 - 搭配量
                remainPlanQty -= mpFinalVo.getConventionProductionQty();
                // 本身搭配被挤掉，置0
                mpFinalVo.setConventionProductionQty(0);
                // 日期向前，依次扣减其他SKU的搭配量，并模拟挤占
                newEndDay = newOnLineDay == lockNextDay ? lockNextDay:newOnLineDay-1;
                contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,扣减其他SKU的搭配-开始！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                deductMatchOtherSku(contextDTO,lockNextDay,newEndDay,remainPlanQty,mpFinalVo,mpProdFinalList);
                contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,扣减其他SKU的搭配-结束！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            }else {

                if (mpFinalVo.getConventionProductionQty() >= remainPlanQty){
                    mpFinalVo.setConventionProductionQty(remainPlanQty);
                }
            }
            //2.5、重置一下搭配排产量标识
            if (mpFinalVo.getConventionProductionQty()>0){
                splitMatchQtyByDay(contextDTO,mpFinalVo.getConventionProductionQty(), lockNextDay,mpFinalVo);
            }
        }
    }

    /**
     * 结构调整：在机SKU增量
     * @param contextDTO 周程滚动调整上下文
     * @param onIncrementAdjustList 在机SKU增量调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void structureOutAdjustWithOnIncrement(MpRollAdjustContextDTO contextDTO,
                                                  List<MpAdjustStructureOut> onIncrementAdjustList,
                                                  List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if(PubUtil.isEmpty(onIncrementAdjustList)){
            return;
        }

        int lockNextDay = contextDTO.getLockEndDay() + 1;
        //1、排序：在机SKU上机日期早的优先增量排产
        mpProdFinalList.sort(Comparator.comparingInt(FactoryMonthPlanFinalAdjustVo::getBeginDay));
        Map<String, MpAdjustStructureOut> mpAdjustStructOutMap = onIncrementAdjustList.stream().collect(Collectors.groupingBy(item->item.getMaterialCode(),
                Collectors.collectingAndThen(Collectors.toList(),m-> {
                    return m.get(0);
                })));

        MpAdjustStructureOut adjustStructOutVo;
        Integer newOnLineDay,newPlanQty,newEndDay;

        //2、先排实单->自带的搭配
        int iOrder = 0;
        for (FactoryMonthPlanFinalAdjustVo mpFinalVo:mpProdFinalList) {
            adjustStructOutVo = mpAdjustStructOutMap.get(mpFinalVo.getMaterialCode());
            if (adjustStructOutVo == null) {
                // 非在机SKU，继续
                continue;
            }
            iOrder += 1;
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,排序:%s,物料编码:%s,开始日:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),mpFinalVo.getBeginDay())).append(ApsConstant.DIVISION);
            if(mpFinalVo.getBeginDay() < lockNextDay && !hasPlanByDay(mpFinalVo,lockNextDay -1)){
                // 开始日 < 锁定日 且 锁定前日没有值,继续
                contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,排序:%s,物料编码:%s,开始日小于锁定日且锁定日之前没有值,退出！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                continue;
            }
            //2.1、敲定在机SKU新的上机日期
            newOnLineDay = getNewOnLineDayForStructOut(contextDTO, lockNextDay, mpFinalVo);
            if (newOnLineDay == null){
                contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,排序:%s,物料编码:%s,没有获取到新的上机日期,退出！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                continue;
            }
            //2.2、计算新需要排产的计划量 = 实单量+自带的搭配量，其中，实单量：待调整量 + 锁定日之后的每日实单排产量
            newPlanQty = getNewPlanQty(contextDTO,adjustStructOutVo,mpFinalVo,lockNextDay);
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,排序:%s,物料编码:%s,新的上机日期:%s,新的排产量:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty)).append(ApsConstant.DIVISION);
            //2.3、清空定稿表日计划量
            clearMpFinalDayValue(contextDTO,lockNextDay, mpFinalVo);

            //2.4、增模排产
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】--增模排产,排序:%s,物料编码:%s,开始！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            int remainPlanQty = incMouldProduction(mpProdFinalList, contextDTO, newOnLineDay, newPlanQty, mpFinalVo);
            contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】--增模排产,排序:%s,物料编码:%s,结束！还有剩余排产计划量:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),remainPlanQty)).append(ApsConstant.DIVISION);
            if (remainPlanQty > mpFinalVo.getConventionProductionQty()){
                // 若剩余量 > 搭配量，说明实单还有剩余
                // 实单剩余  = 剩余量 - 搭配量
                remainPlanQty -= mpFinalVo.getConventionProductionQty();
                // 本身搭配被挤掉，置0
                mpFinalVo.setConventionProductionQty(0);
                // 日期向前，依次扣减其他SKU的搭配量，并模拟挤占
                newEndDay = newOnLineDay == lockNextDay ? lockNextDay:newOnLineDay-1;
                contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,扣减其他SKU的搭配-开始！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                deductMatchOtherSku(contextDTO,lockNextDay,newEndDay,remainPlanQty,mpFinalVo,mpProdFinalList);
                contextDTO.getLogDetail().append(String.format("结构:%s,【在机SKU增量】,物料编码:%s,扣减其他SKU的搭配-结束！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            }else {

                if (mpFinalVo.getConventionProductionQty() >= remainPlanQty){
                    mpFinalVo.setConventionProductionQty(remainPlanQty);
                }
            }
            //2.5、重置一下搭配排产量标识
            if (mpFinalVo.getConventionProductionQty()>0){
                splitMatchQtyByDay(contextDTO,mpFinalVo.getConventionProductionQty(), lockNextDay,mpFinalVo);
            }
        }
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
     * @param mpFinalVo 定稿Vo
     * @return 新上机日
     */
    private Integer getNewOnLineDayForStructOut(MpRollAdjustContextDTO contextDTO, int lockNextDay, FactoryMonthPlanFinalAdjustVo mpFinalVo) {
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
     * @param startDay 锁定次日
     * @param endDay 结束日（新上机日向前）
     * @param remainPlanQty 剩余计划量
     * @param curFinalVo 当前定稿Vo
     * @param mpProdFinalList 定稿列表
     */
    private void deductMatchOtherSku(MpRollAdjustContextDTO contextDTO,int startDay,int endDay,int remainPlanQty,FactoryMonthPlanFinalAdjustVo curFinalVo,
                                List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList){
        if (PubUtil.isEmpty(mpProdFinalList)){
            return;
        }
        if (endDay < startDay){
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】-结束日小于开始日,退出！",contextDTO.getStructureName(), curFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            return;
        }
        int startMould = getStartMould(endDay,curFinalVo);
        if (startMould > curFinalVo.getTypeBlockQty()){
            // 在机的已排模具数已达到活块数，则退出
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】-在机的已排模具数:%s 已达到活块数:%s,退出！",contextDTO.getStructureName(), curFinalVo.getMaterialCode(),startMould,curFinalVo.getTypeBlockQty())).append(ApsConstant.DIVISION);
            return;
        }
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = contextDTO.getDailyCapacityLimitVoMap();
        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
        adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList,endDay, dailyCapacityLimitVoMap.get(endDay), curFinalVo.getMainPattern());
        contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产日:%s,其产能限制信息:%s！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay,dailyCapacityLimitVoMap.get(endDay).toString())).append(ApsConstant.DIVISION);
        //检查: 当前每日硫化机台数\当前每日胎胚种类数 符合性
        //检查：主花纹向下模具数量(/2转成机台数) 符合性
        if (!adjustDailyCapacityLimitObj.checkCapacitySatisfy(dailyCapacityLimitVoMap.get(endDay)) ||
                !checkMouldSatisfy(dailyCapacityLimitVoMap.get(endDay),curFinalVo)){
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,每日硫化机台数或每日胎胚种类数不符合产能限制,退出！",contextDTO.getStructureName(),curFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            return;
        }

        // 1. 获取某日有搭配量的其他SKU定稿列表
        List<FactoryMonthPlanFinalAdjustVo> newOtherFinalList = getMatchFinalListByDay(endDay, curFinalVo, mpProdFinalList);
        if (PubUtil.isEmpty(newOtherFinalList)){
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产%s日,其他SKU没有搭配量,退出！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay)).append(ApsConstant.DIVISION);
            return;
        }
        FactoryMonthPlanFinalAdjustVo optimalFinalVo;
        while (newOtherFinalList.size() >0 ){
            // 2.从多个SKU中，匹配其他最优的定稿SKU记录
            optimalFinalVo = getOptimalOtherSku(curFinalVo, newOtherFinalList);
            // 3.清空搭配日计划 及扣减搭配总量
            int clearDayValue = clearMpFinalDayValue(contextDTO,endDay,optimalFinalVo);
            optimalFinalVo.setConventionProductionQty(optimalFinalVo.getConventionProductionQty() - clearDayValue);
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产%s日,已匹配到最优的有搭配量的物料编码:%s,减少搭配量:%s！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay,optimalFinalVo.getMaterialCode(),clearDayValue)).append(ApsConstant.DIVISION);
            // 4.增模模拟排产
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产%s日,模拟排产-开始！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay)).append(ApsConstant.DIVISION);
            remainPlanQty = incMouldProduction(mpProdFinalList, contextDTO, endDay, remainPlanQty, curFinalVo);
            contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产%s日,模拟排产-结束,剩余排产计划量:%s！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay,remainPlanQty)).append(ApsConstant.DIVISION);
            if (remainPlanQty > curFinalVo.getConventionProductionQty()){
                newOtherFinalList.remove(optimalFinalVo);
            }else{
                break;
            }
        }
        // 5.递归，扣减其他SKU的搭配量，并模拟挤占
        contextDTO.getLogDetail().append(String.format("结构:%s,物料编码:%s,【扣减其他SKU的搭配】,排产%s日,递归-开始！",contextDTO.getStructureName(),curFinalVo.getMaterialCode(),endDay-1)).append(ApsConstant.DIVISION);
        deductMatchOtherSku(contextDTO,startDay,endDay-1,remainPlanQty,curFinalVo,mpProdFinalList);
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
        int minMatchQty = 0;
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
            if (minMatchQty < tFinalVo.getConventionProductionQty() ){
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
    private int incMouldProduction(List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                    MpRollAdjustContextDTO contextDTO,
                                    Integer newOnLineDay, Integer newPlanQty, FactoryMonthPlanFinalAdjustVo mpFinalVo) {
        String dayField;
        int dayValue;
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = contextDTO.getDailyCapacityLimitVoMap();
        int structureDeadLine = contextDTO.getStructureDeadLine();
        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
        int startMould = getStartMould(newOnLineDay,mpFinalVo);
        int dayVulcanizationQty = getDayVulcanizationQty(mpFinalVo);

        while (newPlanQty > 0){
            contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,尝试增模具数:%s！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),startMould)).append(ApsConstant.DIVISION);
            for (int i = newOnLineDay; i<= structureDeadLine; i++){
                //SKU的模具数限制：SKU的模具数<=SKU活块的数量
                if (startMould > mpFinalVo.getTypeBlockQty()){
                    contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,SKU增模后的模具数:%s 大于SKU活块的数量:%s！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),startMould,mpFinalVo.getTypeBlockQty())).append(ApsConstant.DIVISION);
                    return newPlanQty < 0 ? 0:newPlanQty;
                }
                dayField = FactoryConstant.DAY_FIELD + i;
                dayValue = mpFinalVo.getFieldValueByFieldName(dayField) == null ? 0 : (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
                dayValue += dayVulcanizationQty;
                mpFinalVo.setFieldValueByFieldName(dayField,dayValue);
                adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList,i, dailyCapacityLimitVoMap.get(i), mpFinalVo.getMainPattern());
                contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,其产能限制信息:%s！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),i,dailyCapacityLimitVoMap.get(i).toString())).append(ApsConstant.DIVISION);
                //检查: 当前每日硫化机台数\当前每日胎胚种类数 符合性
                if (!adjustDailyCapacityLimitObj.checkCapacitySatisfy(dailyCapacityLimitVoMap.get(i))){
                    // 将值还原，并退出，继续加模
                    dayValue -= dayVulcanizationQty;
                    mpFinalVo.setFieldValueByFieldName(dayField,dayValue);
                    contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,每日硫化机台数或每日胎胚种类数不符合产能限制,退出！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),i)).append(ApsConstant.DIVISION);
                    break;
                }
                //检查：主花纹向下模具数量(/2转成机台数) 符合性
                if (!checkMouldSatisfy(dailyCapacityLimitVoMap.get(i),mpFinalVo)){
                    // 将值还原，并退出 外循环
                    dayValue -= dayVulcanizationQty;
                    mpFinalVo.setFieldValueByFieldName(dayField,dayValue);
                    contextDTO.getLogDetail().append(String.format("结构:%s,【增模排产】,物料编码:%s,排产日:%s,主花纹:%s,其主花纹模具数不符合产能限制,退出！",contextDTO.getStructureName(),mpFinalVo.getMaterialCode(),i,mpFinalVo.getMainPattern())).append(ApsConstant.DIVISION);
                    return newPlanQty < 0 ? 0:newPlanQty;
                }
                newPlanQty -= dayVulcanizationQty;
                if (newPlanQty <=0){
                    return 0;
                }
            }

            startMould += 2;
        }
        return newPlanQty < 0 ? 0:newPlanQty;
    }

    /**
     * 获取初始模具
     * @param newOnLineDay 新的上机日
     * @param mpFinalVo 定稿Vo
     * @return
     */
    private int getStartMould(Integer newOnLineDay, FactoryMonthPlanFinalAdjustVo mpFinalVo){
        int startMould = 2;
        String dayField = FactoryConstant.DAY_FIELD + newOnLineDay;
        if (mpFinalVo.getFieldValueByFieldName(dayField) == null || (Integer) mpFinalVo.getFieldValueByFieldName(dayField) == 0){
            return startMould;
        }
        int dayValue = (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
        // 原模具数据+新增2副模
        return (int)Math.ceil((double) dayValue / mpFinalVo.getDayVulcanizationQty()) + startMould;
    }

    /**
     * 检查模具满足情况
     *
     * @param dailyCapacityLimitVo 产能限制Vo
     * @param mpFinalVo 定稿Vo
     * @return true-满足，false-不满足
     */
    private boolean checkMouldSatisfy(MpDailyCapacityLimitVo dailyCapacityLimitVo,
                                      FactoryMonthPlanFinalAdjustVo mpFinalVo){
        //型腔台数
        int patternCount = mpFinalVo.getMouldCavityQty() /2;
        //主花纹向下所有SKU的模具数量 <= 主花纹.型腔数量
        return dailyCapacityLimitVo.getPatternUsedLhMachines() <= patternCount;
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
                break;
            }
            matchDayField = FactoryConstant.MATCH_DAY_FIELD+i;
            if (mpFinalVo.getFieldValueByFieldName(matchDayField) != null){
                //若搭配天的值不为空，直接退
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
                break;
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
    private void structureInAdjustWithIncrement(MpRollAdjustContextDTO contextDTO,
                                                List<MpAdjustStructureIn> incrementAdjustList,
                                                List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if(PubUtil.isEmpty(incrementAdjustList)){
            return;
        }
        int lockNextDay = contextDTO.getLockEndDay() + 1;
        Integer newOnLineDay,newPlanQty,newEndDay;
        FactoryMonthPlanFinalAdjustVo mpFinalVo;
        //2、排实单
        int iOrder = 0;
        for (MpAdjustStructureIn adjustStructInVo:incrementAdjustList){
            mpFinalVo = createMpFinalAdjustVo(contextDTO, adjustStructInVo);
            iOrder += 1;
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,排序:%s,物料编码:%s,开始日:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),mpFinalVo.getBeginDay())).append(ApsConstant.DIVISION);
            //2.1、敲定在机SKU新的上机日期
            newOnLineDay = getNewOnLineDay(contextDTO, lockNextDay, null);
            if (newOnLineDay == null){
                contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,排序:%s,物料编码:%s,没有获取到新的上机日期,退出！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                continue;
            }
            //2.2、计算新需要排产的计划量 = 实单量，其中，实单量：待调整量
            newPlanQty = adjustStructInVo.getConfirmAdjustQty();
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,排序:%s,物料编码:%s,新的上机日期:%s,新的排产量:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty)).append(ApsConstant.DIVISION);
            //2.4、增模排产,挤占空产能
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】--增模排产,排序:%s,物料编码:%s,开始！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            int remainPlanQty = incMouldProduction(mpProdFinalList, contextDTO, newOnLineDay, newPlanQty, mpFinalVo);
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】--增模排产,排序:%s,物料编码:%s,结束！还有剩余排产计划量:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),remainPlanQty)).append(ApsConstant.DIVISION);
            //2.5、若还有剩余，向前挤占其他SKU的搭配量
            if (remainPlanQty > 0){
                // 若剩余量 > 0，说明实单还有剩余
                // 日期向前，依次扣减其他SKU的搭配量，并模拟挤占
                newEndDay = newOnLineDay == lockNextDay ? lockNextDay:newOnLineDay-1;
                contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,物料编码:%s,扣减其他SKU的搭配-开始！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                deductMatchOtherSku(contextDTO,lockNextDay,newEndDay,remainPlanQty,mpFinalVo,mpProdFinalList);
                contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,物料编码:%s,扣减其他SKU的搭配-结束！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            }

            //2.6、将新增的SKU纳入定稿列表
            mpProdFinalList.add(mpFinalVo);
        }
    }

    /**
     * 结构调整：新增SKU
     * @param incrementAdjustList 新增SKU调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void structureOutAdjustWithIncrement(MpRollAdjustContextDTO contextDTO,
                                                List<MpAdjustStructureOut> incrementAdjustList,
                                                List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if(PubUtil.isEmpty(incrementAdjustList)){
            return;
        }
        int lockNextDay = contextDTO.getLockEndDay() + 1;
        Integer newOnLineDay,newPlanQty,newEndDay;
        FactoryMonthPlanFinalAdjustVo mpFinalVo;
        //2、排实单
        int iOrder = 0;
        for (MpAdjustStructureOut adjustStructInVo:incrementAdjustList){
            mpFinalVo = createMpFinalAdjustVo(contextDTO, adjustStructInVo);
            iOrder += 1;
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,排序:%s,物料编码:%s,开始日:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),mpFinalVo.getBeginDay())).append(ApsConstant.DIVISION);
            //2.1、敲定在机SKU新的上机日期
            newOnLineDay = getNewOnLineDayForStructOut(contextDTO, lockNextDay, null);
            if (newOnLineDay == null){
                contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,排序:%s,物料编码:%s,没有获取到新的上机日期,退出！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                continue;
            }
            //2.2、计算新需要排产的计划量 = 实单量，其中，实单量：待调整量
            newPlanQty = adjustStructInVo.getConfirmAdjustQty();
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,排序:%s,物料编码:%s,新的上机日期:%s,新的排产量:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),newOnLineDay,newPlanQty)).append(ApsConstant.DIVISION);
            //2.4、增模排产,挤占空产能
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】--增模排产,排序:%s,物料编码:%s,开始！",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            int remainPlanQty = incMouldProduction(mpProdFinalList, contextDTO, newOnLineDay, newPlanQty, mpFinalVo);
            contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】--增模排产,排序:%s,物料编码:%s,结束！还有剩余排产计划量:%s",contextDTO.getStructureName(), iOrder,mpFinalVo.getMaterialCode(),remainPlanQty)).append(ApsConstant.DIVISION);
            //2.5、若还有剩余，向前挤占其他SKU的搭配量
            if (remainPlanQty > 0){
                // 若剩余量 > 0，说明实单还有剩余
                // 日期向前，依次扣减其他SKU的搭配量，并模拟挤占
                newEndDay = newOnLineDay == lockNextDay ? lockNextDay:newOnLineDay-1;
                contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,物料编码:%s,扣减其他SKU的搭配-开始！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
                deductMatchOtherSku(contextDTO,lockNextDay,newEndDay,remainPlanQty,mpFinalVo,mpProdFinalList);
                contextDTO.getLogDetail().append(String.format("结构:%s,【新增SKU】,物料编码:%s,扣减其他SKU的搭配-结束！",contextDTO.getStructureName(), mpFinalVo.getMaterialCode())).append(ApsConstant.DIVISION);
            }

            //2.6、将新增的SKU纳入定稿列表
            mpProdFinalList.add(mpFinalVo);
        }
    }

    /**
     * 创建定稿记录对象
     * @param contextDTO 周程滚动上下文
     * @param adjustStructInVo 结构内调整Vo
     * @return 定稿记录对象
     */
    private FactoryMonthPlanFinalAdjustVo createMpFinalAdjustVo(MpRollAdjustContextDTO contextDTO, MpAdjustStructureIn adjustStructInVo) {
        FactoryMonthPlanFinalAdjustVo mpFinalVo;
        mpFinalVo = new FactoryMonthPlanFinalAdjustVo();
        mpFinalVo.setFactoryCode(contextDTO.getFactoryCode());
        mpFinalVo.setYear(contextDTO.getMpYear());
        mpFinalVo.setMonth(contextDTO.getMpMonth());
        String yearAndMonth = String.format("%s%02d", contextDTO.getMpYear(), contextDTO.getMpMonth());
        mpFinalVo.setYearMonth(Integer.valueOf(yearAndMonth));
        mpFinalVo.setMonthPlanVersion(adjustStructInVo.getMonthPlanVersion());
        mpFinalVo.setProductionVersion(adjustStructInVo.getProductionVersion());
        mpFinalVo.setLastMonthPlanVersion(adjustStructInVo.getVersion());
        mpFinalVo.setStructureName(adjustStructInVo.getStructureName());
        mpFinalVo.setProductTypeCode(adjustStructInVo.getProductTypeCode());
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
        return mpFinalVo;
    }
    /**
     * 创建定稿记录对象 for 结构调整
     * @param contextDTO 周程滚动上下文
     * @param adjustStructOutVo 结构调整Vo
     * @return 定稿记录对象
     */
    private FactoryMonthPlanFinalAdjustVo createMpFinalAdjustVo(MpRollAdjustContextDTO contextDTO, MpAdjustStructureOut adjustStructOutVo) {
        FactoryMonthPlanFinalAdjustVo mpFinalVo;
        mpFinalVo = new FactoryMonthPlanFinalAdjustVo();
        mpFinalVo.setFactoryCode(contextDTO.getFactoryCode());
        mpFinalVo.setYear(contextDTO.getMpYear());
        mpFinalVo.setMonth(contextDTO.getMpMonth());
        String yearAndMonth = String.format("%s%02d", contextDTO.getMpYear(), contextDTO.getMpMonth());
        mpFinalVo.setYearMonth(Integer.valueOf(yearAndMonth));
        mpFinalVo.setMonthPlanVersion(adjustStructOutVo.getMonthPlanVersion());
        mpFinalVo.setProductionVersion(adjustStructOutVo.getProductionVersion());
        mpFinalVo.setLastMonthPlanVersion(adjustStructOutVo.getVersion());
        mpFinalVo.setStructureName(adjustStructOutVo.getStructureName());
        mpFinalVo.setProductTypeCode(adjustStructOutVo.getProductTypeCode());
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
        return mpFinalVo;
    }
}
