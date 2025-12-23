package com.zlt.aps.monthplan.adjust.engine;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import springfox.documentation.schema.Entry;

import java.util.ArrayList;
import java.util.Comparator;
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
        onIncrementAdjustList = onIncrementAdjustList.stream().sorted(Comparator.comparing(MpAdjustStructureIn::getAdjustPriority,Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        structureInAdjustWithOnIncrement(onIncrementAdjustList,mpProdFinalList);
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
     * @param onIncrementAdjustList 在机SKU增量调整列表
     * @param mpProdFinalList 月计划定稿表列表
     * @throws BusinessException
     */
    private void structureInAdjustWithOnIncrement(List<MpAdjustStructureIn> onIncrementAdjustList,List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) throws BusinessException {
        if(PubUtil.isEmpty(onIncrementAdjustList)){
            return;
        }

        Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap = mpProdFinalList.stream().collect(Collectors.groupingBy(item->item.getMaterialCode(),
                 Collectors.collectingAndThen(Collectors.toList(),m-> {
                     return m.get(0);
                 })));
        FactoryMonthPlanFinalAdjustVo mpFinalVo;
        //1、按结构内调整记录依次匹配月计划定稿表
        for (MpAdjustStructureIn onIncrementAdjust:onIncrementAdjustList) {
            mpFinalVo = mpProdFinalMap.get(onIncrementAdjust.getMaterialCode());
            if (mpFinalVo == null) {
                continue;
            }
            

        }
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
