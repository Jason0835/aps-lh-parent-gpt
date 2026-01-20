package com.zlt.aps.factory.scheduling;

import com.zlt.aps.factory.domain.dto.DayCapacityLimitHelper;
import com.zlt.aps.factory.domain.dto.TireDrumDayInfoHelper;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基础配置信息数据缓存容器
 *
 * @author ZLT
 * @date 20251221
 */
@Data
public class BaseDataContainer implements Serializable {
    /**
     * 排产参数配置信息
     */
    ProductionCapacityParamConfiguration paramConfiguration;
    /**
     * 成型产能信息集合
     * key=cxMachineCode : value=成型机信息
     */
    Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo;
    /**
     * 成型鼓(工装台账)信息集合
     * key=鼓类型 ：value=鼓类型台账信息{key=鼓groupId ：value=数量}
     */
    Map<String, Map<String, TireDrumInfoVo>> tireDrumInfoMap;
    /**
     * 模具信息
     * key=型腔模号 : value=模具信息
     */
    Map<String, ProductionMouldInfoVo> mouldInfoMap;
    /**
     * Sku与模具关系
     * key=materialDesc : value=关系列表
     */
    Map<String, List<MonthPlanProductMouldInfoVo>> skuMouldRelationMap;
    /**
     * 结构+主花纹的模具关系
     * key=group+主花纹 : value=模具关系列表
     */
    Map<String, List<ProductionMouldInfoVo>> groupMainPatternMouldRelationMap;
    /**
     * 结构+主花纹的模具配比配置
     * key=group+主花纹 ：value=配比配置，同时需转化成每天的量信息
     */
    Map<String, MouldAllocationInfoVo> groupMainPatternAllocationLimitMap;
    /**
     * 模壳总数信息
     * key=模壳标准 : value=模壳标准数量
     */
    Map<String, MouldShellBaseInfoVo> mouldShellMap;
    /**
     * 胶囊卡盘总数信息
     * key=胶囊卡盘GroupId : value=胶囊卡盘总数信息
     */
    Map<String, CapsuleChuckInfoVo> capsuleChuckInfoMap;
    /**
     * 生胎对应的特殊原材料配置信息
     * key=胎胚号 : value={ key=特殊原材料编码 : value=比例}
     */
    Map<String, Map<String, BigDecimal>> embryoSpecialMaterialInfoMap;
    /**
     * 日产能控制信息
     * key=排产日 : value=日排产控制信息
     */
    Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap;
    /**
     * 分组(结构)成型硫化配比
     */
    List<MonthPlanStructureLhRatioVo> structureLhRatioList;

    /**
     * 所有模具信息--用于记录模具日志
     * key=型腔模号 : value=模具信息
     */
    Map<String, ProductionMouldInfoVo> allMouldInfoMap;

