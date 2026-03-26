package com.zlt.aps.mp.engine.capacity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.common.utils.PubUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日产能限制抽象类
 * @author Sandy
 * @date 2025/12/24
 */
public abstract class AbstractDailyCapacityLimit {

    /**
     * 施工阶段字段
     */
    private static String CONSTRUCTION_STAGE_FIELD = "constructionStage";

    /**
     * 初始化日产能
     * @param contextDTO 周程滚动上下文
     */
    public Map<Integer, MpDailyCapacityLimitVo> getDailyCapacityLimitMap(MpRollAdjustContextDTO contextDTO){
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = new HashMap<>();
        MpDailyCapacityLimitVo dailyCapacityLimitVo;
        Integer decLhMachines = (Integer) contextDTO.getParamMap().get(MonthPlanEnums.CHANGE_STRUCT_DEC_LH_MACHINES.getCode());
        for (int i = contextDTO.getStructureStartDay(); i<= FactoryConstant.MONTH_MAX_DAY; i++){
            dailyCapacityLimitVo = dailyCapacityLimitVoMap.get(i);
            if (dailyCapacityLimitVo == null){
                dailyCapacityLimitVo = new MpDailyCapacityLimitVo();
            }
            dailyCapacityLimitVo.setDailyDate(i);
            // 1、设置每日硫化机台总限制数、每日胎胚种类总限制数
            setMaxLhMachinesWithEmbryoTypes(contextDTO.getOneStructureAllocationList(),i,dailyCapacityLimitVo,decLhMachines);
            dailyCapacityLimitVoMap.put(i,dailyCapacityLimitVo);
        }
        return dailyCapacityLimitVoMap;
    }

