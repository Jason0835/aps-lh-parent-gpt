package com.zlt.aps.factory.capacity;

import java.util.*;

/**
 * 轮胎APS机台组合算法组件
 * 问题描述：
 * 1. 减模X个，其中Z个不让增模组合
 * 2. 增模Y个
 * 3. 换活字块M个
 * 4. 1个减模 + 1个增模 = 1个机台
 * 5. 1个减模 + 1个换活字块 = 1个机台
 * 6. 剩余减模单独算机台
 * 7. 剩余增模或换活字块单独算机台
 * 求解最终机台数
 */
public class MachineCombinationCalculator {

    /**
     * 核心算法：计算最终机台数
     * 算法逻辑：
     * 1. 先处理可以组合的减模与增模配对
     * 2. 再处理剩余的减模与换活字块配对
     * 3. 最后计算剩余的单独机台
     *
     * 算法步骤伪代码：
     * 1. 计算可增模组合的减模数 = X - Z
     * 2. 减模与增模组合数 = min(可增模组合减模数, Y)
     * 3. 剩余减模(可组合部分) = 可增模组合减模数 - 组合数
     * 4. 剩余增模 = Y - 组合数
     * 5. 可用于换活字块的减模总数 = Z + 剩余减模(可组合部分)
     * 6. 减模与换活字块组合数 = min(可用于换活字块的减模总数, M)
     * 7. 最终剩余减模 = 可用于换活字块的减模总数 - 组合数
     * 8. 最终剩余换活字块 = M - 组合数
     * 9. 总机台数 = 增模组合数 + 换活字块组合数 + 剩余减模 + 剩余增模 + 剩余换活字块
     */
    public static MachineResultVo calculateMachines(MachineInputVo input) {
        StringBuilder steps = new StringBuilder();
        steps.append("开始机台组合计算...\n");
        steps.append(String.format("输入参数: 减模X=%d, 不让增模Z=%d, 增模Y=%d, 换活字块M=%d\n",
                input.getTotalReduceMolds(), input.getCannotAddMolds(),
                input.getTotalAddMolds(), input.getTotalCharacterBlocks()));

        // 步骤1: 计算可以用于增模组合的减模数量
        int availableForAddCombine = input.getTotalReduceMolds() - input.getCannotAddMolds();
        steps.append(String.format("步骤1: 可以用于增模组合的减模数 = %d - %d = %d\n",
                input.getTotalReduceMolds(), input.getCannotAddMolds(), availableForAddCombine));

        // 步骤2: 减模与增模组合
        int addCombineCount = Math.min(availableForAddCombine, input.getTotalAddMolds());
        int remainingReduceAfterAdd = availableForAddCombine - addCombineCount;
        int remainingAdd = input.getTotalAddMolds() - addCombineCount;

        steps.append(String.format("步骤2: 减模与增模组合数 = min(%d, %d) = %d\n",
                availableForAddCombine, input.getTotalAddMolds(), addCombineCount));
        steps.append(String.format("      组合后剩余减模(可组合部分) = %d\n", remainingReduceAfterAdd));
        steps.append(String.format("      组合后剩余增模 = %d\n", remainingAdd));

        // 步骤3: 所有减模（包括不让增模的）与换活字块组合
        // 可用的减模总数 = 不让增模的减模 + 增模组合后剩余的减模
        int totalReduceAvailable = input.getCannotAddMolds() + remainingReduceAfterAdd;
        steps.append(String.format("步骤3: 可用于换活字块组合的减模总数 = %d + %d = %d\n",
                input.getCannotAddMolds(), remainingReduceAfterAdd, totalReduceAvailable));

        int characterCombineCount = Math.min(totalReduceAvailable, input.getTotalCharacterBlocks());
        int remainingReduceAfterCharacter = totalReduceAvailable - characterCombineCount;
        int remainingCharacter = input.getTotalCharacterBlocks() - characterCombineCount;

        steps.append(String.format("      减模与换活字块组合数 = min(%d, %d) = %d\n",
                totalReduceAvailable, input.getTotalCharacterBlocks(), characterCombineCount));
        steps.append(String.format("      组合后剩余减模 = %d\n", remainingReduceAfterCharacter));
        steps.append(String.format("      组合后剩余换活字块 = %d\n", remainingCharacter));

        // 步骤4: 计算单独机台
        int remainingReduceMachines = remainingReduceAfterCharacter;
        int remainingAddMachines = remainingAdd;
        int remainingCharacterMachines = remainingCharacter;

        steps.append("步骤4: 计算剩余单独机台\n");
        steps.append(String.format("      剩余减模机台数 = %d\n", remainingReduceMachines));
        steps.append(String.format("      剩余增模机台数 = %d\n", remainingAddMachines));
        steps.append(String.format("      剩余换活字块机台数 = %d\n", remainingCharacterMachines));

        // 步骤5: 计算总机台数
        int totalMachines = addCombineCount + characterCombineCount +
                remainingReduceMachines + remainingAddMachines +
                remainingCharacterMachines;

        steps.append(String.format("步骤5: 总机台数 = %d + %d + %d + %d + %d = %d\n",
                addCombineCount, characterCombineCount, remainingReduceMachines,
                remainingAddMachines, remainingCharacterMachines, totalMachines));

        return new MachineResultVo(
                totalMachines, addCombineCount, characterCombineCount,
                remainingReduceMachines, remainingAddMachines, remainingCharacterMachines,
                steps.toString()
        );
    }

    /**
     * 批量计算多个方案
     */
    public static List<MachineResultVo> calculateMultipleScenarios(List<MachineInputVo> inputs) {
        List<MachineResultVo> results = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            try {
                MachineResultVo result = calculateMachines(inputs.get(i));
                results.add(result);
            } catch (Exception e) {
                System.err.println("方案" + (i+1) + "计算失败: " + e.getMessage());
            }
        }
        return results;
    }

    /**
     * 测试主方法
     */
    public static void main(String[] args) {
        System.out.println("=== 轮胎APS机台组合算法测试 ===\n");
        // 批量计算示例
        System.out.println("批量计算示例:");
        List<MachineInputVo> scenarios = Arrays.asList(
                new MachineInputVo(3, 2, 5, 2),
                new MachineInputVo(15, 3, 10, 8),
                new MachineInputVo(20, 5, 12, 10),
                new MachineInputVo(30, 10, 20, 15)
        );

        List<MachineResultVo> batchResults = calculateMultipleScenarios(scenarios);
        for (int i = 0; i < batchResults.size(); i++) {
            System.out.printf("方案%d: %d个机台\n", i+1, batchResults.get(i).getTotalMachines());
        }
    }
}