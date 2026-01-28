package com.zlt.aps.factory.capacity;

public class MachineResultVo {

    private int totalMachines;                     // 总机台数
    private int addCombineMachines;               // 增模组合机台数
    private int characterCombineMachines;         // 换活字块组合机台数
    private int remainingReduceMachines;          // 剩余减模机台数
    private int remainingAddMachines;             // 剩余增模机台数
    private int remainingCharacterMachines;       // 剩余换活字块机台数
    private String calculationSteps;              // 计算步骤

    public MachineResultVo(int totalMachines, int addCombineMachines, int characterCombineMachines,
                      int remainingReduceMachines, int remainingAddMachines,
                      int remainingCharacterMachines, String calculationSteps) {
        this.totalMachines = totalMachines;
        this.addCombineMachines = addCombineMachines;
        this.characterCombineMachines = characterCombineMachines;
        this.remainingReduceMachines = remainingReduceMachines;
        this.remainingAddMachines = remainingAddMachines;
        this.remainingCharacterMachines = remainingCharacterMachines;
        this.calculationSteps = calculationSteps;
    }

    @Override
    public String toString() {
        return String.format(
                "总机台数: %d\n" +
                        "增模组合机台: %d\n" +
                        "换活字块组合机台: %d\n" +
                        "剩余减模机台: %d\n" +
                        "剩余增模机台: %d\n" +
                        "剩余换活字块机台: %d\n" +
                        "计算步骤:\n%s",
                totalMachines, addCombineMachines, characterCombineMachines,
                remainingReduceMachines, remainingAddMachines, remainingCharacterMachines,
                calculationSteps
        );
    }

    // Getters
    public int getTotalMachines() { return totalMachines; }
}
