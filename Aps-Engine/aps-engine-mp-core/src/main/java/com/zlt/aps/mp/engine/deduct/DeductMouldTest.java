package com.zlt.aps.mp.engine.deduct;

import com.zlt.aps.mp.api.domain.deduct.DailyScheduleVo;
import com.zlt.aps.mp.api.domain.deduct.DeductMouldVo;

import java.time.LocalDate;
import java.util.List;

public class DeductMouldTest {

    // ==================== 测试示例 ====================

    public static void main(String[] args) {
        // 创建测试数据
        DeductMouldVo deductMouldVo = new DeductMouldVo();
        deductMouldVo.setMaterialCode("SKU001");
        deductMouldVo.setTotalQty(464);
        deductMouldVo.setRemainingQty(deductMouldVo.getTotalQty());
        deductMouldVo.setMachinesAssigned(3);
        deductMouldVo.setDailyOutputPerMachine(46);
        deductMouldVo.setStartDate(1);
        deductMouldVo.setDeadline(7);

        /*Set<Integer> shutDownDaySet = new HashSet<>();
        shutDownDaySet.add(5);
        deductMouldVo.setShutDownDaySet(shutDownDaySet);

        Set<Integer> productionStartDaySet = new HashSet<>();
        productionStartDaySet.add(6);
        deductMouldVo.setProductionStartDaySet(productionStartDaySet);*/
        // 执行排产
        System.out.println("=== 轮胎APS降模排产开始 ===");
        System.out.println("开始日期: " + LocalDate.now());
        System.out.println("=========================\n");

        DeductMouldScheduler scheduler = new DeductMouldScheduler();
        List<DailyScheduleVo> schedules = scheduler.scheduleProduction(deductMouldVo);

        // 输出排产结果
        for (int i = 0; i < schedules.size(); i++) {
            System.out.println("【第" + (i+1) + "天】");
            System.out.println(schedules.get(i));
            System.out.println("---");

            // 只显示前7天详细排产
            if (i >= 6) {
                System.out.println("... (后续排产省略)");
                break;
            }
        }
    }
}