    /**
     * 设置每日硫化机台总限制数、每日胎胚种类总限制数
     * @param mpStructAllocList 结构转产列表
     * @param iDay 第X天
     * @param dailyCapacityLimitVo 日产能限制Vo
     */
    public void setMaxLhMachinesWithEmbryoTypes(List<MpStructureAllocation> mpStructAllocList, int iDay,
                                                 MpDailyCapacityLimitVo dailyCapacityLimitVo,int decLhMachines) {
        if(PubUtil.isEmpty(mpStructAllocList)){
            return;
        }

        int iMaxLhMachines = 0;
        int iMaxEmbryoTypes = 0;
        for (MpStructureAllocation strutAllocVo: mpStructAllocList){
            if (iDay >= strutAllocVo.getBeginDay() && iDay <= strutAllocVo.getEndDay()){
                iMaxLhMachines += Convert.toInt(strutAllocVo.getMaxLhMachineCount(), 0);
                iMaxEmbryoTypes += Convert.toInt(strutAllocVo.getMaxEmbryoCodeCount(), 0);
            }
            if (iDay == strutAllocVo.getBeginDay()){
                //若当前日 = 结构转产开始日，减 硫化机台数 ,防止切换结构时，换模过多
                iMaxLhMachines -= (decLhMachines > strutAllocVo.getMaxLhMachineCount() ? strutAllocVo.getMaxLhMachineCount() : decLhMachines);
            }
        }
        dailyCapacityLimitVo.setMaxLhMachines(iMaxLhMachines);
        dailyCapacityLimitVo.setMaxEmbryoTypes(iMaxEmbryoTypes);
    }
    /**
     *  1、计算当前每日硫化机台数,根据日计划量反向判断
     * 	1）步骤1：按SKU维度，取整(日计划量/日单台硫化量)，并统计有余数的SKU个数
     * 	2）步骤2：按SKU维度，单独统计 日计划量<日单台硫化量的SKU个数，即换模或换活字块；
     * 	3）当前每日硫化机台数 = SUM(所有SKU的 日计划量/日单台硫化量 取整) + MAX(步骤1有余数的SKU个数，步骤2有换模或活字块的SKU个数)
     * 	2.计算当前每日胎胚种类数
     * 	SUM(DISTINCT(SKU所关联的胎胚))
     * @param mpProdFinalList 月计划定稿
     * @param iDay 第X天
     * @param dailyCapacityLimitVo 日产能限制Vo
     * @param mainPattern 主花纹
     */
  /*  public void calcLhMachinesWithEmbryoTypes(List<? extends BaseEntity> mpProdFinalList, int iDay,
                                               MpDailyCapacityLimitVo dailyCapacityLimitVo,String mainPattern) {
        if (PubUtil.isEmpty(mpProdFinalList) || dailyCapacityLimitVo == null){
            return;
        }
        // 按日期向下，统计日硫化机台数
        int intPart = 0;
        int remainderCount = 0;
        int changeMouldCount = 0;

        // 按日期+主花纹向下，统计日硫化机台数
        int patternIntPart = 0;
        int patternRemainderCount = 0;
        int patternChangeMouldCount = 0;

        int dayPlanQty,dailyLhQty;
        String dayField = FactoryConstant.DAY_FIELD + iDay;
        // 次日字段
        String day2Field = FactoryConstant.DAY_FIELD + (iDay +1 > FactoryConstant.MONTH_MAX_DAY ? FactoryConstant.MONTH_MAX_DAY:iDay +1);
        String embryoFieldValue;
        dailyCapacityLimitVo.getEmbryoCodes().clear();
        for (BaseEntity mpFinalVo: mpProdFinalList){
            if (mpFinalVo.getFieldValueByFieldName(dayField) == null) {
                continue;
            }
            if (mpFinalVo.getFieldValueByFieldName(CONSTRUCTION_STAGE_FIELD) != null &&
                    ConstructionStageEnum.MEASUREMENT.getStage().equals(mpFinalVo.getFieldValueByFieldName(CONSTRUCTION_STAGE_FIELD))){
                //试制 不纳入统计
                continue;
            }
            // 日计划量
            dayPlanQty = (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
            // 日硫化量 = 单模硫化量 * 2；
            dailyLhQty = getDayVulcanizationQty(mpFinalVo);

            if (mpFinalVo.getFieldValueByFieldName(day2Field) != null &&
                    (Integer)mpFinalVo.getFieldValueByFieldName(day2Field) >= dayPlanQty) {
                // 若次日计划量 比 当日计划量 大，说明在增模
                // 日计划量 / 日单台硫化量 向上取整
                //intPart += Math.ceil((double) dayPlanQty / dailyLhQty);
                intPart += dayPlanQty / dailyLhQty;
                // 计算主花纹向下的硫化机台数
                if (mpFinalVo.getFieldValueByFieldName(getMainPatternField()).equals(mainPattern)){
                    //patternIntPart += Math.ceil((double) dayPlanQty / dailyLhQty);
                    patternIntPart += dayPlanQty / dailyLhQty;
                }

            }else {
                // 取整(日计划量/日单台硫化量)
                intPart += dayPlanQty / dailyLhQty;
                // 统计有余数的SKU个数
                remainderCount += dayPlanQty % dailyLhQty > 0 ? 1:0;

                // 计算主花纹向下的硫化机台数
                if (mpFinalVo.getFieldValueByFieldName(getMainPatternField()).equals(mainPattern)){
                    patternIntPart += dayPlanQty / dailyLhQty;
                    patternRemainderCount += dayPlanQty % dailyLhQty > 0 ? 1:0;
                }

            }
            // 统计换模的SKU个数
            changeMouldCount += dayPlanQty > 0 && dayPlanQty < dailyLhQty ? 1:0;
            // 计算主花纹向下的硫化机台数
            if (mpFinalVo.getFieldValueByFieldName(getMainPatternField()).equals(mainPattern)){
                patternChangeMouldCount += dayPlanQty > 0 && dayPlanQty < dailyLhQty ? 1:0;
            }

            // 统计胎胚种类数
            if (dayPlanQty > 0){
                embryoFieldValue = (String) mpFinalVo.getFieldValueByFieldName(getEmbryoCodeField());
                dailyCapacityLimitVo.getEmbryoCodes().add(embryoFieldValue);
            }
        }
        int iCount = Math.max(remainderCount,changeMouldCount);

        // 已用硫化机台数
        dailyCapacityLimitVo.setUsedLhMachines(intPart + iCount);
        // 已用胎胚种类数
        dailyCapacityLimitVo.setUsedEmbryoTypes(dailyCapacityLimitVo.getEmbryoCodes().size());

        // 计算主花纹向下的硫化机台数
        int iPatternCount = Math.max(patternRemainderCount,patternChangeMouldCount);
        dailyCapacityLimitVo.setPatternUsedLhMachines(patternIntPart + iPatternCount);
    }*/

