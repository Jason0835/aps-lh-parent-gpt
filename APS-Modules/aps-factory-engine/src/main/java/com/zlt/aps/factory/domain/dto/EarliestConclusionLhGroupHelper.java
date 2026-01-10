package com.zlt.aps.factory.domain.dto;

import lombok.Getter;

import java.io.Serializable;
import java.util.Set;

/**
 * 构建最终收尾硫化组信息对象
 *
 * @author ZLT
 * @date 20251231
 */
@Getter
public class EarliestConclusionLhGroupHelper implements Serializable {
    /**
     * 前规格物料描述
     */
    private String beforeMaterialDesc;
    /**
     * 前规格物料编码
     */
    private String beforeMaterialCode;
    /**
     * 前规格排产量
     */
    private Integer beforeProductionQty;
    /**
     * 收尾日-即下一个Sku上机日
     */
    private Integer closingDay;
    /**
     * 排产结束日
     */
    private Integer endDay;
    /**
     * 使用的模具
     */
    private Set<String> usedMouldSet;

    /**
     * 构造函数
     *
     * @param beforeMaterialDesc  前Sku物料描述
     * @param beforeMaterialCode  前Sku物料编码
     * @param beforeProductionQty 前Sku排产量
     * @param closingDay          收尾日
     * @param endDay              排产结束日
     */
    public EarliestConclusionLhGroupHelper(String beforeMaterialDesc, String beforeMaterialCode, Integer beforeProductionQty, Integer closingDay, Integer endDay, Set<String> usedMouldSet) {
        this.beforeMaterialDesc = beforeMaterialDesc;
        this.beforeMaterialCode = beforeMaterialCode;
        this.beforeProductionQty = beforeProductionQty;
        this.closingDay = closingDay;
        this.endDay = endDay;
        this.usedMouldSet = usedMouldSet;
    }

    /**
     * 更新可排产时间范围
     *
     * @param closingDay 新收尾日
     * @param endDay     新排产结束日
     */
    public void updateProductionDateRange(Integer closingDay, Integer endDay) {
        this.closingDay = closingDay;
        this.endDay = endDay;
    }
}
