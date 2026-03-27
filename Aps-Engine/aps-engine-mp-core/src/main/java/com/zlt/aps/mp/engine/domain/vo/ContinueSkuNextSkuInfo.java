package com.zlt.aps.mp.engine.domain.vo;

import com.zlt.aps.mp.engine.daylimit.BeforeSkuProductionInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

/**
 * 续作Sku下一个排产Sku信息对象
 * 辅助传递使用
 *
 * @author ZLT
 * @date 20260327
 */
@Slf4j
@Getter
public class ContinueSkuNextSkuInfo implements Serializable {
    /**
     * 选中的Sku
     */
    private String materialDesc;
    /**
     * 选中的模具
     */
    private List<ProductionMouldInfoVo> selectedMouldList;
    /**
     * 前Sku信息
     */
    private BeforeSkuProductionInfo lhBeforeSkuInfo;

    /**
     * 构造函数
     *
     * @param materialDesc
     * @param selectedMouldList
     * @param lhBeforeSkuInfo
     */
    public ContinueSkuNextSkuInfo(String materialDesc, List<ProductionMouldInfoVo> selectedMouldList, BeforeSkuProductionInfo lhBeforeSkuInfo) {
        this.materialDesc = materialDesc;
        this.selectedMouldList = selectedMouldList;
        this.lhBeforeSkuInfo = lhBeforeSkuInfo;
    }
}