    /**
     *  1、计算当前每日硫化机台数,根据日计划量反向判断
     * 	1)步骤1：减模-满产硫化机台数
     * 	2)步骤2：增模-满产硫化机台数
     * 	3)步骤3： 组合
     * 	4）减模-满产硫化机台数 + 增模-满产硫化机台数 + 组合
     * 	2.计算当前每日胎胚种类数, SUM(DISTINCT(SKU所关联的胎胚))
     * @param mpProdFinalList 月计划定稿
     * @param iDay 第X天
     * @param dailyCapacityLimitVo 日产能限制Vo
     * @param paramMap 参数Map
     * @param mainPattern 主花纹
     */
    public void calcLhMachinesWithEmbryoTypes(List<? extends BaseEntity> mpProdFinalList, int iDay,
                                                MpDailyCapacityLimitVo dailyCapacityLimitVo,Map<String,Object> paramMap,String mainPattern) {
        calcLhMachinesWithEmbryoTypes2(mpProdFinalList,iDay,dailyCapacityLimitVo,paramMap,mainPattern,false);
    }
    /**
     *  1、计算当前每日硫化机台数,根据日计划量反向判断
     * 	1)步骤1：减模-满产硫化机台数
     * 	2)步骤2：增模-满产硫化机台数
     * 	3)步骤3： 组合
     * 	4）减模-满产硫化机台数 + 增模-满产硫化机台数 + 组合
     * 	2.计算当前每日胎胚种类数, SUM(DISTINCT(SKU所关联的胎胚))
     * @param mpProdFinalList 月计划定稿
     * @param iDay 第X天
     * @param dailyCapacityLimitVo 日产能限制Vo
     * @param paramMap 参数Map
     * @param mainPattern 主花纹
     */
    private Integer calcLhMachinesWithEmbryoTypes2(List<? extends BaseEntity> mpProdFinalList, int iDay,
                                              MpDailyCapacityLimitVo dailyCapacityLimitVo,Map<String,Object> paramMap,String mainPattern,boolean returnFirstQty) {
        if (PubUtil.isEmpty(mpProdFinalList) || dailyCapacityLimitVo == null){
            return null;
        }
        // 按日期向下，统计日硫化机台数
        int fullMachinesDecMould = 0;
        int closeMachinesDecMould = 0;
        int closeNoAddMachinesDecMould = 0;
        int closeNoChangeMachinesDecMould = 0;
        int fullMachinesAddMould = 0;
        int openMachinesAddMould = 0;
        int blockMachinesAddMould = 0;

        int iChangeMouldCount = 0;

        // 按日期+主花纹向下，统计日硫化机台数
        int mpFullMachinesDecMould = 0;
        int mpCloseMachinesDecMould = 0;

        int mpFullMachinesAddMould = 0;
        int mpOpenMachinesAddMould = 0;
        int mpBlockMachinesAddMould = 0;

        //Map<主花纹,减模机台数>
        Map<String,Integer> patternDecMouldMap = new HashMap<>();
        //Map<主花纹,增模机台数>
        Map<String,Integer> patternAddMouldMap = new HashMap<>();
        //Map<主花纹,余量大于日计划量/2的减模机台数>
        Map<String,Integer> patternNoAddDecMouldMap = new HashMap<>();
        //Map<主花纹,前SKU收尾量与日硫化量差异<40条 的减模机台数>
        Map<String,Integer> patternNoChangeDecMouldMap = new HashMap<>();
        //Map<主花纹,余量与日硫化量差异数<=8 的减模机台数>
        //Map<String,Integer> patternDiffDailyQtyDecMouldMap = new HashMap<>();
        //Map<主花纹,换20条活字块的增模机台数>
        //Map<String,Integer> patternTwentyBlockAddMouldMap = new HashMap<>();
        Integer changeMouldFirstQty = (Integer)paramMap.get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode());
        int dayPlanQty,dailyLhQty;
        // 当日字段-dayField，昨日字段-day1Field，次日字段-day2Field
        String dayField = FactoryConstant.DAY_FIELD + iDay;
        String day1Field = FactoryConstant.DAY_FIELD + (iDay -1 < FactoryConstant.MONTH_START_DAY ? FactoryConstant.MONTH_START_DAY:iDay -1);
        String day2Field = FactoryConstant.DAY_FIELD + (iDay +1 > FactoryConstant.MONTH_MAX_DAY ? FactoryConstant.MONTH_MAX_DAY:iDay +1);
        String embryoFieldValue;
        dailyCapacityLimitVo.getEmbryoCodes().clear();
        for (BaseEntity mpFinalVo: mpProdFinalList){
            // 日硫化量 = 单模硫化量 * 2；
            dailyLhQty = getDayVulcanizationQty(mpFinalVo);
            if (mpFinalVo.getFieldValueByFieldName(dayField) == null ||
                    (Integer) mpFinalVo.getFieldValueByFieldName(dayField) == 0) {
                //若前日有收尾，主花纹机台数也要纳入统计
                Integer preDayValue = (Integer)mpFinalVo.getFieldValueByFieldName(day1Field);
                if (preDayValue != null && preDayValue > 0) {
                    int preMachines = (int)Math.ceil((double) preDayValue / dailyLhQty);
                    patternMachinesCountMap(patternDecMouldMap,mpFinalVo,preMachines);
                }
                continue;
            }
            if (mpFinalVo.getFieldValueByFieldName(CONSTRUCTION_STAGE_FIELD) != null &&
                    ConstructionStageEnum.MEASUREMENT.getStage().equals(mpFinalVo.getFieldValueByFieldName(CONSTRUCTION_STAGE_FIELD))){
                //试制 不纳入统计
                continue;
            }
            // 日计划量
            dayPlanQty = (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
            if (dailyCapacityLimitVo.isOpenProductionFirstDay()){
                //若开产首日，将日硫化量等比例减，奇数+1
                dailyLhQty = getProportionalDeductQty(dailyCapacityLimitVo,dailyLhQty);
            }
            int tmpCount;
            if (isDecMould(mpFinalVo,dayField,day1Field)){
                //减模处理：
                // 取整(日计划量/日单台硫化量)
                fullMachinesDecMould += dayPlanQty / dailyLhQty;
                // 统计有余数的SKU个数
                tmpCount = dayPlanQty % dailyLhQty > 0 ? 1:0;
                closeMachinesDecMould += tmpCount;
                // 计算主花纹向下的硫化机台数
                if (mpFinalVo.getFieldValueByFieldName(getMainPatternField()) !=null
                        && mpFinalVo.getFieldValueByFieldName(getMainPatternField()).equals(mainPattern)){
                    mpFullMachinesDecMould += dayPlanQty / dailyLhQty;
                    mpCloseMachinesDecMould += tmpCount;
                }

                //Map<主花纹，收尾台数>（用于判断是否换活字块）
                countPatternCloseMachines(patternDecMouldMap,dailyLhQty, mpFinalVo,dayField,day1Field);
                //Map<主花纹,余量大于日计划量/2 的减模机台数>
                closeNoAddMachinesDecMould += countPatternCloseNoAddMachines(patternNoAddDecMouldMap,dailyLhQty, mpFinalVo,paramMap,dayField);
                //Map<主花纹,前SKU收尾量与日硫化量差异<40条 的减模机台数>
                closeNoChangeMachinesDecMould += countPatternCloseNoChangeMachines(patternNoChangeDecMouldMap,dailyLhQty, mpFinalVo,paramMap,dayField,dailyCapacityLimitVo);
                //Map<主花纹,余量与日硫化量差异数<=8 的减模机台数>
                //countPatternCloseDiffDailyQtyMachines(patternDiffDailyQtyDecMouldMap,dailyLhQty,mpFinalVo,paramMap,dayField);
            }else{
                //增模处理：
                // 取整(日计划量/日单台硫化量)
                fullMachinesAddMould += dayPlanQty / dailyLhQty;
                // 统计有余数的SKU个数
                int[]addMouldArr = getAddMouldMachines(mpFinalVo,dailyLhQty,paramMap,dayField,day2Field);
                openMachinesAddMould += addMouldArr[0];
                blockMachinesAddMould += addMouldArr[1] + addMouldArr[2];
                // 统计换模次数(区别于addMouldArr[0]，主要是将收尾的排除)
                iChangeMouldCount += addMouldArr[3];
                // 计算主花纹向下的硫化机台数
                if (mpFinalVo.getFieldValueByFieldName(getMainPatternField()) != null &&
                        mpFinalVo.getFieldValueByFieldName(getMainPatternField()).equals(mainPattern)){
                    mpFullMachinesAddMould += dayPlanQty / dailyLhQty;
                    mpOpenMachinesAddMould += addMouldArr[0];
                    mpBlockMachinesAddMould += addMouldArr[1] + addMouldArr[2];
                }

                //Map<主花纹，增模台数>
                patternMachinesCountMap(patternAddMouldMap,mpFinalVo, addMouldArr[0]+addMouldArr[1]+addMouldArr[2]);
                //Map<主花纹，换20条活字块的增模机台数>
                //patternMachinesCountMap(patternTwentyBlockAddMouldMap, mpFinalVo, addMouldArr[1]);
            }

            // 统计胎胚种类数
            if (dayPlanQty >= changeMouldFirstQty){
                embryoFieldValue = (String) mpFinalVo.getFieldValueByFieldName(getEmbryoCodeField());
                dailyCapacityLimitVo.getEmbryoCodes().add(embryoFieldValue);
            }
        }

        // 计算机台组合（减模、增模、换活字块）
        MachineCombinationCalculator machinesCalculator = new MachineCombinationCalculator(closeMachinesDecMould, closeNoAddMachinesDecMould, closeNoChangeMachinesDecMould,openMachinesAddMould, blockMachinesAddMould);
        MachineResultVo machineResultVo = machinesCalculator.calculate();
        // 总硫化机台数 = 减模满机台数 + 增模满机台数 + 组合机台数
        int iCount = fullMachinesDecMould + fullMachinesAddMould + machineResultVo.getTotalMachines();

        // 已用硫化机台数
        dailyCapacityLimitVo.setUsedLhMachines(iCount);
        // 已用胎胚种类数
        dailyCapacityLimitVo.setUsedEmbryoTypes(dailyCapacityLimitVo.getEmbryoCodes().size());
        // 已用换模次数
        dailyCapacityLimitVo.setUsedChangeMould(iChangeMouldCount);
        //==================计算主花纹向下的硫化机台数==========================
        // 计算机台组合（减模、增模、换活字块）
        int mpPatternNoAddDecMould = patternNoAddDecMouldMap.get(mainPattern) == null ? 0:patternNoAddDecMouldMap.get(mainPattern);
        int mpPatternNoChangeDecMould = patternNoChangeDecMouldMap.get(mainPattern) == null ? 0:patternNoChangeDecMouldMap.get(mainPattern);
        machinesCalculator = new MachineCombinationCalculator(mpCloseMachinesDecMould, mpPatternNoAddDecMould,
                mpPatternNoChangeDecMould, mpOpenMachinesAddMould, mpBlockMachinesAddMould);
        machineResultVo = machinesCalculator.calculate();
        // 总硫化机台数 = 减模满机台数 + 增模满机台数 + 组合机台数
        iCount = mpFullMachinesDecMould + mpFullMachinesAddMould + machineResultVo.getTotalMachines();
        dailyCapacityLimitVo.setPatternUsedLhMachines(iCount);
        //==================计算主花纹向下的硫化机台数==========================

        if (returnFirstQty){
            int patternDecMouldCount = patternDecMouldMap.get(mainPattern) == null ? 0:patternDecMouldMap.get(mainPattern);
            int patternAddMouldCount = patternAddMouldMap.get(mainPattern) == null ? 0:patternAddMouldMap.get(mainPattern);
            int patternNoChangeDecMouldCount = patternNoChangeDecMouldMap.get(mainPattern) == null ? 0:patternNoChangeDecMouldMap.get(mainPattern);
            if (patternNoChangeDecMouldCount >0){
                //若主花纹下，不让换花纹，直接退
                return null;
            }
            //int patternDiffDailyQtyDecMouldCount = patternDiffDailyQtyDecMouldMap.get(mainPattern) == null ? 0:patternDiffDailyQtyDecMouldMap.get(mainPattern);
            //int patternTwentyBlockAddMouldCount = patternTwentyBlockAddMouldMap.get(mainPattern) == null ? 0:patternTwentyBlockAddMouldMap.get(mainPattern);
            return getFirstDayQty(patternDecMouldCount - patternNoChangeDecMouldCount,patternAddMouldCount,paramMap);
        }

        return null;
    }

