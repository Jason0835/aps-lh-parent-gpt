package com.zlt.aps.monthplan.adjust.engine;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.capacity.MpAdjustDailyCapacityLimit;
import com.zlt.aps.factory.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.factory.domain.dto.ProductionSkuParamHelper;
import com.zlt.aps.monthplan.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
     * 结构内自动调整
     * @param contextDTO
     * @throws BusinessException
     */
    public void structureInAutoAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        //注：结构内自动调整列表：关单直接排除，同时取订单列表与月计划最大并集；
        //1.结构内订单调整记录空检查
        if (PubUtil.isEmpty(contextDTO.getMpAdjustStructureInList())){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.orderAdjustRecordNotFound"),
                    contextDTO.getMpYear(),contextDTO.getMpMonth()));
        }
        //2.月计划定稿数据空检查
        if (PubUtil.isEmpty(contextDTO.getFactoryMonthPlanProdFinalList())){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecordNotFound"),
                    contextDTO.getMpYear(),contextDTO.getMpMonth()));
        }

        //3.按结构序列化分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> mpProdFinalMap = contextDTO.getFactoryMonthPlanProdFinalList().stream().collect(Collectors.groupingBy(item->item.getStructureName()));
        Map<String, List<MpAdjustStructureIn>> adjustStructInMap = contextDTO.getMpAdjustStructureInList().stream().collect(Collectors.groupingBy(item->item.getStructureName()));
        for (Map.Entry<String, List<MpAdjustStructureIn>> entry : adjustStructInMap.entrySet()) {
            //结构内，按结构分别调整
            structureInAdjustForOne(contextDTO,entry.getValue(),mpProdFinalMap.get(entry.getKey()));
        }
    }

    /**
     * 结构内调整，按结构分别调整
     * @param contextDTO 周程滚动调整上下文
     * @param mpAdjustStructureInList 结构内调整记录列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void structureInAdjustForOne(MpRollAdjustContextDTO contextDTO,List<MpAdjustStructureIn> mpAdjustStructureInList,List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        //1.解析出关单/减量、在产SKU、新增SKU以及暂缓
        List<MpAdjustStructureIn> deductAdjustList = new ArrayList<>();
        List<MpAdjustStructureIn> onIncrementAdjustList = new ArrayList<>();
        List<MpAdjustStructureIn> incrementAdjustList = new ArrayList<>();
        List<String> onMaterialCodeList = mpProdFinalList.stream().map(x->x.getMaterialCode()).collect(Collectors.toList());
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
        }
        //2.减量调整
        structureInAdjustWithDeduct(contextDTO,deductAdjustList,mpProdFinalList);
        //3.在机SKU增量
        //onIncrementAdjustList = onIncrementAdjustList.stream().sorted(Comparator.comparing(MpAdjustStructureIn::getAdjustPriority,Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        structureInAdjustWithOnIncrement(contextDTO,onIncrementAdjustList,mpProdFinalList,null);
        //4.新增SKU
        incrementAdjustList = incrementAdjustList.stream().sorted(Comparator.comparing(MpAdjustStructureIn::getAdjustPriority,Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        structureInAdjustWithIncrement(incrementAdjustList,mpProdFinalList);
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
            //允许扣减量,高优先级+中优先级-锁定量
            int allowDeductQty = mpFinalVo.getHeightProductionQty() + mpFinalVo.getMidProductionQty() - mpFinalVo.getLockQty();
            if (allowDeductQty >= reAdjustQty){
                //若允许扣减量 >= 剩余调整量
                //需要扣减量 = 剩余调整量;
                needDeductQty = reAdjustQty;
                needDeductProductionQty(needDeductQty, mpFinalVo);
                //2.1 遍历31天日排产量，根据实际扣减量依次扣减
                deductScheduleQtyByDay(reAdjustQty,contextDTO.getLockEndDay(), mpFinalVo);
                //reAdjustQty = 0;
            }else{
                //若允许扣减量 < 剩余调整量,允许扣减量 可以全扣
                needDeductQty = allowDeductQty;
                //根据 需要扣减量，从高优先级->中优先级
                needDeductProductionQty(needDeductQty, mpFinalVo);
                //2.1 遍历31天日排产量，根据实际扣减量依次扣减
                deductScheduleQtyByDay(allowDeductQty,contextDTO.getLockEndDay(), mpFinalVo);
                //reAdjustQty -= allowDeductQty;
            }
        }
    }

    /**
     * 按需要扣减的量，分别扣减高优先级，再扣减中优先级
     * @param needDeductQty 需要扣减的量
     * @param prodFinal 定额记录
     */
    private void needDeductProductionQty(int needDeductQty, FactoryMonthPlanFinalAdjustVo prodFinal) {
        //根据 需要扣减量，从高优先级->中优先级
        if (prodFinal.getHeightProductionQty() >= needDeductQty) {
            prodFinal.setHeightProductionQty(prodFinal.getHeightProductionQty() - needDeductQty);
        } else {
            needDeductQty = needDeductQty - prodFinal.getHeightProductionQty();
            prodFinal.setHeightProductionQty(0);
            prodFinal.setMidProductionQty(prodFinal.getMidProductionQty() - needDeductQty);
        }
        prodFinal.setTotalQty(prodFinal.getTotalQty() - needDeductQty);
        //将调减量置到空产能
        prodFinal.setEmptyQty(needDeductQty);
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
     * @param realDeductQty
     * @param lockEndDay
     * @param prodFinal
     */
    private void deductScheduleQtyByDay(int realDeductQty, int lockEndDay, FactoryMonthPlanFinalAdjustVo prodFinal) {
        int dayQty;
        String dayField;
        //实单肯定在前，从后向前扣减
        for (int i = FactoryConstant.MONTH_MAX_DAY; i> lockEndDay; i--){
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
                                                  List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                                  List<MpStructureAllocation> mpStructAllocList) throws BusinessException {
        if(PubUtil.isEmpty(onIncrementAdjustList)){
            return;
        }

        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
        //1、初始日产能限制
        int startDay = contextDTO.getLockEndDay() + 1;
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = adjustDailyCapacityLimitObj.getDailyCapacityLimitMap(startDay,mpProdFinalList,mpStructAllocList);

        //2、排序：在机SKU上机日期早的优先增量排产
        mpProdFinalList.sort(Comparator.comparingInt(FactoryMonthPlanFinalAdjustVo::getBeginDate));
        Map<String, MpAdjustStructureIn> mpAdjustStructInMap = onIncrementAdjustList.stream().collect(Collectors.groupingBy(item->item.getMaterialCode(),
                 Collectors.collectingAndThen(Collectors.toList(),m-> {
                     return m.get(0);
                 })));
        MpAdjustStructureIn adjustStructInVo;
        Integer newOnLineDay,newPlanQty;
        //3、先排实单->自带的搭配
        for (FactoryMonthPlanFinalAdjustVo mpFinalVo:mpProdFinalList) {
            adjustStructInVo = mpAdjustStructInMap.get(mpFinalVo.getMaterialCode());
            if (adjustStructInVo == null) {
                continue;
            }
            //3.1、敲定在机SKU新的上机日期
            newOnLineDay = adjustDailyCapacityLimitObj.getNewOnLineDay(startDay,mpFinalVo.getBeginDay(),dailyCapacityLimitVoMap);
            //3.2、计算新需要排产的计划量 = 实单量+自带的搭配量，其中，实单量：新的净需求量 - （调整日~锁定日）的每日排产量
            newPlanQty = getNewPlanQty(adjustStructInVo,mpFinalVo);

            //3.3、清空定稿表日计划量
            clearMpFinalDayValue(startDay, mpFinalVo);

            //3.4、增模排产
            incMouldProduction(mpProdFinalList, adjustDailyCapacityLimitObj, dailyCapacityLimitVoMap, newOnLineDay, newPlanQty, mpFinalVo);
        }
    }

    private void incMouldProduction(List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                    MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj,
                                    Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap,
                                    Integer newOnLineDay, Integer newPlanQty, FactoryMonthPlanFinalAdjustVo mpFinalVo) {
        String dayField;
        int dayValue;
        int startMould = 2;
        while (newPlanQty <= 0){

            for (int i = newOnLineDay; i<= mpFinalVo.getEndDay(); i++){
                //SKU的模具数限制：SKU的模具数<=SKU活块的数量
                if (startMould > mpFinalVo.getTypeBlockQty()){
                    break;
                }
                dayField = FactoryConstant.DAY_FIELD + i;
                dayValue = mpFinalVo.getFieldValueByFieldName(dayField) == null ? 0 : (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
                dayValue += getDayVulcanizationQty(mpFinalVo);
                mpFinalVo.setFieldValueByFieldName(dayField,dayValue);
                adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList,i, dailyCapacityLimitVoMap.get(i), mpFinalVo.getMainPattern());
                //检查: 主花纹向下模具数量(/2转成机台数)\当前每日硫化机台数\当前每日胎胚种类数 符合性
                if (!checkCapacitySatisfy(dailyCapacityLimitVoMap.get(i), mpFinalVo.getMouldCavityQty()/2)){
                    // 将值还原，并退出
                    dayValue -= getDayVulcanizationQty(mpFinalVo);
                    mpFinalVo.setFieldValueByFieldName(dayField,dayValue);
                    newPlanQty = 0;
                    break;
                }
                newPlanQty -= dayValue;
                if (newPlanQty <=0){
                    break;
                }
            }
            startMould += 2;
        }
    }

    /**
     * 检查产能满足情况
     *
     * @param dailyCapacityLimitVo 产能限制Vo
     * @param patternCount 型腔台数
     * @return true-满足，false-不满足
     */
    public boolean checkCapacitySatisfy(MpDailyCapacityLimitVo dailyCapacityLimitVo, int patternCount){
        //当前每日硫化机台数 < 每日硫化机台总限制数 且 当前每日胎胚种类数 < 每日胎胚种类总限制数 且 主花纹向下所有SKU的模具数量 <= 主花纹.型腔数量
        return dailyCapacityLimitVo.getUsedEmbryoTypes() < dailyCapacityLimitVo.getMaxEmbryoTypes() &&
                dailyCapacityLimitVo.getUsedLhMachines() < dailyCapacityLimitVo.getMaxLhMachines() &&
                dailyCapacityLimitVo.getPatternUsedLhMachines() < patternCount;
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
     * @param startDay 锁定次日
     * @param prodFinal 定稿表计划Vo
     */
    private void clearMpFinalDayValue(int startDay,FactoryMonthPlanFinalAdjustVo prodFinal){
        if (prodFinal == null){
            return;
        }
        String dayField;
        for (int i = startDay; i<=FactoryConstant.MONTH_MAX_DAY; i++) {
            dayField = FactoryConstant.DAY_FIELD + i;
            prodFinal.setFieldValueByFieldName(dayField,null);
        }
    }

    /**
     * 获取的排产计划量（实单量+自带的搭配量）
     * @param adjustStructInVo 结构内调整Vo
     * @param mpFinalVo 定稿Vo
     * @return 新的排产计划量
     */
    private int getNewPlanQty(MpAdjustStructureIn adjustStructInVo,FactoryMonthPlanFinalAdjustVo mpFinalVo){
        return adjustStructInVo.getConfirmAdjustQty() + mpFinalVo.getConventionProductionQty();
    }

    /**
     * 结构内调整：新增SKU
     * @param incrementAdjustList 新增SKU调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void structureInAdjustWithIncrement(List<MpAdjustStructureIn> incrementAdjustList,List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {

    }
}