    /**
     * 判断同结构下前后两个Sku是否共用模具
     *
     * @param beforeSku 前Sku
     * @param afterSku  后Sku
     * @return
     */
    public boolean isShareMouldSameGroup(String beforeSku, String afterSku) {
        if (StringUtils.isBlank(beforeSku) || StringUtils.isBlank(afterSku)) {
            return false;
        }
        if (beforeSku.equals(afterSku)) {
            return true;
        }
        List<MonthPlanProductMouldInfoVo> beforeMouldList = skuMouldRelationMap.get(beforeSku);
        List<MonthPlanProductMouldInfoVo> afterMouldList = skuMouldRelationMap.get(afterSku);
        if (CollectionUtils.isEmpty(beforeMouldList) || CollectionUtils.isEmpty(afterMouldList)) {
            return false;
        }
        Set<String> beforeMouldSet = beforeMouldList.stream().map(MonthPlanProductMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        Set<String> afterMouldSet = afterMouldList.stream().map(MonthPlanProductMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        Set<String> intersectionSet = beforeMouldSet.stream().filter(afterMouldSet::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(intersectionSet)) {
            return false;
        }
        return true;
    }

    /**
     * 获取proSize的剩余机台数数量-在机结构
     * 需要根据成型鼓、胎体鼓、带束层鼓中剩余量最小的
     *
     * @param proSize
     * @return
     */
    public Integer getLeftOverQtyByProSizeAndContinueGroupPlan(String proSize) {
        if (StringUtils.isBlank(proSize) || CollectionUtils.isEmpty(tireDrumInfoMap)) {
            return BigDecimal.ZERO.intValue();
        }
        //获取匹配的工装类型各自信息
        Map<String, Integer> workWeakTypeLimitQtyMap = new HashMap<>();
        tireDrumInfoMap.forEach((workWeakType, limitGroupMap) -> {
            List<TireDrumInfoVo> limitGroupList = limitGroupMap.values().stream().filter(singleGroupLimit -> singleGroupLimit.isMatch(proSize)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(limitGroupList)) {
                workWeakTypeLimitQtyMap.put(workWeakType, BigDecimal.ZERO.intValue());
                return;
            }
            Integer sumQty = limitGroupList.stream().mapToInt(TireDrumInfoVo::getLeftOverUsedQtyByContinueGroup).sum();
            workWeakTypeLimitQtyMap.put(workWeakType, sumQty);
        });
        if (CollectionUtils.isEmpty(workWeakTypeLimitQtyMap)) {
            return BigDecimal.ZERO.intValue();
        }
        //取得工装类型中剩余量最低的
        Optional<Map.Entry<String, Integer>> minEntry = workWeakTypeLimitQtyMap.entrySet().stream().min(Map.Entry.comparingByValue());
        return minEntry.get().getValue();
    }

    /**
     * 获取符合proSize的成型工装量排产日集合
     * 需要3鼓都有剩余量1
     *
     * @param proSize
     * @return
     */
    public Set<Integer> getLeftOverProductionDayInfo(String proSize) {
        if (StringUtils.isBlank(proSize) || CollectionUtils.isEmpty(tireDrumInfoMap)) {
            return Collections.emptySet();
        }
        Map<String, Map<String, TireDrumInfoVo>> effectiveMap = new HashMap<>();
        tireDrumInfoMap.forEach((workWeakType, limitGroupMap) -> {
            List<TireDrumInfoVo> limitGroupList = limitGroupMap.values().stream().filter(singleGroupLimit -> singleGroupLimit.isMatch(proSize)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(limitGroupList)) {
                return;
            }
            //todo 每天的剩余使用量

        });
        return null;
    }

    /**
     * 按groupName增加在productionDay日的成型工装使用量
     * 因当前TBR都是三股机台，故而工装类型全部+1
     *
     * @param productionDay 排产日
     * @param groupName     当前为英寸
     * @param cxMachineCode 成型机台
     */
    public void addUsedCount(Integer productionDay, String groupName, String cxMachineCode) {
        if (isCheckEmpty(productionDay, groupName)) {
            return;
        }
        tireDrumInfoMap.forEach((workWeakType, limitGroupMap) -> {
            TireDrumDayInfoHelper dayLimitInfo = getDayLimitInfo(limitGroupMap, groupName, productionDay);
            if (null == dayLimitInfo) {
                return;
            }
            dayLimitInfo.addUsedCount(cxMachineCode);
        });
    }

    /**
     * 因提前收尾，导致按groupName在productionDay日的成型工装使用量释放，即使用量 - 1
     * 因当前TBR都是三股机台，故而工装类型全部 - 1
     *
     * @param productionDay 排产日
     * @param groupName     当前为英寸
     * @param cxMachineCode 成型机台
     */
    public void releaseUsedCount(Integer productionDay, String groupName, String cxMachineCode) {
        if (isCheckEmpty(productionDay, groupName)) {
            return;
        }
        tireDrumInfoMap.forEach((workWeakType, limitGroupMap) -> {
            TireDrumDayInfoHelper dayLimitInfo = getDayLimitInfo(limitGroupMap, groupName, productionDay);
            if (null == dayLimitInfo) {
                return;
            }
            dayLimitInfo.deductionUsedCount(cxMachineCode);
        });
    }

    /**
     * 根据成型工装类型限制信息，获取匹配proSize，在productionDay
     * 的日排产限制对象
     *
     * @param workWeakTypeLimitInfo 某种成型工装类型限制对象
     * @param proSize               英寸
     * @param productionDay         排产日
     * @return
     */
    private TireDrumDayInfoHelper getDayLimitInfo(Map<String, TireDrumInfoVo> workWeakTypeLimitInfo, String proSize, Integer productionDay) {
        if (CollectionUtils.isEmpty(workWeakTypeLimitInfo) || StringUtils.isBlank(proSize) || null == productionDay) {
            return null;
        }
        List<TireDrumInfoVo> limitGroupList = workWeakTypeLimitInfo.values().stream().filter(singleGroupLimit -> singleGroupLimit.isMatch(proSize)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(limitGroupList)) {
            //没有找到与proSize匹配的成型工装限制信息
            return null;
        }
        //随意一条
        limitGroupList.sort(Comparator.comparing(TireDrumInfoVo::getGroupId));
        TireDrumInfoVo tireDrumInfo = limitGroupList.get(BigDecimal.ZERO.intValue());
        Map<Integer, TireDrumDayInfoHelper> dayLimitInfoMap = tireDrumInfo.getDayLimitInfoMap();
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            //没有找到在productionDay的日排产限制信息
            return null;
        }
        return dayLimitInfoMap.get(productionDay);
    }

    /**
     * 校验空参数
     *
     * @param productionDay 排产日
     * @param groupName     分组信息
     * @return
     */
    private boolean isCheckEmpty(Integer productionDay, String groupName) {
        if (StringUtils.isBlank(groupName) || null == productionDay) {
            return true;
        }
        return CollectionUtils.isEmpty(tireDrumInfoMap);
    }

}
