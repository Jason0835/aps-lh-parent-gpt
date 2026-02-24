package com.zlt.aps.mp.engine.check;

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
            return true;
        }
        if (newOnLineDay <=lastCloseDay){
            return true;
        }
        int diffDays = newOnLineDay - lastCloseDay;
        return diffDays > skuSecondOnlineDays;
    }
}
