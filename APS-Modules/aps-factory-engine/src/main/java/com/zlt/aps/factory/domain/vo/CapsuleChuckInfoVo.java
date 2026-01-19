package com.zlt.aps.factory.domain.vo;

import com.tlt.aps.constant.StringConstant;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CapsuleChuckDayInfoHelper;
import com.zlt.aps.monthplan.api.domain.entity.MdmCapsuleChuck;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 胶囊卡盘台账信息
 *
 * @author ZLT
 * @date 20260119
 */
@Data
public class CapsuleChuckInfoVo implements Serializable {
    /**
     * 胶囊卡盘组ID
     */
    private String groupId;
    /**
     * 工厂编码
     */
    private String factoryCode;
    /**
     * 总数量
     */
    private Integer maxLimitQty;
    /**
     * 配比的英寸
     */
    private Set<String> proSizeSet;
    /**
     * 匹配的规格
     */
    private Set<String> specificationsSet;
    /**
     * 胶囊卡盘日限制信息集合
     */
    private Map<Integer, CapsuleChuckDayInfoHelper> dayLimitInfoMap;

    /**
     * 根据胶囊卡盘配置转化成胶囊卡盘限制信息
     * 构建每日限制对象集合
     *
     * @param context      排产上下文
     * @param capsuleChuck 胶囊卡盘配置
     * @return
     */
    public static CapsuleChuckInfoVo builder(Context context, MdmCapsuleChuck capsuleChuck) {
        if (null == capsuleChuck || null == capsuleChuck.getId()) {
            return null;
        }
        String groupId = String.valueOf(capsuleChuck.getId());
        CapsuleChuckInfoVo info = new CapsuleChuckInfoVo();
        info.setGroupId(groupId);
        info.setFactoryCode(capsuleChuck.getFactoryCode());
        String proSizeInfo = capsuleChuck.getProSize();
        Set<String> proSizeSet = Collections.emptySet();
        if (StringUtils.isNotBlank(proSizeInfo)) {
            proSizeSet = Stream.of(proSizeInfo.split(StringConstant.COMMA)).collect(Collectors.toSet());
        }
        info.setProSizeSet(proSizeSet);
        Set<String> specificationsSet = Collections.emptySet();
        String specifications = capsuleChuck.getSpecifications();
        if (StringUtils.isNotBlank(specifications)) {
            specificationsSet = Stream.of(specifications.split(StringConstant.COMMA)).collect(Collectors.toSet());
        }
        info.setSpecificationsSet(specificationsSet);
        Integer totalQty = BigDecimal.ZERO.intValue();
        if (null != capsuleChuck.getInternalQty()) {
            totalQty = totalQty + capsuleChuck.getInternalQty();
        }
        if (null != capsuleChuck.getNewChuckQty()) {
            totalQty = totalQty + capsuleChuck.getNewChuckQty();
        }
        info.setMaxLimitQty(totalQty);
        Set<Integer> productionDaySet = context.getProductionDay();
        if (CollectionUtils.isEmpty(productionDaySet)) {
            info.setDayLimitInfoMap(Collections.emptyMap());
            return info;
        }
        Integer maxLimitQty = totalQty;
        Map<Integer, CapsuleChuckDayInfoHelper> dayLimitInfoMap = new HashMap<>(64);
        productionDaySet.forEach(productionDay -> {
            CapsuleChuckDayInfoHelper dayLimitInfo = CapsuleChuckDayInfoHelper.buildInit(groupId, productionDay, maxLimitQty);
            dayLimitInfoMap.put(productionDay, dayLimitInfo);
        });
        info.setDayLimitInfoMap(dayLimitInfoMap);
        return info;
    }

    /**
     * 排产计划，是否匹配到胶囊卡盘
     * 规格或是英寸进行匹配
     *
     * @param productionPlan 同物料描述任意一条排产计划
     * @return
     */
    public boolean isMatch(MonthPlanProductionRequirePlanVo productionPlan) {
        if (null == productionPlan) {
            return false;
        }
        String proSize = productionPlan.getProSize();
        String specifications = productionPlan.getSpecifications();
        if (StringUtils.isBlank(proSize) || StringUtils.isBlank(specifications)) {
            return false;
        }
        if (specificationsSet.contains(specifications)) {
            return true;
        }
        return proSizeSet.contains(proSize);
    }

    /**
     * 获取剩余使用量的模具数量
     * 续作Sku场景，以第一天的排产量来确定
     *
     * @return
     */
    public Integer getLeftOverUsedQtyByContinueSku() {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return BigDecimal.ZERO.intValue();
        }
        Optional<Map.Entry<Integer, CapsuleChuckDayInfoHelper>> minEntry = dayLimitInfoMap.entrySet().stream().min(Map.Entry.comparingByKey());
        CapsuleChuckDayInfoHelper dayLimit = minEntry.get().getValue();
        if (null == dayLimit) {
            return BigDecimal.ZERO.intValue();
        }
        return dayLimit.getLeftOverUsedQty();
    }

    /**
     * 获取可进行两副模具的排产日范围
     *
     * @return
     */
    public Set<Integer> getEnableDoubleMouldProductionRange() {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return new HashSet<>();
        }
        List<CapsuleChuckDayInfoHelper> dayLimitList = dayLimitInfoMap.values().stream().collect(Collectors.toList());
        List<CapsuleChuckDayInfoHelper> enableList = dayLimitList.stream().filter(singleDay -> singleDay.getLeftOverUsedQty() >= ProductionConstant.DOUBLE_MOULD_PRODUCTION).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(enableList)) {
            return new HashSet<>();
        }
        return enableList.stream().map(CapsuleChuckDayInfoHelper::getProductionDay).collect(Collectors.toSet());
    }

    /**
     * 清空模具分配比例的每日使用量
     */
    public void clearDayUsed() {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return;
        }
        dayLimitInfoMap.forEach((productionDay, dayLimit) -> dayLimit.clearUsedCount());
    }

    /**
     * 在productionDay天，模具使用量+1
     *
     * @param productionDay 排产日
     * @param mouldCode     型腔模号
     */
    public void addUsedCount(Integer productionDay, String mouldCode) {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return;
        }
        CapsuleChuckDayInfoHelper dayLimit = dayLimitInfoMap.get(productionDay);
        if (null == dayLimit) {
            return;
        }
        dayLimit.addUsedCount(mouldCode);
    }

}
