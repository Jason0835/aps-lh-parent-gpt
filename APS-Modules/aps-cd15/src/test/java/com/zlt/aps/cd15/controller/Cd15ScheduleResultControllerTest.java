package com.zlt.aps.cd15.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 斜裁排程结果列表控制器测试。
 */
public class Cd15ScheduleResultControllerTest {

    /**
     * 默认列表必须先按中班生产顺序展示，大卷代码只能作为同顺序的稳定兜底。
     */
    @Test
    public void shouldSortClass1ProduceOrderBeforeBigRoll() {
        Cd15ScheduleResultController controller = new Cd15ScheduleResultController();

        assertEquals(
                " MACHINE_CODE ASC,CLASS1_PRODUCE_ORDER IS NULL ASC,"
                        + "CLASS1_PRODUCE_ORDER ASC,BIG_ROLL_CODE ASC,ID ASC",
                controller.getOrderBy());
    }
}
