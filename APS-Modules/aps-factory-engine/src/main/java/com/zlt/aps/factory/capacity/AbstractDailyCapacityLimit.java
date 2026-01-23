package com.zlt.aps.factory.capacity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ConstructionStageEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.monthplan.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.common.utils.PubUtil;
import io.swagger.models.auth.In;

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
     * @param startDay 开始日
     * @param mpProdFinalList 月计划定稿列表
     * @param mpStructAllocList 月计划结构转产列表
     */
    public Map<Integer, MpDailyCapacityLimitVo> getDailyCapacityLimitMap(int startDay, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                        List<MpStructureAllocation> mpStructAllocList){
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = new HashMap<>();
        MpDailyCapacityLimitVo dailyCapacityLimitVo;
        for (int i = startDay; i<= FactoryConstant.MONTH_MAX_DAY; i++){
            dailyCapacityLimitVo = dailyCapacityLimitVoMap.get(i);
            if (dailyCapacityLimitVo == null){
                dailyCapacityLimitVo = new MpDailyCapacityLimitVo();
            }
            dailyCapacityLimitVo.setDailyDate(i);
            // 1、设置每日硫化机台总限制数、每日胎胚种类总限制数
            setMaxLhMachinesWithEmbryoTypes(mpStructAllocList,i,dailyCapacityLimitVo);
            // 2、计算每日硫化机台数、每日胎胚种类数
            calcLhMachinesWithEmbryoTypes(mpProdFinalList,i,dailyCapacityLimitVo,null);
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
                                                 MpDailyCapacityLimitVo dailyCapacityLimitVo) {
        if(PubUtil.isEmpty(mpStructAllocList)){
            return;
        }

        int iMaxLhMachines = 0;
        int iMaxEmbryoTypes = 0;
        for (MpStructureAllocation strutAllocVo: mpStructAllocList){
            if (iDay >= strutAllocVo.getBeginDay() && iDay <= strutAllocVo.getEndDay()){
                iMaxLhMachines += strutAllocVo.getMaxLhMachineCount();
                iMaxEmbryoTypes += strutAllocVo.getMaxEmbryoCodeCount();
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
    public void calcLhMachinesWithEmbryoTypes(List<? extends BaseEntity> mpProdFinalList, int iDay,
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
                intPart += Math.ceil((double) dayPlanQty / dailyLhQty);

                // 计算主花纹向下的硫化机台数
                if (mpFinalVo.getFieldValueByFieldName(getMainPatternField()).equals(mainPattern)){
                    patternIntPart += Math.ceil((double) dayPlanQty / dailyLhQty);
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
        if (startDay >= endDay){
            return null;
        }

        MpDailyCapacityLimitVo dailyCapacityLimitVo;
        for (int i = startDay; i< endDay; i++){
            dailyCapacityLimitVo = dailyCapacityLimitVoMap.get(i);
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
