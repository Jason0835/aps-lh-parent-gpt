package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.mp.engine.daylimit.MouldAllocationInfoVo;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 不同分组，同主花纹的模具分配信息对象
 * 用以辅助判断续作Sku模具分配比例调整
 *
 * @author ZLT
 * @date 20260420
 */
@Data
public class DifferentGroupMainPatternAllocationInfoHelper implements Serializable {
    /**
     * 主花纹
     */
    private String mainPattern;
    /**
     * 不同分组的模具分配信息
     */
    private Map<String, MouldAllocationInfoVo> groupNameAllocationInfoMap;
    /**
     * 不同分组的Sku模具使用信息
     */
    private Map<String, Map<String, SkuDayUsedMoldInfoHelper>> groupNameProductionInfoMap;

}
