package com.zlt.aps.factory.scheduling;

import com.zlt.aps.factory.domain.dto.DayCapacityLimitHelper;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * 模壳总数信息
     * key=模块标准 : value=模块标准数量
     */
    Map<String, MouldShellBaseInfoVo> mouldShellMap;
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
}
