package com.zlt.mix.schedule.engine.materialschedule;

import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.engine.service.decompose.DecomposeEngineService;
import com.zlt.mix.schedule.engine.service.materialschedule.MaterialEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.List;

@SpringBootTest
class MaterialScheduleEngineTests {

    @Resource
    private MaterialEngineService materialEngineService;

    /**
     * 根据终炼胶的汇总计划分解出对应的母炼胶的日计划
     */
    @Test
    public void autoSchedule() throws Exception{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        materialEngineService.autoSchedule(sdf.parse("2022-09-02"), "M4");
    }

    @Test
    public void addSchedule() throws Exception {
        MaterialScheduleResult schedule = new MaterialScheduleResult();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        schedule.setScheduleDate(sdf.parse("2022-07-15"));
        schedule.setMixArea("M2");
        schedule.setMaterialName("HA739-F270-自");
        schedule.setRecipeVersionId("2");  //配方版本号
        schedule.setRecipeType("1");  //配方类型
        schedule.setMidPlanQty(10D);
        schedule.setMidProduceOrder(10);
        schedule.setMachineCode("02022");   //机台code
        List<MaterialScheduleResult> list = materialEngineService.addEngineSchedule(schedule);
        System.out.println(list);
    }
}