    /**
     * 获取等比例减量，若是奇数，+1
     * @param dailyCapacityLimitVo 日产能限制Vo
     * @param dayVulcanizationQty 计划量
     * @return 获取等比例减量
     */
    public int getProportionalDeductQty(MpDailyCapacityLimitVo dailyCapacityLimitVo,int dayVulcanizationQty){
        //新计划量 = 计划量 * 比例/100
        int newDayQty = dayVulcanizationQty * dailyCapacityLimitVo.getDayProductionRate()/100;
        //若新计划是奇数，则+1
        return (newDayQty % 2 != 0) ? (newDayQty + 1):newDayQty;
    }

    /**
     *  获取首日计划量
     * @param mpProdFinalList 月计划定稿
     * @param iDay 第X天
     * @param dailyCapacityLimitVo 日产能限制Vo
     * @param paramMap 参数Map
     * @param mainPattern 主花纹
     */
    public Integer getFirstDayQty(List<? extends BaseEntity> mpProdFinalList, int iDay,
                                               MpDailyCapacityLimitVo dailyCapacityLimitVo,Map<String,Object> paramMap,String mainPattern) {
        return calcLhMachinesWithEmbryoTypes2(mpProdFinalList,iDay,dailyCapacityLimitVo,paramMap,mainPattern,true);
    }

