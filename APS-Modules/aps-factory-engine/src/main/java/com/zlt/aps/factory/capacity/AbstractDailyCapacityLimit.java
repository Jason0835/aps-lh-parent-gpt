package com.zlt.aps.factory.capacity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.tlt.aps.constant.FactoryConstant;
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
     * 初始化日产能
     * @param startDay 开始日
     * @param mpProdFinalList 月计划定稿列表
     * @param mpStructAllocList 月计划结构转产列表
     */
    public Map<Integer, MpDailyCapacityLimitVo> getDailyCapacityLimitMap(int startDay, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                        List<MpStructureAllocation> mpStructAllocList){
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = new HashMap<>();
        MpDailyCapacityLimitVo dailyCapacityLimitVo;
        for (int i = startDay; i< 31; i++){
            dailyCapacityLimitVo = dailyCapacityLimitVoMap.get(i);
            if (dailyCapacityLimitVo == null){
                dailyCapacityLimitVo = new MpDailyCapacityLimitVo();
            }
            dailyCapacityLimitVo.setDailyDate(i);
            // 1、设置每日硫化机台总限制数、每日胎胚种类总限制数
            setMaxLhMachinesWithEmbryoTypes(mpStructAllocList,i,dailyCapacityLimitVo);
            // 2、计算每日硫化机台数、每日胎胚种类数
            calcLhMachinesWithEmbryoTypes(mpProdFinalList,i,dailyCapacityLimitVo);
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
                iMaxLhMachines += dailyCapacityLimitVo.getMaxLhMachines();
                iMaxEmbryoTypes += dailyCapacityLimitVo.getMaxEmbryoTypes();
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
     */
    public void calcLhMachinesWithEmbryoTypes(List<? extends BaseEntity> mpProdFinalList, int iDay,
                                               MpDailyCapacityLimitVo dailyCapacityLimitVo) {
        int intPart = 0;
        int remainderCount = 0;
        int changeMouldCount = 0;
        int dayPlanQty,dailyLhQty;
        String dayField = FactoryConstant.DAY_FIELD + iDay;
        String embryoFieldValue;
        for (BaseEntity mpFinalVo: mpProdFinalList){
            if (mpFinalVo.getFieldValueByFieldName(dayField) == null) {
                continue;
            }
            // 日计划量
            dayPlanQty = (Integer) mpFinalVo.getFieldValueByFieldName(dayField);
            // 日硫化量 = 单模硫化量 * 2；
            dailyLhQty = getDayVulcanizationQty(mpFinalVo);
            // 取整(日计划量/日单台硫化量)
            intPart += dayPlanQty / dailyLhQty;
            // 统计有余数的SKU个数
            remainderCount += dayPlanQty % dailyLhQty > 0 ? 1:0;
            // 统计换模的SKU个数
            changeMouldCount += dayPlanQty > 0 && dayPlanQty < dailyLhQty ? 1:0;

            // 统计胎胚种类数
            embryoFieldValue = (String) mpFinalVo.getFieldValueByFieldName(getEmbryoCodeField());
            dailyCapacityLimitVo.getEmbryoCodes().add(embryoFieldValue);
        }
        int iCount = Math.max(remainderCount,changeMouldCount);

        // 日硫化机台数
        dailyCapacityLimitVo.setUsedLhMachines(intPart + iCount);
        // 每日胎胚种类数
        dailyCapacityLimitVo.setUsedEmbryoTypes(dailyCapacityLimitVo.getEmbryoCodes().size());
    }

    public Integer getDayVulcanizationQty(BaseEntity mpFinalVo){
        throw new BusinessException("需要处理日硫化量");
    }

    /**
     * 获取胎胚字段
     * @return
     */
    public  String getEmbryoCodeField(){
        return "embryoCode";
    }
}
