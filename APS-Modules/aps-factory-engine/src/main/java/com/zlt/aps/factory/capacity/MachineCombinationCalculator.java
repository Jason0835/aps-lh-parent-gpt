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
     * X - 减模总数
     */
    private int totalMolds;

    /**
     * Z - 不能与增模组合的减模数
     */
    private int noIncreaseMolds;

    /**
     * W - 不能与换活字块组合的减模数
     */
    private int noChangeMolds;

    /**
     * Y - 增模数
     */
    private int increaseMolds;

    /**
     * M - 换活字块数
     */
    private int changeBlocks;



    // 减模分类统计
    /**
     * I类 - 两者都不能匹配
     */
    private int isolatedCount;

    /**
     * 只能匹配增模 (W_only)
     */
    private int onlyIncreaseCount;

    /**
     * 只能匹配换活字块 (Z_only)
     */
    private int onlyChangeCount;

    /**
     * 可以匹配两者
     */
    private int normalCount;

    /**
     * 构造函数
     * @param totalMolds 减模总数X
     * @param noIncreaseMolds 不能与增模组合的减模数Z
     * @param noChangeMolds 不能与换活字块组合的减模数W
     * @param increaseMolds 增模数Y
     * @param changeBlocks 换活字块数M
     */
    public MachineCombinationCalculator(int totalMolds, int noIncreaseMolds, int noChangeMolds,
                         int increaseMolds, int changeBlocks) {
        validateInput(totalMolds, noIncreaseMolds, noChangeMolds, increaseMolds, changeBlocks);

        this.totalMolds = totalMolds;
        this.noIncreaseMolds = noIncreaseMolds;
        this.noChangeMolds = noChangeMolds;
        this.increaseMolds = increaseMolds;
        this.changeBlocks = changeBlocks;

        classifyMolds();
    }

    /**
     * 验证输入参数
     */
    private void validateInput(int totalMolds, int noIncreaseMolds, int noChangeMolds,
                               int increaseMolds, int changeBlocks) {
        if (totalMolds < 0 || noIncreaseMolds < 0 || noChangeMolds < 0 ||
                increaseMolds < 0 || changeBlocks < 0) {
            throw new IllegalArgumentException("所有参数必须为非负整数");
        }

        if (noIncreaseMolds > totalMolds) {
            throw new IllegalArgumentException("不能与增模组合的减模数不能超过减模总数");
        }

        if (noChangeMolds > totalMolds) {
            throw new IllegalArgumentException("不能与换活字块组合的减模数不能超过减模总数");
        }

        // Z和W的交集不能超过两者各自的最小值
        int intersection = Math.max(0, noIncreaseMolds + noChangeMolds - totalMolds);
        if (intersection > Math.min(noIncreaseMolds, noChangeMolds)) {
            throw new IllegalArgumentException("参数不合法：Z和W的交集计算错误");
        }
    }

    /**
     * 对减模进行分类
     */
    private void classifyMolds() {
        // 计算交集I（两者都不能匹配的减模）
        isolatedCount = Math.max(0, noIncreaseMolds + noChangeMolds - totalMolds);

        // 计算只能匹配增模的数量 (W_only)
        onlyIncreaseCount = noChangeMolds - isolatedCount;

        // 计算只能匹配换活字块的数量 (Z_only)
        onlyChangeCount = noIncreaseMolds - isolatedCount;

        // 计算可以匹配两者的正常减模
        normalCount = totalMolds - (onlyIncreaseCount + onlyChangeCount + isolatedCount);

        // 验证分类结果
        if (onlyIncreaseCount < 0 || onlyChangeCount < 0 || normalCount < 0) {
            throw new IllegalStateException("分类计算错误，请检查输入参数");
        }
    }

    /**
     * 获取减模分类信息
     */
    public Map<DeductMoldType, Integer> getMoldClassification() {
        Map<DeductMoldType, Integer> classification = new EnumMap<>(DeductMoldType.class);
        classification.put(DeductMoldType.NORMAL, normalCount);
        classification.put(DeductMoldType.ONLY_INCREASE, onlyIncreaseCount);
        classification.put(DeductMoldType.ONLY_CHANGE, onlyChangeCount);
        classification.put(DeductMoldType.ISOLATED, isolatedCount);
        return classification;
    }

    /**
     * 计算最小机台数
     * @return 计算结果
     */
    public MachineResultVo calculate() {
        // 第一步：处理只能匹配换活字块的减模 (Z_only)
        int s1 = Math.min(onlyChangeCount, changeBlocks);
        int remainingChanges = changeBlocks - s1;
        int remainingOnlyChange = onlyChangeCount - s1;

        // 第二步：处理只能匹配增模的减模 (W_only)
        int s2 = Math.min(onlyIncreaseCount, increaseMolds);
        int remainingIncreases = increaseMolds - s2;
        int remainingOnlyIncrease = onlyIncreaseCount - s2;

        // 第三步：处理正常减模
        // 正常减模可以匹配剩余的增模和换活字块
        int availableResources = remainingIncreases + remainingChanges;
        int s3_s4 = Math.min(normalCount, availableResources);

        // 分配正常减模到两种资源，尽量先匹配一种类型
        int s3 = Math.min(normalCount, remainingIncreases);  // 匹配增模
        int s4 = Math.min(normalCount - s3, remainingChanges); // 匹配换活字块

        // 如果还有剩余匹配能力，调整分配
        if (s3 + s4 < s3_s4) {
            // 可以增加s3或s4
            int additional = s3_s4 - (s3 + s4);
            if (remainingIncreases - s3 >= additional) {
                s3 += additional;
            } else {
                int fromIncrease = remainingIncreases - s3;
                s3 += fromIncrease;
                s4 += (additional - fromIncrease);
            }
        }

        // 总匹配对数
        int totalMatches = s1 + s2 + s3 + s4;

        // 计算单独机台数
        // 未匹配的减模
        int unmatchedMolds = totalMolds - totalMatches;

        // 未匹配的增模
        int unmatchedIncreases = increaseMolds - (s2 + s3);

        // 未匹配的换活字块
        int unmatchedChanges = changeBlocks - (s1 + s4);

        // 总机台数 = 匹配对数 + 未匹配的减模 + 未匹配的增模 + 未匹配的换活字块
        int totalMachines = totalMatches + unmatchedMolds + unmatchedIncreases + unmatchedChanges;

        return new MachineResultVo(totalMachines, totalMatches, unmatchedMolds,
                unmatchedIncreases, unmatchedChanges);
    }

    /**
     * 测试用例执行方法
     */
    private static void testCase(String title, int x, int z, int w, int y, int m) {
        System.out.println("\n" + title);
        System.out.printf("输入参数:\n");
        System.out.printf("  减模总数(X): %d\n", x);
        System.out.printf("  不能与增模组合的减模数(Z): %d\n", z);
        System.out.printf("  不能与换活字块组合的减模数(W): %d\n", w);
        System.out.printf("  增模数(Y): %d\n", y);
        System.out.printf("  换活字块数(M): %d\n", m);

        try {
            MachineCombinationCalculator calculator = new MachineCombinationCalculator(x, z, w, y, m);

            // 显示减模分类
            Map<DeductMoldType, Integer> classification = calculator.getMoldClassification();
            System.out.println("\n减模分类:");
            System.out.printf("  正常减模(可匹配两者): %d\n", classification.get(DeductMoldType.NORMAL));
            System.out.printf("  只能匹配增模: %d\n", classification.get(DeductMoldType.ONLY_INCREASE));
            System.out.printf("  只能匹配换活字块: %d\n", classification.get(DeductMoldType.ONLY_CHANGE));
            System.out.printf("  不能匹配任何资源: %d\n", classification.get(DeductMoldType.ISOLATED));

            // 计算结果
            MachineResultVo result = calculator.calculate();
            System.out.println("\n计算结果:");
            System.out.println(result);

        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
        }
    }

    /**
     * 使用示例和测试
     */
    public static void main(String[] args) {
        System.out.println("=== 轮胎APS组合组件测试 ===\n");

        // 测试用例1：基本示例
        testCase("测试用例1：基本示例",
                10, 3, 2, 8, 6);

        // 测试用例2：Z和W有重叠
        testCase("测试用例2：Z和W有重叠",
                15, 5, 7, 10, 8);

        // 测试用例3：资源充足
        testCase("测试用例3：资源充足",
                10, 2, 3, 15, 12);

        // 测试用例4：减模过多
        testCase("测试用例4：减模过多",
                20, 5, 6, 8, 7);

        // 测试用例5：完全匹配
        testCase("测试用例5：完全匹配",
                10, 2, 2, 8, 8);

        // 用户自定义测试
        System.out.println("\n=== 自定义测试 ===");
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.print("请输入减模总数(X): ");
            int x = scanner.nextInt();

            System.out.print("请输入不能与增模组合的减模数(Z): ");
            int z = scanner.nextInt();

            System.out.print("请输入不能与换活字块组合的减模数(W): ");
            int w = scanner.nextInt();

            System.out.print("请输入增模数(Y): ");
            int y = scanner.nextInt();

            System.out.print("请输入换活字块数(M): ");
            int m = scanner.nextInt();

            testCase("自定义测试", x, z, w, y, m);
        } catch (Exception e) {
            System.out.println("输入错误: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}