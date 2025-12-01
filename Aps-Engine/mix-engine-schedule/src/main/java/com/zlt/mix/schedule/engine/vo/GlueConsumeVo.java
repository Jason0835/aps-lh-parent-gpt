package com.zlt.mix.schedule.engine.vo;

import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import lombok.Data;

/**
 * 胶料消耗硫磺辅料VO
 */
@Data
public class GlueConsumeVo {

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 胶料名称
     */
    private String glue;

    /**
     * 密炼区
     */
    private String mixArea;

    /**
     * 胶料白班（8-16）计划消耗硫磺辅料数（车）
     */
    private Double dayPlanQty;

    /**
     * 胶料实际（8-12）点计划消耗硫磺辅料数（车）
     */
    private Double finishQty;

    /**
     * 支领量
     */
    private Double shelfNum;

    /**
     * 机台code
     */
    private String machineCode;

    /**
     * 配方重量
     */
    private Double formulaWeight;

    /**
     * 配方类型
     */
    private String recipeType;

    /**
     * 获取映射字段的的key
     *
     * @return 映射字段的的key
     */
    public String getGlueRecipeMapKey() {
        return GenerageMapKeyUtils.createMapKey(getGlue(), getRecipeType());
    }
}
