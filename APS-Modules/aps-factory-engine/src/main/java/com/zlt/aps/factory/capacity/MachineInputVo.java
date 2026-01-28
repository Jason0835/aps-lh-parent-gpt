package com.zlt.aps.factory.capacity;

/**
 * 减模/增模组合-机台信息输入
 */
public class MachineInputVo {

    /**
     * X: 减模总数
     */
    private int totalReduceMolds;
    /**
     * Z: 不让增模组合的减模数
     */
    private int cannotAddMolds;
    /**
     * Y: 增模总数
     */
    private int totalAddMolds;
    /**
     * M: 换活字块总数
     */
    private int totalCharacterBlocks;

    public MachineInputVo(int totalReduceMolds, int cannotAddMolds,
                          int totalAddMolds, int totalCharacterBlocks) {
        validateInput(totalReduceMolds, cannotAddMolds, totalAddMolds, totalCharacterBlocks);

        this.totalReduceMolds = totalReduceMolds;
        this.cannotAddMolds = cannotAddMolds;
        this.totalAddMolds = totalAddMolds;
        this.totalCharacterBlocks = totalCharacterBlocks;
    }

    private void validateInput(int x, int z, int y, int m) {
        if (x < 0 || z < 0 || y < 0 || m < 0) {
            throw new IllegalArgumentException("输入参数不能为负数");
        }
        if (z > x) {
            throw new IllegalArgumentException("不让增模组合的减模数不能大于减模总数");
        }
    }

    // Getters
    public int getTotalReduceMolds() { return totalReduceMolds; }
    public int getCannotAddMolds() { return cannotAddMolds; }
    public int getTotalAddMolds() { return totalAddMolds; }
    public int getTotalCharacterBlocks() { return totalCharacterBlocks; }
}