    private Integer getFirstDayQty(int patternDecMouldCount,int patternAddMouldCount,Map<String,Object> paramMap){
        if (patternDecMouldCount > patternAddMouldCount){
            //若主花纹向下的减模机台数 > 增模机台数，则表示本次新增为换活字块
            /*if (patternDiffDailyQtyDecMouldCount > patternTwentyBlockAddMouldCount){
                //减模20条换活块的机台数 > 20条换活块已占的机台数
                return (Integer)paramMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode());
            }else{
                return (Integer)paramMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode());
            }*/
            return (Integer)paramMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode());
        }else{
            return (Integer)paramMap.get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode());
        }
    }
    /**
     * 主花纹收尾机台数统计
     * @param patternMachinesMap
     * @param mpFinalVo
     */
    private void countPatternCloseMachines(Map<String, Integer> patternMachinesMap, Integer dailyLhQty,BaseEntity mpFinalVo,String dayField,String day1Field) {
        //Map<主花纹，收尾台数>（用于判断是否换活字块）：有余数(日计划量/日单台硫化量)，记1台
        // 日计划量
        Integer dayPlanQty = (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
        // 日硫化量 = 单模硫化量 * 2；
        //Integer dailyLhQty = getDayVulcanizationQty(mpFinalVo);
        int iCount = (Integer)mpFinalVo.getFieldValueByFieldName(dayField) % dailyLhQty > 0 ? 1:0;
        int preMachines,curMachines;
        if (dayPlanQty < dailyLhQty){
            //若今日的计划量<单日硫化量，则今日的主花纹收尾台数 = 取整(昨日计划量/日单台硫化量)
            if (mpFinalVo.getFieldValueByFieldName(day1Field) != null){
                iCount = (Integer)mpFinalVo.getFieldValueByFieldName(day1Field) / dailyLhQty;
            }
        }else if (dayPlanQty.equals(dailyLhQty)){
            //若今日的计划量=单日硫化量 且 取整(昨日计划量/日单台硫化量) > 取整(今日计划量/日单台硫化量)
            //则今日的主花纹收尾台数 = 取整(昨日计划量/日单台硫化量) - 取整(今日计划量/日单台硫化量)
            if (mpFinalVo.getFieldValueByFieldName(day1Field) != null){
                preMachines = (Integer)mpFinalVo.getFieldValueByFieldName(day1Field) / dailyLhQty;
                curMachines = (Integer)mpFinalVo.getFieldValueByFieldName(dayField) / dailyLhQty;
                if (preMachines > curMachines){
                    iCount =  preMachines - curMachines;
                }
            }
        }

        //主花纹机台数统计
        patternMachinesCountMap(patternMachinesMap,mpFinalVo,iCount);
    }

    /**
     * 主花纹收尾\但不能增模的机台数统计
     * @param patternMachinesMap
     * @param mpFinalVo
     */
    private int countPatternCloseNoAddMachines(Map<String, Integer> patternMachinesMap, Integer dailyLhQty,BaseEntity mpFinalVo, Map<String,Object> paramMap,String dayField) {
        // 日硫化量 = 单模硫化量 * 2；
        //Integer dailyLhQty = getDayVulcanizationQty(mpFinalVo);
        int remainQty = (Integer)mpFinalVo.getFieldValueByFieldName(dayField) % dailyLhQty;
        int iCount = 0;
        if (remainQty > dailyLhQty/2){
            //当前SKU的余量大于日硫化量/2的台数（有收尾但当日不能换模）
            iCount += 1;
            patternMachinesCountMap(patternMachinesMap,mpFinalVo,iCount);
        }
        return iCount;
    }

    /**
     * 主花纹收尾\但不能换活块的机台数统计
     * @param patternMachinesMap
     * @param mpFinalVo
     */
    private int countPatternCloseNoChangeMachines(Map<String, Integer> patternMachinesMap, Integer dailyLhQty,BaseEntity mpFinalVo, Map<String,Object> paramMap,String dayField,MpDailyCapacityLimitVo dailyCapacityLimitVo) {
        // 日硫化量 = 单模硫化量 * 2；
        //Integer dailyLhQty = getDayVulcanizationQty(mpFinalVo);
        int remainQty = (Integer)mpFinalVo.getFieldValueByFieldName(dayField) % dailyLhQty;
        int iCount = 0;
        int changeTypeBlockDiffQty = (Integer) paramMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode());
        if (dailyCapacityLimitVo.isOpenProductionFirstDay()){
            //若开产首日，将日硫化量等比例减，奇数+1
            changeTypeBlockDiffQty = getProportionalDeductQty(dailyCapacityLimitVo,changeTypeBlockDiffQty);
        }
        //前SKU的收尾量与日硫化量差异<=40条(32+8)（有收尾但当日不能换活字块）
        if (dailyLhQty - remainQty <= changeTypeBlockDiffQty){
            iCount += 1;
            patternMachinesCountMap(patternMachinesMap,mpFinalVo,iCount);
        }
        return iCount;
    }

    /**
     * 主花纹收尾量与日硫化量差异<=8的机台数统计
     * @param patternMachinesMap
     * @param mpFinalVo
     */
    private void countPatternCloseDiffDailyQtyMachines(Map<String, Integer> patternMachinesMap, Integer dailyLhQty,BaseEntity mpFinalVo, Map<String,Object> paramMap,String dayField) {
        // 日硫化量 = 单模硫化量 * 2；
        //Integer dailyLhQty = getDayVulcanizationQty(mpFinalVo);
        int remainQty = (Integer)mpFinalVo.getFieldValueByFieldName(dayField) % dailyLhQty;
        int changeTypeBlockMaxQty = (Integer) paramMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode());
        //前SKU的收尾量与日硫化量差异<=8条
        if (dailyLhQty - remainQty <= changeTypeBlockMaxQty){
            patternMachinesCountMap(patternMachinesMap,mpFinalVo,1);
        }
    }

    /**
     * 主花纹机台数统计
     * @param patternMachinesMap
     * @param mpFinalVo
     */
    private void patternMachinesCountMap(Map<String, Integer> patternMachinesMap, BaseEntity mpFinalVo,int iCount) {

        Integer oriCount = patternMachinesMap.get(mpFinalVo.getFieldValueByFieldName(getMainPatternField()));
        if ( oriCount == null){
            patternMachinesMap.put((String) mpFinalVo.getFieldValueByFieldName(getMainPatternField()),iCount);
        }else{
            patternMachinesMap.put((String) mpFinalVo.getFieldValueByFieldName(getMainPatternField()),oriCount + iCount);
        }
    }

    /**
     * 增模台数
     * [0]--新增模机台数
     * [1]--换活字块机台数20条
     * [2]--换活字块机台数X条(32)
     * [3]--换模次数
     * @param mpFinalVo
     */
    public int[] getAddMouldMachines(BaseEntity mpFinalVo,Integer dailyLhQty,Map<String,Object> paramMap,String dayField,String day2Field) {
        //增模台数：有余数(日计划量/日单台硫化量)，记1台
        // 日计划量
        Integer dayPlanQty = (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
        // 日硫化量 = 单模硫化量 * 2；
        //Integer dailyLhQty = getDayVulcanizationQty(mpFinalVo);
        //换模起排量
        int changeMouldFirstQty = (Integer) paramMap.get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode());
        //换活字块20条
        int changeMouldBlockQty = (Integer) paramMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode());
        //换活字块X32条
        int changeMouldXBlockQty = (Integer) paramMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode());
        //余量
        int remainQty = (Integer)mpFinalVo.getFieldValueByFieldName(dayField) % dailyLhQty;
        int[] resultArr = {0,0,0,0};
        if (remainQty == 0){
            //没有余量，直接退回
            return resultArr;
        }
        if (remainQty == changeMouldFirstQty){
            //若余数 == 换模起排量，则视新增机台数
            resultArr[0] = 1;
            resultArr[3] = 1;
        }else if (remainQty == changeMouldBlockQty){
            //若余数 == 换活字块20条
            resultArr[1] = 1;
            resultArr[3] = 1;
        }else if (remainQty == changeMouldXBlockQty){
            resultArr[2] = 1;
            resultArr[3] = 1;
        }else {
            resultArr[0] = 1;
        }
        if (mpFinalVo.getFieldValueByFieldName(day2Field) != null){
            //例子：
            //46 46
            //8  46
            //8  46
            int intPart = dayPlanQty / dailyLhQty;
            int afterMachines = (Integer)mpFinalVo.getFieldValueByFieldName(day2Field) / dailyLhQty - intPart;
            if (afterMachines <=0){
                return resultArr;
            }
            //若今日的计划量<单日硫化量 且 今日计划量 >= 单模起排量*明日的硫化机台数，则今日的增模台数 = 明日的硫化机台数
            int tmpQty = afterMachines * changeMouldFirstQty;
            if (remainQty < dailyLhQty && remainQty>=tmpQty){
                resultArr[0] = afterMachines;
                resultArr[1] = 0;
                resultArr[2] = 0;
                resultArr[3] = afterMachines;
                //例子：
                //16 46
                //40（32+8） 104(每日52)
                if (remainQty != changeMouldBlockQty || remainQty != changeMouldXBlockQty){
                    int mouldCount = remainQty / changeMouldFirstQty;
                    //3是换活块32与换模8的倍差
                    if (mouldCount > afterMachines && (mouldCount - afterMachines)<3){
                        resultArr[0] = mouldCount;
                        resultArr[3] = mouldCount;
                    }
                }
            }

            //增模数超过日硫化的情况，例子：48 276
            afterMachines = (Integer)mpFinalVo.getFieldValueByFieldName(day2Field) / dailyLhQty;
            if ((afterMachines * changeMouldFirstQty) >= dailyLhQty){
                resultArr[0] = afterMachines - intPart;
                resultArr[1] = 0;
                resultArr[2] = 0;
                resultArr[3] = afterMachines - intPart;
            }
        }
       return resultArr;
    }

    /**
     * 判断是否减模
     * @param mpFinalVo 定稿对象Vo
     * @param dayField 当日计划量字段
     * @param day1Field 昨日计划量字段
     * @return
     */
    public boolean isDecMould(BaseEntity mpFinalVo,String dayField,String day1Field){
        if (mpFinalVo.getFieldValueByFieldName(dayField) == null){
            return false;
        }

        // 日计划量
        Integer dayPlanQty = (Integer) mpFinalVo.getFieldValueByFieldName(dayField);

        //1. 若次日计划量 < 当日计划量
      /*  if (mpFinalVo.getFieldValueByFieldName(day2Field) != null &&
                (Integer)mpFinalVo.getFieldValueByFieldName(day2Field) < dayPlanQty) {
            return true;
        }*/

        //2. 若昨日计划量 > 当日计划量
        if (mpFinalVo.getFieldValueByFieldName(day1Field) != null &&
                (Integer)mpFinalVo.getFieldValueByFieldName(day1Field) > dayPlanQty) {
            return true;
        }
        return false;
    }

    /**
     * 获取新的上机日期
     * @param startDay 开始日
     * @param endDay 结束日
     * @param dailyCapacityLimitVoMap 日产能Map
     * @return 新的上机日期
     */
    public Integer getNewOnLineDay(int startDay,int endDay, Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap){
        if (PubUtil.isEmpty(dailyCapacityLimitVoMap)){
            return null;
        }
        if (startDay > endDay){
            return null;
        }

        MpDailyCapacityLimitVo dailyCapacityLimitVo;
        for (int i = startDay; i<= endDay; i++){
            dailyCapacityLimitVo = dailyCapacityLimitVoMap.get(i);
            if (dailyCapacityLimitVo == null) {
                continue;
            }
            if (dailyCapacityLimitVo.getUsedEmbryoTypes() < dailyCapacityLimitVo.getMaxEmbryoTypes() &&
                    dailyCapacityLimitVo.getUsedLhMachines() < dailyCapacityLimitVo.getMaxLhMachines()){
                return i;
            }
        }
        return null;
    }

    /**
     * 检查产能是否满足
     * @param dailyCapacityLimitVo 日产能限制
     * @return true-满足，false-不满足
     */
    public boolean checkCapacitySatisfy(MpDailyCapacityLimitVo dailyCapacityLimitVo){
        return dailyCapacityLimitVo.getUsedEmbryoTypes() <= dailyCapacityLimitVo.getMaxEmbryoTypes() &&
                dailyCapacityLimitVo.getUsedLhMachines() <= dailyCapacityLimitVo.getMaxLhMachines();
    }

    /**
     * 预检产能是否满足,
     * @param dailyCapacityLimitVo 日产能限制
     * @return true-满足，false-不满足
     */
    public boolean preCheckCapacitySatisfy(MpDailyCapacityLimitVo dailyCapacityLimitVo){
        if (dailyCapacityLimitVo == null) {
            return Boolean.FALSE;
        }
        return dailyCapacityLimitVo.getUsedEmbryoTypes() < dailyCapacityLimitVo.getMaxEmbryoTypes() &&
                dailyCapacityLimitVo.getUsedLhMachines() < dailyCapacityLimitVo.getMaxLhMachines();
    }

    public Integer getDayVulcanizationQty(BaseEntity mpFinalVo){
        throw new BusinessException("需要处理日硫化量");
    }

    /**
     * 获取胎胚字段
     * @return
     */
    public  String getEmbryoCodeField(){
        return "mainMaterialDesc";
    }

    /**
     * 获取主花纹字段
     * @return
     */
    public  String getMainPatternField(){
        return "mainPattern";
    }
}
