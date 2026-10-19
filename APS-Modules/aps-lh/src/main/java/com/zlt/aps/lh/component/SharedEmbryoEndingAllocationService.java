package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * 共用胎胚收尾的独立数量分配，不读取普通余量收尾或时间下机决定。
 *
 * @author APS
 * @date 2026/10/19
 */
@Component
public class SharedEmbryoEndingAllocationService {

    /**
     * 按原始硫化余量稳定排序，前序向下取偶，末项接收全部尾差。
     * 全零余量由既有唯一续作承接规则处理，本方法不创建平均分配兜底。
     *
     * @param stock 原始胎胚库存
     * @param skus 已按物料及状态去重的有效SKU
     * @return 按分摊顺序保存的SKU额度，余量合计为零时为空
     */
    public Map<String, Integer> allocateSkus(int stock, List<SkuScheduleDTO> skus) {
        List<SkuScheduleDTO> ordered = new ArrayList<>(skus);
        ordered.sort(Comparator.comparingInt((SkuScheduleDTO sku) -> Math.max(0, sku.getSurplusQty()))
                .thenComparing(SkuScheduleDTO::getMaterialCode)
                .thenComparing(sku -> sku.getProductStatus() == null ? "" : sku.getProductStatus()));
        long totalSurplus = ordered.stream().mapToLong(sku -> Math.max(0, sku.getSurplusQty())).sum();
        Map<String, Integer> quotas = new LinkedHashMap<>(ordered.size());
        if (totalSurplus == 0) {
            return quotas;
        }
        int budget = Math.max(0, stock);
        int allocated = 0;
        for (int index = 0; index < ordered.size(); index++) {
            SkuScheduleDTO sku = ordered.get(index);
            int quantity = index == ordered.size() - 1 ? budget - allocated
                    : (int) ((long) budget * Math.max(0, sku.getSurplusQty()) / totalSurplus / 2 * 2);
            quotas.put(MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus()), quantity);
            allocated += quantity;
        }
        return quotas;
    }

    /**
     * 在真实时间轴上求最大可排量和最早整组完工时刻。
     * 胎胚硬目标普通机台单位为1，单控L/R整机单位为2，因此可用容量与奇偶即可判定可行性，
     * 无需按库存逐条枚举或构建数量组合表；时间二分过程只读容量画像。
     *
     * @param profiles 原机物理容量画像
     * @param target SKU尚未消费的固定额度
     * @param start 窗口起点
     * @param end 窗口终点
     * @return 按物理机台编码排序的实际任务量
     * @throws IllegalArgumentException 数量单位不属于胎胚硬目标支持的1或2时抛出
     */
    public Map<String, Integer> allocateMachines(List<MachineCapacity> profiles,
            int target, Date start, Date end) {
        List<MachineCapacity> ordered = new ArrayList<>(profiles);
        ordered.sort(Comparator.comparing(profile -> profile.machineCode));
        for (MachineCapacity profile : ordered) {
            if (profile.multiple != 1 && profile.multiple != 2) {
                throw new IllegalArgumentException("胎胚收尾物理机台数量单位必须为1或2");
            }
        }
        int schedulable = this.maximumSchedulable(ordered, end, Math.max(0, target));
        long low = start.getTime();
        long high = end.getTime();
        while (schedulable > 0 && low < high) {
            long middle = low + (high - low) / 2;
            if (this.maximumSchedulable(ordered, new Date(middle), schedulable) == schedulable) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        Date completion = new Date(low);
        Map<String, Integer> capacities = new LinkedHashMap<>(ordered.size());
        long singleCapacity = 0;
        for (MachineCapacity profile : ordered) {
            int capacity = schedulable == 0 ? 0 : profile.quantityUntil(completion);
            capacities.put(profile.machineCode, capacity);
            if (profile.multiple == 1) {
                singleCapacity += capacity;
            }
        }
        int singleBudget = (int) Math.min(singleCapacity, schedulable);
        if ((schedulable - singleBudget) % 2 != 0) {
            singleBudget--;
        }
        int pairBudget = schedulable - singleBudget;
        Map<String, Integer> allocated = new LinkedHashMap<>(ordered.size());
        for (MachineCapacity profile : ordered) {
            String machineCode = profile.machineCode;
            int quantity = Math.min(capacities.get(machineCode), profile.multiple == 1 ? singleBudget : pairBudget);
            allocated.put(machineCode, quantity);
            if (profile.multiple == 1) {
                singleBudget -= quantity;
            } else {
                pairBudget -= quantity;
            }
        }
        return allocated;
    }

    /**
     * 汇总给定时刻前可完成的量，并保护只有L/R整机时的奇数缺口。
     *
     * @param profiles 已校验数量单位的物理机台
     * @param deadline 共同生产完成时刻
     * @param target 待完成预算
     * @return 不超过预算且能够合法组合的最大数量
     */
    private int maximumSchedulable(List<MachineCapacity> profiles, Date deadline, int target) {
        long capacity = 0;
        boolean singleAvailable = false;
        for (MachineCapacity profile : profiles) {
            int quantity = profile.quantityUntil(deadline);
            capacity += quantity;
            singleAvailable |= profile.multiple == 1 && quantity > 0;
        }
        int quantity = (int) Math.min(capacity, target);
        return singleAvailable ? quantity : quantity / 2 * 2;
    }

    /**
     * 物理机台的只读容量入口，不持有可变排程结果或普通下机决定。
     *
     * @author APS
     * @date 2026/10/19
     */
    public static final class MachineCapacity {
        /**
         * 已按冻结模式归一化的生产单元编码，整机合并L/R，独立单侧保留侧别。
         */
        private final String machineCode;
        /**
         * 数量单位，普通胎胚硬目标为1，L/R整机为2。
         */
        private final int multiple;
        /**
         * 读取指定绝对时刻前最大合法产量的函数。
         */
        private final ToIntFunction<Date> capacityUntil;

        /**
         * 构造无副作用的容量入口，实际时间轴由原产能内核提供。
         *
         * @param machineCode 物理机台编码
         * @param multiple 数量单位
         * @param capacityUntil 指定时刻前的最大合法连续产量
         */
        public MachineCapacity(String machineCode, int multiple, ToIntFunction<Date> capacityUntil) {
            this.machineCode = machineCode;
            this.multiple = multiple;
            this.capacityUntil = capacityUntil;
        }

        /**
         * 读取共同完成时刻前的合法产量，不修改任何数量或资源状态。
         *
         * @param deadline 共同完成时刻
         * @return 本物理机台在此时刻前的最大合法数量
         */
        private int quantityUntil(Date deadline) {
            return capacityUntil.applyAsInt(deadline);
        }
    }
}
