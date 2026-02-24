package com.zlt.aps.mp.engine.domain.dto;

import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
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
     * 前规格日硫化量(双模)
     */
    private Integer beforeDayMaxQty;
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
     * @param beforeDayMaxQty     前Sku的日硫化量(双模)
     * @param closingDay          收尾日
     * @param endDay              排产结束日
     */
    public EarliestConclusionLhGroupHelper(String beforeMaterialDesc, String beforeMaterialCode, Integer beforeProductionQty, Integer beforeDayMaxQty, Integer closingDay, Integer endDay, Set<String> usedMouldSet) {
        this.beforeMaterialDesc = beforeMaterialDesc;
        this.beforeMaterialCode = beforeMaterialCode;
        this.beforeProductionQty = beforeProductionQty;
        this.beforeDayMaxQty = beforeDayMaxQty;
        this.closingDay = closingDay;
        this.endDay = endDay;
        this.usedMouldSet = usedMouldSet;
    }

    /**
     * 构建空的最早收尾组信息
     * 只有可排产范围
     * [closingDay~endDay]
     *
     * @param conclusionDay 前一Sku的收尾日
     * @param endDay        排产结束日
     * @return
     */
    public static EarliestConclusionLhGroupHelper createEmptyEarliestConclusionLhGroup(Integer conclusionDay, Integer endDay) {
        return new EarliestConclusionLhGroupHelper("", "", BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue(), conclusionDay, endDay, new HashSet<>());
    }

    /**
     * 根据排产的信息，构建最早收尾组信息
     * 排产Sku信息和可排产范围
     * [closingDay~endDay]
     *
     * @param conclusionDay 前一Sku收尾日
     * @param endDay        排产结束日
     * @param previousSku   前一Sku信息
     * @return
     */
    public static EarliestConclusionLhGroupHelper createEarliestConclusionLhGroup(Integer conclusionDay, Integer endDay, SkuDayProductionInfoHelper previousSku, boolean hasRemainder) {
        if (hasRemainder) {
            return new EarliestConclusionLhGroupHelper(previousSku.getMaterialDesc(), previousSku.getMaterialCode(), previousSku.getLastRemainder(), previousSku.getDayLhMachineQty(), conclusionDay, endDay, previousSku.getUsedMouldSet());
        }
        return new EarliestConclusionLhGroupHelper(previousSku.getMaterialDesc(), previousSku.getMaterialCode(), BigDecimal.ZERO.intValue(), previousSku.getDayLhMachineQty(), conclusionDay, endDay, previousSku.getUsedMouldSet());
    }

    /**
     * 转化成CxLhProductionHelper对象，为后面日排产量计算使用
     *
     * @return
     */
    public CxLhProductionHelper transformCxLhGroup() {
        CxLhProductionHelper cxLhGroup = new CxLhProductionHelper();
        cxLhGroup.setMaterialDesc(this.beforeMaterialDesc);
        cxLhGroup.setMaterialCode(this.beforeMaterialCode);
        cxLhGroup.setDayMaxProductionQty(this.beforeDayMaxQty);
        cxLhGroup.setProductionQty(this.beforeProductionQty);
        cxLhGroup.setProductionMouldSet(this.usedMouldSet);
        return cxLhGroup;
    }

    /**
     * 当前硫化组是否需要换模
     *
     * @param addSkuInfo
     * @return
     */
    public boolean isChangeMould(MonthPlanProductionRequirePlanVo addSkuInfo) {
        if (null == addSkuInfo || StringUtils.isEmpty(addSkuInfo.getMaterialDesc())) {
            return false;
        }
        String connectSkuMaterialDesc = addSkuInfo.getMaterialDesc();
        return !connectSkuMaterialDesc.equals(beforeMaterialDesc);
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

    /**
     * 更新排产前Sku信息
     *
     * @param beforeMaterialDesc  前Sku物料描述
     * @param beforeMaterialCode  前Sku编码
     * @param beforeProductionQty 前Sku排产量
     * @param beforeDayMaxQty     前Sku日硫化量
     */
    public void updateBeforeSkuInfo(String beforeMaterialDesc, String beforeMaterialCode, Integer beforeProductionQty, Integer beforeDayMaxQty) {
        this.beforeMaterialDesc = beforeMaterialDesc;
        this.beforeMaterialCode = beforeMaterialCode;
        this.beforeProductionQty = beforeProductionQty;
        this.beforeDayMaxQty = beforeDayMaxQty;
    }
}
