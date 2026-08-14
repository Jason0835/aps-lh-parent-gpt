package com.zlt.aps.mp.engine.domain.dto;

import com.google.common.collect.Sets;
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
     * 初始备份--后续重排使用
     */
    private CxMachineAllocationPlanHelper cloneObject;
    /**
     * 拼接字符
     */
    private static final String TIME_EXTENSION_KEY_FORMAT = "%s|@|%s|@|%s";
    /**
     * 分组切换日限制时，前分组需要延长到的排产日
     */
    private Integer timeExtensionDay;
    /**
     * 分组切换日，前分组分配信息，记录是否需要延长处理
     */
    private CxMachineAllocationPlanHelper beforeAllocation;
    /**
     * 是否为切换英寸
     */
    private boolean isChangeProSize;

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
        this.timeExtensionFlag = true;
        this.realProductionPlanList = new ArrayList<>();
        //20260428+ 备份使用
        CxMachineAllocationPlanHelper cloneObject = new CxMachineAllocationPlanHelper();
        cloneObject.cxMachineCode = cxMachineCode;
        cloneObject.productionPlanInfo = productionPlanInfo;
        cloneObject.maxRatio = lhRatio.getMaxLhMachineCount();
        cloneObject.minRatio = productionPlanInfo.getClosureMinLhRatio();
        cloneObject.maxEmbryoCodeCount = lhRatio.getMaxEmbryoCodeCount();
        cloneObject.continueSkuMap = continueSkuMap;
        cloneObject.allocationDay = allocationDay;
        cloneObject.startDay = startDay;
        cloneObject.endDay = endDay;
        cloneObject.timeExtensionFlag = true;
        cloneObject.realProductionPlanList = new ArrayList<>();
        this.cloneObject = cloneObject;
    }

    /**
     * 还原配置信息
     */
    public void restoreConfiguration() {
        if (null == cloneObject) {
            return;
        }
        this.cxMachineCode = cloneObject.getCxMachineCode();
        this.productionPlanInfo = cloneObject.getProductionPlanInfo();
        this.maxRatio = cloneObject.getMaxRatio();
        this.minRatio = cloneObject.getMinRatio();
        this.maxEmbryoCodeCount = cloneObject.getMaxEmbryoCodeCount();
        this.continueSkuMap = cloneObject.getContinueSkuMap();
        this.allocationDay = cloneObject.getAllocationDay();
        this.startDay = cloneObject.getStartDay();
        this.endDay = cloneObject.getEndDay();
        this.timeExtensionFlag = cloneObject.timeExtensionFlag;
        this.realProductionPlanList = cloneObject.getRealProductionPlanList();
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
     * 结构切换是否为切换英寸
     *
     * @param isChangeProSize
     */
    public void setChangeProSize(boolean isChangeProSize) {
        this.isChangeProSize = isChangeProSize;
    }

    /**
     * 获取分配信息对应的真实可排产日信息集合
     *
     * @param context 排产上下文
     * @return
     */
    public Set<Integer> getRealProductionDayInfo(Context context) {
        if (StringUtils.isBlank(cxMachineCode)) {
            return Collections.emptySet();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineInfoByCode(cxMachineCode);
        if (null == cxMachineInfo) {
            return Collections.emptySet();
        }
        Set<Integer> stopDayInfo = Optional.ofNullable(cxMachineInfo.getStopDayInfo()).orElse(Collections.emptySet());
        Set<Integer> realProductionDayInfo = Sets.newHashSet();
        for (Integer day = startDay; day <= endDay; ) {
            if (!stopDayInfo.contains(day)) {
                realProductionDayInfo.add(day);
            }
            day = day + BigDecimal.ONE.intValue();
        }
        if (CollectionUtils.isEmpty(realProductionDayInfo)) {
            return Collections.emptySet();
        }
        return realProductionDayInfo;
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
     * 因分组切换日限制，导致前分组需要延长收尾的日
     *
     * @param timeExtensionDay
     */
    public void setTimeExtensionDayByChangeLimit(Integer timeExtensionDay) {
        if (null == timeExtensionDay) {
            return;
        }
        this.timeExtensionDay = timeExtensionDay;
    }

    /**
     * 因分组切换日限制，存储前分组配置
     *
     * @param beforeAllocation
     */
    public void setBeforeAllocationByChangeLimit(CxMachineAllocationPlanHelper beforeAllocation) {
        this.beforeAllocation = beforeAllocation;
    }

    /**
     * 在机分组(TBR-结构)因指定业务导致的强制下机，故而新的收尾日 = newEndDay
     * 根据新的分配结束日，调整分配的结束日及分配天数
     * 并返回中间的实际排产日信息
     *
     * @param productionContext 排产上下文
     * @param newEndDay         新的分配结束日
     */
    public Set<Integer> getReduceDaysByForceOffline(TbrProductionContext productionContext, Integer newEndDay) {
        if (StringUtils.isBlank(cxMachineCode) || null == newEndDay) {
            return Collections.emptySet();
        }
        //超出排产周期，则无效
        if (newEndDay < BigDecimal.ONE.intValue() || newEndDay > productionContext.getMonthDays()) {
            return Collections.emptySet();
        }
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineInfoByCode(cxMachineCode);
        if (null == cxMachineInfo) {
            return Collections.emptySet();
        }
        Integer originEndDay = endDay;
        if (newEndDay.equals(originEndDay)) {
            return Collections.emptySet();
        }
        Integer startDay = Math.min(originEndDay, newEndDay) + BigDecimal.ONE.intValue();
        Integer endDay = Math.max(originEndDay, newEndDay);
        Set<Integer> adjustDaySet = Sets.newHashSet();
        for (Integer index = startDay; index <= endDay; index++) {
            if (!cxMachineInfo.getStopDayInfo().contains(index)) {
                adjustDaySet.add(index);
            }
        }
        return adjustDaySet;
    }

    /**
     * 20260731+
     * 因前结构达不到最低实单要求，后结构自动提前1天
     *
     * @param newStartDay      新的起始日
     * @param addAllocationDay 增加的分配天数
     */
    public void autoAdvanceProduction(Integer newStartDay, Integer addAllocationDay) {
        if (null == newStartDay || null == addAllocationDay) {
            return;
        }
        if (newStartDay < BigDecimal.ONE.intValue() || addAllocationDay < BigDecimal.ZERO.intValue()) {
            return;
        }
        if (newStartDay >= startDay) {
            return;
        }
        startDay = newStartDay;
        allocationDay = allocationDay + addAllocationDay;
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
        if (allocationDay <= deductionDay) {
            allocationDay = BigDecimal.ZERO.intValue();
            return;
        }
        allocationDay = allocationDay - deductionDay;
    }

    /**
     * 标记该机台
     * 不可进行延长探测处理
     * 业务场景：
     * 1、指定业务，续作在机强行下机
     * 2、指定业务，控制最长排产时间
     */
    public void markNoTimeExtension() {
        this.timeExtensionFlag = false;
    }

    /**
     * 获取延长日信息
     * 分组名|@|成型机编号|@|排产日
     *
     * @param timeExtensionEndDay 分组延长收尾日
     * @return
     */
    public String getTimeExtensionDayInfo(Integer timeExtensionEndDay) {
        if (null == timeExtensionEndDay) {
            return "";
        }
        return String.format(TIME_EXTENSION_KEY_FORMAT, getAllocationGroup(), cxMachineCode, timeExtensionEndDay);
    }

    /**
     * 获取延长日前缀信息
     *
     * @return
     */
    public String getTimeExtensionPrefix() {
        return String.format(TIME_EXTENSION_KEY_FORMAT, getAllocationGroup(), cxMachineCode, "");
    }

    /**
     * 20260427+
     * 因在多机台时，得不到具体排产Sku
     * 修改成从计划中获取
     *
     * @return
     */
    public List<MonthPlanProductionRequirePlanVo> getRealProductionPlanList() {
        if (!CollectionUtils.isEmpty(realProductionPlanList)) {
            return realProductionPlanList;
        }
        if (null == productionPlanInfo) {
            return Collections.emptyList();
        }
        return productionPlanInfo.getGroupPlanData();
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

    /**
     * 排产英寸
     *
     * @return
     */
    public String getProductionProSize() {
        return productionPlanInfo.getProSizeInfo();
    }

    private CxMachineAllocationPlanHelper() {

    }
}
