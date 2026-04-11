package com.zlt.aps.mp.engine.domain.dto;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * 成型机台-分配分组计划信息
 * TBR按结构
 *
 * @author ZLT
 * @date 20251215
 */
@Getter
public class CxMachineAllocationPlanHelper implements Serializable {
    /**
     * 成型机台编号
     */
    private String cxMachineCode;
    /**
     * 分组计划信息
     */
    private ProductionPlanGroupInfo productionPlanInfo;

    /**
     * 分配的天数
     */
    private Integer allocationDay;

    /**
     * 起始天数
     */
    private Integer startDay;

    /**
     * 结束天数
     */
    private Integer endDay;

    /**
     * 20260410+ 释放优先级：值越小，越晚释放
     * 场景：只在同分组分配不同机台使用
     */
    private Integer releasePriority;
    /**
     * 续作规格信息
     */
    private Map<String, CxContinueSkuInfoHelper> continueSkuMap;
    /**
     * 当前最大硫化配比
     */
    private Integer maxRatio;
    /**
     * 当前最低硫化配比
     */
    private Integer minRatio;
    /**
     * 最大胎胚种类数
     */
    private Integer maxEmbryoCodeCount;
    /**
     * 是否进行延长收尾
     * 只在在机结构进行结构延长探测时使用
     */
    private boolean timeExtensionFlag;
    /**
     * 实际排产规格计划
     */
    private List<MonthPlanProductionRequirePlanVo> realProductionPlanList;
    /**
     * 备注说明
     */
    private String remark;

    /**
     * 构造函数
     *
     * @param cxMachineCode      成型机台
     * @param productionPlanInfo 分配的分组计划信息
     * @param lhRatio            硫化配比信息
     * @param continueSkuMap     续作规格信息
     * @param allocationDay      分配的天数
     * @param startDay           起始天数
     * @param endDay             结束天数
     */
    public CxMachineAllocationPlanHelper(String cxMachineCode, ProductionPlanGroupInfo productionPlanInfo, ProductGroupCxCapacityInfo lhRatio, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Integer allocationDay, Integer startDay, Integer endDay) {
        this.cxMachineCode = cxMachineCode;
        this.productionPlanInfo = productionPlanInfo;
        this.maxRatio = lhRatio.getMaxLhMachineCount();
        this.minRatio = productionPlanInfo.getClosureMinLhRatio();
        this.maxEmbryoCodeCount = lhRatio.getMaxEmbryoCodeCount();
        this.continueSkuMap = continueSkuMap;
        this.allocationDay = allocationDay;
        this.startDay = startDay;
        this.endDay = endDay;
        this.timeExtensionFlag = false;
        this.realProductionPlanList = new ArrayList<>();
    }

    /**
     * 机台分配的天产能范围只能有一个结构
     *
     * @return
     */
    public String getDuplicateKey() {
        String keyFormat = "%s|*|%s|*|%s";
        return String.format(keyFormat, cxMachineCode, startDay, endDay);
    }

