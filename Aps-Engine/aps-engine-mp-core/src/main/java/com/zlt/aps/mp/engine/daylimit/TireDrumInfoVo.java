package com.zlt.aps.mp.engine.daylimit;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.monthplan.api.domain.entity.MdmWorkWearInfo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 成型鼓-限制信息对象
 * 当前都是三股
 * 成型鼓、胎体鼓、带束层鼓
 *
 * @author ZLT
 * @date 20260119
 */
@Data
public class TireDrumInfoVo implements Serializable {
    /**
     * 分组Id
     */
    private String groupId;
    /**
     * 工厂
     */
    private String factoryCode;
    /**
     * 鼓类型：01 成型鼓 02 胎体鼓 03 带束层鼓
     */
    private String workWearType;
    /**
     * 英寸
     */
    private Set<String> proSizeSet;
    /**
     * 最大数量
     */
    private Integer maxLimitQty;
    /**
     * 鼓日限制信息集合
     */
    private Map<Integer, TireDrumDayInfoHelper> dayLimitInfoMap;

    /**
     * 根据成型鼓台账配置转化成成型鼓限制信息
     * 构建每日限制对象集合
     *
     * @param context      排产上下文
     * @param workWearInfo 成型鼓工装台账信息
     * @return
     */
    public static TireDrumInfoVo builder(Context context, MdmWorkWearInfo workWearInfo) {
        if (null == workWearInfo || null == workWearInfo.getId() || StringUtils.isBlank(workWearInfo.getWorkWearType())) {
            return null;
        }
        String groupId = String.valueOf(workWearInfo.getId());
        TireDrumInfoVo info = new TireDrumInfoVo();
        info.setGroupId(groupId);
        info.setFactoryCode(workWearInfo.getFactoryCode());
        info.setWorkWearType(workWearInfo.getWorkWearType());
        String proSizeInfo = workWearInfo.getSpecifications();
        Set<String> proSizeSet = Collections.emptySet();
        if (StringUtils.isNotBlank(proSizeInfo)) {
            proSizeSet = Stream.of(proSizeInfo.split(StringConstant.COMMA)).collect(Collectors.toSet());
        }
        info.setProSizeSet(proSizeSet);
        Integer totalQty = BigDecimal.ZERO.intValue();
        if (null != workWearInfo.getQty()) {
            totalQty = totalQty + workWearInfo.getQty();
        }
        info.setMaxLimitQty(totalQty);
        Set<Integer> productionDaySet = context.getProductionDay();
        if (CollectionUtils.isEmpty(productionDaySet)) {
            info.setDayLimitInfoMap(Collections.emptyMap());
            return info;
        }
        Integer maxLimitQty = totalQty;
        Map<Integer, TireDrumDayInfoHelper> dayLimitInfoMap = new HashMap<>(64);
        productionDaySet.forEach(productionDay -> {
            TireDrumDayInfoHelper dayLimitInfo = TireDrumDayInfoHelper.buildInit(groupId, productionDay, maxLimitQty);
            dayLimitInfoMap.put(productionDay, dayLimitInfo);
        });
        info.setDayLimitInfoMap(dayLimitInfoMap);
        return info;
    }

    /**
     * 排产计划，是否匹配到鼓
     * 英寸进行匹配
     *
     * @param productionPlan 同物料描述任意一条排产计划
     * @return
     */
    public boolean isMatch(MonthPlanProductionRequirePlanVo productionPlan) {
        if (null == productionPlan) {
            return false;
        }
        String proSize = productionPlan.getProSize();
        if (StringUtils.isBlank(proSize)) {
            return false;
        }
        return proSizeSet.contains(proSize);
    }

    /**
     * 根据英寸，匹配到鼓
     *
     * @param groupPlanProSize
     * @return
     */
    public boolean isMatch(String groupPlanProSize) {
        if (StringUtils.isBlank(groupPlanProSize)) {
            return false;
        }
        return proSizeSet.contains(groupPlanProSize);
    }


    /**
     * 获取剩余使用量的鼓数量
     * 在机结构场景，以第一天的量来确定
     *
     * @return
     */
    public Integer getLeftOverUsedQtyByContinueGroup() {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return BigDecimal.ZERO.intValue();
        }
        Optional<Map.Entry<Integer, TireDrumDayInfoHelper>> minEntry = dayLimitInfoMap.entrySet().stream().min(Map.Entry.comparingByKey());
        TireDrumDayInfoHelper dayLimit = minEntry.get().getValue();
        if (null == dayLimit) {
            return BigDecimal.ZERO.intValue();
        }
        return dayLimit.getLeftOverUsedQty();
    }
}
