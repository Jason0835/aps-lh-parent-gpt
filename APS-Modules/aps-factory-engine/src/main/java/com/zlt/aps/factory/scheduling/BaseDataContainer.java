package com.zlt.aps.factory.scheduling;

import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.factory.domain.vo.MouldShellBaseInfoVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
     * key cxMachineCode value 成型机信息
     */
    Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo;
    /**
     * 模具信息
     */
    Map<String, ProductionMouldInfoVo> mouldInfoMap;
    /**
     * Sku与模具关系
     */
    Map<String, List<MonthPlanProductMouldInfoVo>> skuMouldRelationMap;
    /**
     * 模壳总数信息
     */
    Map<String, MouldShellBaseInfoVo> mouldShellMap;
    /**
     * 生胎对应的特殊原材料配置信息
     */
    Map<String, Map<String, BigDecimal>> embryoSpecialMaterialInfoMap;
}
