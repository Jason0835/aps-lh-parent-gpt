package com.zlt.aps.mp.engine.capacity;

import java.util.HashMap;
import java.util.Map;

/**
 * 机台组合结果
 */
public class MachineResultVo {

    /**
     * 总机台数
     */
    private int totalMachines;
    /**
     * 成功配对数
     */
    private int matchedPairs;
    /**
     * 单独机台的减模数
     */
    private int isolatedMolds;
    /**
     * 单独机台的增模数
     */
    private int isolatedIncreases;
    /**
     * 单独机台的换活字块数
     */
    private int isolatedChanges;
    /**
     * 详细统计
     */
    private Map<String, Integer> details;

    public MachineResultVo(int totalMachines, int matchedPairs, int isolatedMolds,
                  int isolatedIncreases, int isolatedChanges) {
        this.totalMachines = totalMachines;
        this.matchedPairs = matchedPairs;
        this.isolatedMolds = isolatedMolds;
        this.isolatedIncreases = isolatedIncreases;
        this.isolatedChanges = isolatedChanges;
        this.details = new HashMap<>();
        details.put("配对机台数", matchedPairs);
        details.put("单独减模机台数", isolatedMolds);
        details.put("单独增模机台数", isolatedIncreases);
        details.put("单独换活字块机台数", isolatedChanges);
    }

    // Getters
    public int getTotalMachines() { return totalMachines; }
    public int getMatchedPairs() { return matchedPairs; }
    public int getIsolatedMolds() { return isolatedMolds; }
    public int getIsolatedIncreases() { return isolatedIncreases; }
    public int getIsolatedChanges() { return isolatedChanges; }
    public Map<String, Integer> getDetails() { return details; }

    @Override
    public String toString() {
        return String.format("总机台数: %d\n" +
                        "成功配对数: %d\n" +
                        "单独减模机台: %d\n" +
                        "单独增模机台: %d\n" +
                        "单独换活字块机台: %d",
                totalMachines, matchedPairs, isolatedMolds,
                isolatedIncreases, isolatedChanges);
    }
}