    /**
     * 是否匹配当前段排产结构
     * 1、如果没有排产天数 = false
     * 2、有排产天数，则判断是否同机台
     * 2.1、如果是同机台，则判断是否同结构
     * 2.1.1、如果是同结构
     * 2.1.1.1、则判断是否同段，同段排除 = false
     * 2.1.1.2、如果不同段，则 = true
     * 2.1.2、如果是不同结构，则 = false
     * 2.2、如果是不同机台，则判断是否同结构
     * 2.2.1 如果是同结构 = true
     * 2.2.2 如果不是同结构 = false
     *
     * @param currentProductionInfo
     * @return
     */
    public boolean hasMatchProduction(CxMachineAllocationPlanHelper currentProductionInfo) {
        if (null == currentProductionInfo) {
            return false;
        }
        String currentGroupName = currentProductionInfo.getAllocationGroup();
        if (StringUtils.isBlank(currentGroupName)) {
            return false;
        }
        if (null == allocationDay || allocationDay <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        //同机台
        if (cxMachineCode.equals(currentProductionInfo.getCxMachineCode())) {
            if (!currentGroupName.equals(getAllocationGroup())) {
                return false;
            }
            String productionDuplicateKey = currentProductionInfo.getProductionDuplicateKey();
            return !productionDuplicateKey.equals(getProductionDuplicateKey());
        }
        //不同机台，只判断结构
        return currentGroupName.equals(getAllocationGroup());
    }

    /**
     * 分配到月底
     *
     * @param endDay  结束日
     * @param addDays 增加的天数
     */
    public void addAllocationDayToFull(Integer endDay, Integer addDays) {
        if (null == endDay || null == addDays) {
            return;
        }
        if (endDay <= this.endDay) {
            return;
        }
        if (addDays <= BigDecimal.ZERO.intValue()) {
            return;
        }
        this.endDay = endDay;
        Integer currentAllocationDay = this.allocationDay;
        this.allocationDay = currentAllocationDay + addDays;
    }

    /**
     * 同机台分配的天产能范围只能有一个结构
     * 结构|%|开始日|*|结束日
     *
     * @return
     */
    public String getProductionDuplicateKey() {
        String keyFormat = "%s|*|%s|*|%s";
        return String.format(keyFormat, productionPlanInfo.getGroupName(), startDay, endDay);
    }

    /**
     * 获取真正的收尾日
     *
     * @param context 排产上下文
     * @return
     */
    public Integer getRealConclusionDay(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> allCxMachineMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineMap)) {
            return null;
        }
        CxMachineBaseInfoVo cxMachineInfo = allCxMachineMap.get(cxMachineCode);
        if (null == cxMachineInfo) {
            return null;
        }
        Set<Integer> stopDayInfo = Optional.ofNullable(cxMachineInfo.getStopDayInfo()).orElse(Collections.emptySet());
        if (CollectionUtils.isEmpty(stopDayInfo)) {
            return endDay;
        }
        Integer realConclusionDay = endDay;
        for (; ; ) {
            if (realConclusionDay <= startDay) {
                break;
            }
            if (!stopDayInfo.contains(realConclusionDay)) {
                break;
            }
            realConclusionDay = realConclusionDay - BigDecimal.ONE.intValue();
        }
        return realConclusionDay;
    }

    /**
     * 分配的分组名
     *
     * @return
     */
    public String getAllocationGroup() {
        if (null == productionPlanInfo) {
            return "";
        }
        return productionPlanInfo.getGroupName();
    }

    /**
     * 延长收尾处理 +1
     *
     * @param newEndDay
     */
    public void timeExtensionOneDay(Integer newEndDay) {
        if (null == newEndDay) {
            return;
        }
        if (endDay >= newEndDay) {
            return;
        }
        endDay = newEndDay;
        allocationDay = allocationDay + BigDecimal.ONE.intValue();
    }

    /**
     * 更新分配天数，因特殊材料进行调整
     *
     * @param newAllocationDays 新的分配天数
     */
    public void updateAllocationDayBySpecialMaterial(Integer newAllocationDays) {
        allocationDay = newAllocationDays;
    }

    /**
     * 结构提前收尾处理
     * 新的收尾时间点及提前收尾的天数
     *
     * @param conclusionDay
     * @param deductionDay
     */
    public void beforeConclusion(Integer conclusionDay, Integer deductionDay) {
        endDay = conclusionDay - BigDecimal.ONE.intValue();
        String tisFormat = I18nUtil.getMessage("alg.data.groupCapacity.beforeConclusion");
        remark = String.format(tisFormat, conclusionDay);
        if (allocationDay <= deductionDay) {
            allocationDay = BigDecimal.ZERO.intValue();
            return;
        }
        allocationDay = allocationDay - deductionDay;
    }

    /**
     * 标记进行结构延长探测处理
     */
    public void markTimeExtension() {
        this.timeExtensionFlag = true;
    }

    /**
     * 获取延长日信息
     * 分组名|*|成型机编号|*|排产日
     *
     * @param timeExtensionEndDay 分组延长收尾日
     * @return
     */
    public String getTimeExtensionDayInfo(Integer timeExtensionEndDay) {
        String timeExtensionKeyFormat = "%s|*|%s|*|%s";
        if (null == timeExtensionEndDay) {
            return "";
        }
        return String.format(timeExtensionKeyFormat, getAllocationGroup(), cxMachineCode, timeExtensionEndDay);
    }

    /**
     * 获取日分配Key
     *
     * @return
     */
    public String getDayAllocationKey() {
        String allocationKeyFormat = "%s|*|%s";
        return String.format(allocationKeyFormat, productionPlanInfo.getGroupName(), cxMachineCode);
    }

    /**
     * 获取分配的最小日排产量
     * = 硫化机台配比 * Sku日最小硫化量
     */
    public Integer getDayMinAllocationQty() {
        if (null == productionPlanInfo) {
            return BigDecimal.ZERO.intValue();
        }
        if (null == maxRatio) {
            return BigDecimal.ZERO.intValue();
        }
        return productionPlanInfo.getDayMinCapacityByLhRatio(maxRatio);
    }

    /**
     * 设置释放优先级
     *
     * @param releasePriority
     */
    public void setReleasePriority(int releasePriority) {
        this.releasePriority = releasePriority;
    }

}
