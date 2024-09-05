package com.zlt.aps.lh.engine;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.engine.domain.LhEngineScheduleResult;
import com.zlt.aps.lh.engine.service.LhEngineMoldChangePlanService;
import com.zlt.aps.lh.engine.service.LhEngineService;
import com.zlt.aps.lh.engine.task.LhEngineNewAutoScheduleTask;
import com.zlt.aps.lh.engine.task.MoldChangePlanTask;
import com.zlt.aps.lh.engine.util.LhEngineScheduleUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;


@SpringBootTest
class ApsEngineLhApplicationTests {

    @Autowired
    private LhEngineMoldChangePlanService lhEngineMoldChangePlanService;

    @Autowired
    private MoldChangePlanTask moldChangePlanTask;

    @Autowired
    private LhEngineService lhEngineService;

    @Autowired
    private LhEngineNewAutoScheduleTask lhEngineNewAutoScheduleTask;

    /**
     * 单元测试模具变动单
     */
    @Test
    public void testMoldChangePlan(){
       /* List<LhEngineMoldChangePlan> planList=this.lhEngineMoldChangePlanService.selectLhEngineMoldChangePlanList(new LhEngineMoldChangePlan());
        if(StringUtils.isNotEmpty(planList)){
            for (LhEngineMoldChangePlan plan:planList){
                System.out.println(plan.getMoldBatchNo());
            }
        }*/
//       String cxBatchNo=moldChangePlanTask.preChangePlanCheck("2021-06-23");

       String msg= moldChangePlanTask.preChangePlanCheck("2021-12-05");
       System.out.printf("msg:"+msg);
        if(StringUtils.isEmpty(msg)){
            moldChangePlanTask.moldChangePlanTask("2021-12-05");
        }
//        System.out.println("获取到的批次号："+cxBatchNo);
    }

    /**
     * 单元测试硫化自动排程
     */
    @Test
    public  void  autoSchedule(){
        Date date= DateUtils.parseDate("2021-12-07");
        lhEngineService.autoLhSchedule(date);
    }

    /**
     * 单元测试硫化插单
     */
    @Test
    public void insertScheduleResult(){
        LhScheduleResultDto lhScheduleResultDto=new LhScheduleResultDto();
        lhScheduleResultDto.setLhMachineCode("D02");
        lhScheduleResultDto.setSapCode("123");
        lhScheduleResultDto.setStockArea("T1");
        lhScheduleResultDto.setClass1PlanQty(200);
        lhScheduleResultDto.setScheduleDate(DateUtils.parseDate("2021-06-23"));
        ValidateResult result=lhEngineService.inertLhScheduleResultPreCheck(lhScheduleResultDto);
        System.out.println(result.getMsg());
        if(result.isSuccess()){
            lhEngineService.insertLhScheduleOrder(lhScheduleResultDto);
        }
    }

    @Test
    public void testTimeAdd(){
       /*String dateStr="2021-09-15 00:00:00";
       Date date =DateUtils.parseDate(dateStr);
        Calendar calendar=Calendar.getInstance();
        calendar.setTime(date);
        int index=5;
        while(index>0){
            index--;
            calendar.add(Calendar.HOUR,1);
            Date afterDate=calendar.getTime();
            System.out.println("增加一小时后："+ DateUtil.formatDatetime(afterDate));
        }
        Date lastDate=calendar.getTime();
        System.out.println("最后时间："+ DateUtil.formatDatetime(lastDate));*/

        System.out.println(13%8);

    }

    /**
     * 单元测试硫化自动排程
     */
    @Test
    public  void  newAutoSchedule(){
        lhEngineNewAutoScheduleTask.autoSchedule("2022-06-22");
    }

    @Test
    public void testTimeDiff(){
        Date startDate=DateUtils.parseDate("2022-06-01 12:10:00");
        Date endDate=DateUtils.parseDate("2022-06-02 12:00:00");
        System.out.println(LhEngineScheduleUtils.diffDate(startDate,endDate, LhEngineScheduleUtils.HOUR));
    }

    @Test
    public void testGroupBy(){
        List<LhEngineScheduleResult> list=new ArrayList<>();
        Date date=DateUtils.parseDate("2022-06-02 12:00:00");
        for(int i=0;i<10;i++){
            LhEngineScheduleResult lhEngineScheduleResult=new LhEngineScheduleResult();
            lhEngineScheduleResult.setPlanSort(i);
            if(i>2&&i<6){
                lhEngineScheduleResult.setChangeMoldTime(date);
            }else if(i>=6){

                lhEngineScheduleResult.setChangeMoldTime( DateUtils.addSeconds(date,10));
            }
            list.add(lhEngineScheduleResult);
        }
        /*Map<Date, List<LhEngineScheduleResult>> changeMoldTimeMap = list.stream()
                                                                        .sorted(Comparator.comparing(LhEngineScheduleResult::getChangeMoldTime,Comparator.nullsFirst(Date::compareTo)))
                                                                            .collect(LhEngineScheduleUtils.groupByWithNullKeys(LhEngineScheduleResult::getChangeMoldTime));*/
        list.sort(Comparator.comparing(LhEngineScheduleResult::getChangeMoldTime,Comparator.nullsFirst(Date::compareTo)).reversed());
        Map<Date, List<LhEngineScheduleResult>> collectPlan = list.stream()
                                                                      .filter(item -> item.getChangeMoldTime()!=null)
                                                                      .sorted(Comparator.comparing(LhEngineScheduleResult::getChangeMoldTime).reversed())
                                                                      .collect(Collectors.groupingBy(LhEngineScheduleResult::getChangeMoldTime, LinkedHashMap::new,Collectors.toList()));


        for(Map.Entry<Date, List<LhEngineScheduleResult>> entry:collectPlan.entrySet()){
            System.out.println(entry.getKey());
        }
    }


}
