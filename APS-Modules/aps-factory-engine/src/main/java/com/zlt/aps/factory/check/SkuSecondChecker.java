package com.zlt.aps.factory.check;

/**
 * SKU二次上机检查
 * @author Sandy
 * @date 2026-01-29
 */
public class SkuSecondChecker implements IProductionCheck {

    /**
     * 二次上机间隔天数
     */
    private Integer skuSecondOnlineDays;

    /**
     * 上次收尾日
     */
    private Integer lastCloseDay;

    /**
     * 新的上机日
     */
    private Integer newOnLineDay;

    public SkuSecondChecker(Integer newOnLineDay,Integer lastCloseDay,Integer skuSecondOnlineDays){
        this.newOnLineDay = newOnLineDay;
        this.lastCloseDay = lastCloseDay;
        this.skuSecondOnlineDays = skuSecondOnlineDays;
    }

    @Override
    public boolean doCheck() {
        if (lastCloseDay == null){
            return false;
        }
        int diffDays = newOnLineDay - lastCloseDay;
        return diffDays > skuSecondOnlineDays;
    }
}
