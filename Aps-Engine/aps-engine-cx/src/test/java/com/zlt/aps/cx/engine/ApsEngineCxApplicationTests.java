package com.zlt.aps.cx.engine;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
import com.zlt.aps.common.engine.enums.TireTypeEnums;
import com.zlt.aps.common.engine.planmain.MdmMonthPlanAmountSumService;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.common.engine.service.CxEngineChangeLhMachineService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxScheduleTaskTime;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.*;
import com.zlt.aps.cx.engine.enums.AdjustTypeEnums;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxEngineLastDayScheduleMapper;
import com.zlt.aps.cx.engine.mapper.CxScheduleEngineMapper;
import com.zlt.aps.cx.engine.service.*;
import com.zlt.aps.cx.engine.task.CxAutoScheduleEngine;
import com.zlt.aps.cx.engine.task.CxEngineCommonService;
import com.zlt.aps.cx.engine.task.LastDayScheduleTaskService;
import com.zlt.aps.cx.engine.task.ScheduleCheckService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
class ApsEngineCxApplicationTests {


    @Resource(name="cxScheduleEngineService")
    private CxScheduleEngineService cxScheduleEngineService;

    @Autowired
    private CommonCacheService commonCacheService;

    @Autowired
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;


    @Autowired
    private CxEngineEmbryoMonthPlanSurplusService cxEngineEmbryoMonthPlanSurplusService;

    @Autowired
    private CxEngineMonthPlanSurplusService cxEngineMonthPlanSurplusService;

    @Autowired
    private MdmMonthPlanAmountSumService mdmMonthPlanAmountSumService;

    @Autowired
    private ScheduleCheckService scheduleCheckService;

    @Autowired
    private CxScheduleEngineMapper cxScheduleEngineMapper;

    @Autowired
    private CxEngineGroupMachineListService cxEngineGroupMachineListService;

    @Autowired
    private CxEngineScheduleLimitService cxEngineScheduleLimitService;

    @Autowired
    private CxEngineLastDayScheduleMapper cxEngineLastDayScheduleMapper;

    @Autowired
    private LastDayScheduleTaskService lastDayScheduleTaskService;

    @Autowired
    private CxAutoScheduleEngine cxAutoScheduleEngine;

    @Autowired
    private CxEngineCommonService cxEngineCommonService;

    @Autowired
    private CxEngineLastDaySupplePlanService cxEngineLastDaySupplePlanService;

    @Autowired
    private CxEngineChangeLhMachineService cxEngineChangeLhMachineService;

    @Autowired
    private CxScheduleTaskTimeService cxScheduleTaskTimeService;

    @Autowired
    private CxLhEngineCommonService cxLhEngineCommonService;

    /**
     * 引擎测试方法
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/6/29 14:47
     * @param
     * @return
     */
    @Test
    public void runEngine() throws ParseException, InterruptedException {
        //String dateStr="2022-01-14";
        String dateStr="2022-02-12"; //ll-test
        Date scheduleDate= DateUtils.parseDate(dateStr);
        cxScheduleEngineService.allMachineAutoSchedule(scheduleDate);
        Thread.sleep(6000);
    }

    /**
     * 单机台重排
     * @throws CxScheduleEngineException
     */
    @Test
    public void singleMachineAutoSchedule() throws ParseException, InterruptedException{
        String dateStr="2022-01-07";
        Date scheduleDate= DateUtils.parseDate(dateStr);
        cxScheduleEngineService.singleMachineAutoSchedule("4576",scheduleDate);
        //Thread.sleep(300000);
    }

    /**
     * 自动生成流水号测试
     */
    @Test
    public void testAutoFlowCode(){
        String currentDate = DateUtils.parseDateToStr("yyyyMMdd",new Date());
        Long batchNo = commonCacheService.getIncrementNumber(CxPrefixConstants.SCHEDULE_BATCH_NO_PREFIX +currentDate);
        String cxBatchNo = CxScheduleUtils.getSequence(currentDate,batchNo);
        System.out.println("成型批次号:"+cxBatchNo);

        Long orderNo = commonCacheService.getIncrementNumber(CxPrefixConstants.SCHEDULE_ORDER_NO_PREFIX +currentDate);
        String cxOrderNo = CxScheduleUtils.getSequence(currentDate,orderNo);
        System.out.println("工单号:"+cxOrderNo);

    }

    /**
     * 根据规格描述判断轮胎类型
     */
    @Test
    public void getTireTypeCode(){
        /*String tireSpec="LT265/75R16 10PR CROSSWIND MT 123/120Q AT (HB) ECE CCC PCI";
        String code=TireTypeEnums.getTireTypeCode(tireSpec);
        System.out.println(tireSpec+"=======》轮胎类型为："+code);*/
/*         tireSpec="145/70R12 GREEN-MAX ET 69S LL(HB)ECE-S CCCSWE200OEPCIVCM";
         code= TireTypeEnums.getTireTypeCode(tireSpec);
        System.out.println(tireSpec+"=======》轮胎类型为："+code);

        tireSpec="t145/70R12 GREEN-MAX ET 69S LL(HB)ECE-S CCCSWE200OEPCIVCM";
        code=TireTypeEnums.getTireTypeCode(tireSpec);
        System.out.println(tireSpec+"=======》轮胎类型为："+code);


        tireSpec="205/55RF AM520 91W AT (HB) ECE CCC LRS PCI VCM";
        code=TireTypeEnums.getTireTypeCode(tireSpec);
        System.out.println(tireSpec+"=======》轮胎类型为："+code);

        tireSpec="225/70R15C 8PR GREEN-Max Van﹡ 106/103R LL (HB) ECE-S CCC JL OE PCI";
        code=TireTypeEnums.getTireTypeCode(tireSpec);
        System.out.println(tireSpec+"=======》轮胎类型为："+code);*/

       /* tireSpec="195/70R15LT 12PR R666 109/105R LL (HB) CCC SW N350P-JQ OE PCI trial";
        code=TireTypeEnums.getTireTypeCode(tireSpec);
        System.out.println(tireSpec+"=======》轻卡胎规格轮胎类型为："+code);

        tireSpec="195/70R15C 12PR R666 109/105R LL (HB) CCC SW N350P-JQ OE PCI trial";
        code=TireTypeEnums.getTireTypeCode(tireSpec);
        System.out.println(tireSpec+"=======》轻卡胎规格轮胎类型为："+code);*/

       String tireSpec="175/60R15 81H GREEN-Max HP010/NOVA-FORCE HP";
       String code=TireTypeEnums.getTireTypeCode(tireSpec);
        System.out.println(tireSpec+"=======》轻卡胎规格轮胎类型为："+code);


    }

    @Test
    public void testCalcAllClassAvalableLhShift(){
        CxEngineScheduleResult result=new CxEngineScheduleResult();
        String dayStr="2021-06-23";

        result.setScheduleDate(DateUtils.parseDate(dayStr));
        result.setSapCode("221013753");
        result.setEmbryoCode("EHETB076P");
        CxPlanProductStatus cxPlanProductStatus=new CxPlanProductStatus();
        cxPlanProductStatus.setMonthPlanTotalQty(1000);
        //commonCacheService.calcLeastLhMachineQty(result,cxPlanProductStatus);

    }

    /**
     * 手工插单
     */
    @Test
    public void testInsert() throws  CxScheduleEngineException{
        CxScheduleResult result=new CxScheduleResult();
        String dayStr="2021-12-03";
        result.setScheduleDate(DateUtils.parseDate(dayStr));
        result.setSapCode("202112031021");
        result.setEmbryoCode("22");
        result.setTaskType("0");
        result.setCxMachineCode("67");
        result.setStorageLocation("T1");
        result.setClass1PlanQty(100);
        result.setClass2PlanQty(200);
        result.setBomDataVersion("221");
      /*  result.setClass1PlanQty(350);
        result.setClass2PlanQty(260);
        result.setClass3PlanQty(260);*/
        ValidateResult ajaxResult=cxScheduleEngineService.insertPreCheck(result);
        if(ajaxResult.isSuccess()){
          cxScheduleEngineService.insertTask(result);
        }
        //throw new CxScheduleEngineException("11111");
        //System.out.println(ajaxResult.getMsg());
    }

    /**
     * 转机台测试
     */
    @Test
    public void testChangeMachine(){
        CxScheduleResult result=new CxScheduleResult();
        String dayStr="2021-07-02";
        result.setScheduleDate(DateUtils.parseDate(dayStr));
        result.setId(2341L);
        result.setCxBatchNo("CXPC202107020001");
        result.setOrderNo("Order202107020001");
        result.setSapCode("TEST");
        result.setEmbryoCode("TEST");
        result.setCxMachineCode("H4");
        result.setSpecDimension(18.12);
        ValidateResult ajaxResult=cxScheduleEngineService.changeMachinePreCheck(result,"#CXJT004");
        if(ajaxResult.isSuccess()){
            System.out.println(ajaxResult.getMsg());
            cxScheduleEngineService.changeMachineTask(result,"#CXJT004");
        }
    }

    @Test
    public void testCalc(){
       /* Integer total=6;
        int calc=4;
        double daad=(double)4/6 * 580 ;
        System.out.println(daad);
        Integer result =new BigDecimal(daad).setScale(0,RoundingMode.UP).intValue();
       System.out.println(result);*/
        Integer shiftTime=470;
        Integer brushBagTime=2;
        Double singleLhTime=10.3;
        Double moldNum=6D;
        BigDecimal moldNumDecimal=BigDecimal.valueOf(moldNum);
        BigDecimal singleShiftLhQtyDecimal=BigDecimal.valueOf(((double)shiftTime/(singleLhTime + brushBagTime))).multiply(moldNumDecimal).setScale(1, BigDecimal.ROUND_UP);
        System.out.println("【计算单班硫化量】硫化机*2模计算单条硫化时长："+singleLhTime+"，硫化机模数："+moldNum+"，计算可硫化单班硫化量="+singleShiftLhQtyDecimal.intValue());
    }

    @Test
    public void  testQuota(){
        Integer quota= cxEngineQuotaCommonService.getCxMachineQuota("6060","YHETB216P","E");
        System.out.println("E5机台定额："+quota);
    }

    /**
     * 重新计算各个班次的可硫化班数
     */
    @Test
    public void testReCalcAvalableClassShift(){
        Date date =DateUtils.parseDate("2021-07-10");
        CxScheduleResult cxScheduleResult=new CxScheduleResult();
        cxScheduleResult.setScheduleDate(date);
        cxScheduleResult.setSapCode("221009546");
        cxScheduleResult.setEmbryoCode("YHETB1886");
        cxScheduleResult.setCxMachineCode("L1");
        cxScheduleResult.setClass1PlanQty(280);
        cxScheduleResult.setClass2PlanQty(280);
        cxScheduleResult.setClass3PlanQty(280);
        cxScheduleResult.setClass4PlanQty(280);
        cxScheduleResult.setClass5PlanQty(280);
        cxScheduleEngineService.calcAvaliableClassShift(cxScheduleResult, AdjustTypeEnums.CHANGE_QTY);
    }

    @Test
    public void testCalcAllClassShift(){
        CxEngineScheduleResult cxEngineScheduleResult=new CxEngineScheduleResult();
        cxEngineScheduleResult.setCalcTotalStock(166);
        cxEngineScheduleResult.setSingleShiftLhQty(190);
        cxEngineScheduleResult.setClass3PlannedQty(141);
        cxEngineScheduleResult.setClass1PlanQty(248);
        cxEngineScheduleResult.setClass2PlanQty(248);
        cxEngineScheduleResult.setClass3PlanQty(248);
        cxEngineScheduleResult.setClass4PlanQty(248);
        cxEngineScheduleResult.setClass5PlanQty(248);
        cxEngineScheduleResult.setNewSpecFlag(true);
        CxScheduleUtils.calcAllClassAvailableLhShift(cxEngineScheduleResult);
    }


    @Test
    public void  testRemoveSulplus(){
        String scheduleDateStr="2021-07-26";
        //删除当天外胎插单汇总表数据
        CxEngineMonthPlanSurplus deleteMonthPlanSurplus=new CxEngineMonthPlanSurplus();
        deleteMonthPlanSurplus.setStartTime(scheduleDateStr+" 00:00:00");
        deleteMonthPlanSurplus.setEndTime(scheduleDateStr+" 23:59:59");
       int cls= cxEngineMonthPlanSurplusService.deleteMonthPlanSurplusByDataSource(deleteMonthPlanSurplus);
        System.out.println("删除外胎插单记录数："+cls);
        CxEngineEmbryoMonthPlanSurplus deleteEmbryoMonthPlanSurplus=new CxEngineEmbryoMonthPlanSurplus();
        deleteEmbryoMonthPlanSurplus.setStartTime(scheduleDateStr+" 00:00:00");
        deleteEmbryoMonthPlanSurplus.setEndTime(scheduleDateStr+" 23:59:59");
        cls= cxEngineEmbryoMonthPlanSurplusService.deleteEmbryoMonthPlanSurplusByDataSource(deleteEmbryoMonthPlanSurplus);
        System.out.println("删除胎胚插单记录数："+cls);
        //调用月度汇总全部进行重算
       // mdmMonthPlanAmountSumService.recalculateByApsVersion(monthPlanApsVersion);
    }

    @Test
    public  void  testDateUtil(){
        Date date=DateUtils.parseDate("2021-01-31");
        System.out.println("当前日期：2021-01-31");
        Date nextMonth=DateUtils.addMonths(date,1);
        System.out.println("月份加1："+DateUtils.parseDateToStr("yyyy-MM-dd",nextMonth));
    }

    @Test
    public void  testListToMap(){
        List<CxParamsDto> dtoList=new ArrayList<>();
        CxParamsDto dto=new CxParamsDto();
        dto.setParamCode("CX_PARAMS");
        dto.setParamValue("100");
        dto.setId(100L);
        dtoList.add(dto);
        dto=new CxParamsDto();
        dto.setParamCode("CX_PARAMS1");
        dto.setParamValue("100");
        dto.setId(1001L);
        dtoList.add(dto);
        Map<String,String> dtoMap=dtoList.stream().collect(Collectors.toMap(CxParamsDto::getParamCode,CxParamsDto::getParamValue));
        for(Map.Entry entry:dtoMap.entrySet()){
            System.out.println("key:"+entry.getKey()+",value:"+entry.getValue());
        }
    }


    /**
     * 跑单机台自动排程单元测试
     * @throws InterruptedException
     */
    @Test
    public void runSingleMachineSchedule() throws InterruptedException {
        String dateStr="2021-06-23";
        Date scheduleDate= DateUtils.parseDate(dateStr);
        cxScheduleEngineService.singleMachineAutoSchedule("E6",scheduleDate);
        Thread.sleep(10000);
    }

    @Test
    public void testMdmPlanProd(){
        Map<String,List<MdmMonthProdPlan>> resultMap=scheduleCheckService.getMonthProdPlanMap("APS2021112406");
        if(StringUtils.isNotEmpty(resultMap)){
            for(Map.Entry<String,List<MdmMonthProdPlan>> entry:resultMap.entrySet()){
                System.out.println("胎胚代码："+entry.getKey()+"相同胎胚不同版本的个数有："+entry.getValue().size());
            }
        }
    }

    @Test
    public void testLastDayStock(){
        String date="2021-12-09";
        Date scheduleDate=DateUtils.parseDate(date);
        Date lastDate=DateUtils.addDays(scheduleDate,-1);
        CxEngineScheduleResult condition=new CxEngineScheduleResult();
        condition.setCxScheduleDate(DateUtils.parseDateToStr("yyyyMMdd",lastDate));
        List<CxEngineScheduleResult> lastDayResultList=cxScheduleEngineMapper.selectCxScheduleResultList(condition);
        StringBuilder sb=new StringBuilder();
        commonCacheService.updateLastDayTaskStock(lastDayResultList,scheduleDate,sb,false);
        System.out.println(sb.toString());
    }

    @Test
    public void testCxMachineShift(){
        //成型机台编号投产列表获取
        Map<String,Double> machineProductShiftMap=cxEngineGroupMachineListService.getCxMachineProudctShift();
        if(StringUtils.isNotEmpty(machineProductShiftMap)){
            for(Map.Entry<String,Double> entry: machineProductShiftMap.entrySet()){
                System.out.println("机台编号："+entry.getKey()+".可投产班次："+entry.getValue());
            }
        }
        List<CxEngineScheduleResult> cxEngineScheduleResultList=new ArrayList<>();
        CxEngineScheduleResult nextCxEngineScheduleResult=null;
        CxEngineScheduleResult cxEngineScheduleResult=new CxEngineScheduleResult();
        cxEngineScheduleResult.setCxMachineCode("67");
        cxEngineScheduleResult.setScheduleDate(new Date());
        nextCxEngineScheduleResult=cxEngineScheduleResult;
        cxEngineScheduleResultList.add(nextCxEngineScheduleResult);
        //加载成型排程限制
        Map<String, List<CxEngineScheduleLimit>> scheduleLimitMap=cxEngineScheduleLimitService.getCxScheduleLimitMachineCodeMap();
        commonCacheService.maxLhShiftCount(nextCxEngineScheduleResult,"17",3.67,scheduleLimitMap);
        System.out.println(cxEngineScheduleResultList.get(0));
    }


    @Test
    public void  testLeastPlanQty(){
        String dateStr="2021-12-28";
        Date scheduleDate= DateUtils.parseDate(dateStr);
        CxEngineScheduleResult test=new CxEngineScheduleResult();
        test.setSapCode("221014533");
        test.setEmbryoCode("EHETB383P");
        test.setScheduleDate(scheduleDate);
        commonCacheService.calcLeastLhMachineQtyByMonthRemainQty(test,1611,null);
        System.out.println("最小硫化机数："+test.getMinimumLhMachineComQty());
    }

    /**
     * 最小硫化机需求数
     */
    @Test
    public void testLeastLhMachineQty(){
        String date="2022-01-04";
        CxEngineScheduleResult cxEngineScheduleResult=new CxEngineScheduleResult();
        cxEngineScheduleResult.setSapCode("221026323");
        cxEngineScheduleResult.setEmbryoCode("YHETB1304P");
        cxEngineScheduleResult.setBomDataVersion("B");
        cxEngineScheduleResult.setTotalStock(243);
        cxEngineScheduleResult.setMonthRemainQty(12683);
        cxEngineScheduleResult.setScheduleDate(DateUtils.parseDate(date));
        cxEngineScheduleResult.setLhMachineQty(4D);
        commonCacheService.calcLeastLhMachineQtyByMonthRemainQty(cxEngineScheduleResult,12683,null);
    }

    @Test
    public void testMiddleNightFinishQty(){
        CxMiddleNightFinishQty condition=new CxMiddleNightFinishQty();
        condition.setScheduleDateStr("20210714");
        List<CxMiddleNightFinishQty> list= cxEngineLastDayScheduleMapper.listCxFinish(condition);
        for (CxMiddleNightFinishQty cxMiddleNightFinishQty:list){
            System.out.println(cxMiddleNightFinishQty);
        }
    }

    /**
     * 单元测试前一天三班计划量修正
     */
    @Test
    public void testLastDaySchedule(){
        String dateStr="2022-02-11";
        Date date=DateUtils.parseDate(dateStr);
        cxScheduleEngineService.createSupplePlanTask(date);
    }

    @Test
    public void testPlanSort(){
        String dateStr="2021-12-11";
        Date date=DateUtils.parseDate(dateStr);
        lastDayScheduleTaskService.cxScheduleAutoSupple(date);
    }

    @Test
    public void rePlanSort(){
        String dateStr="2022-02-11";
        Date date=DateUtils.parseDate(dateStr);
        lastDayScheduleTaskService.reCreateLastDaySchedule(date);
    }

    @Test
    public void testNewAutoSchedule(){
        String dateStr="2022-02-12";
        Date date=DateUtils.parseDate(dateStr);
        cxAutoScheduleEngine.autoSchedule(null,date);
    }

    @Test
    public void testEngineCommonService(){
        String dateStr="2022-02-11";
        Date date=DateUtils.parseDate(dateStr);
        Map<String,String> resultMap=cxEngineCommonService.cxMachineInProductSpecMap(date);
        for(Map.Entry entry:resultMap.entrySet()){
            System.out.println("机台编号："+entry.getKey()+",胎胚代码："+entry.getValue());
        }
    }

    @Test
    public void testCloseOutByOrderNo(){
        CxEngineLastDaySupplePlan condition=new CxEngineLastDaySupplePlan();
        condition.setSuppleDateStr("20220211");
        List<CxEngineLastDaySupplePlan> planList=cxEngineLastDaySupplePlanService.selectSupplePlanListByCondition(condition);
        if(StringUtils.isNotEmpty(planList)){
            List<CxEngineScheduleResult> resultList= BeanConverUtil.converList(planList,CxEngineScheduleResult.class);
            //更新生产状态为收尾状态
            cxScheduleEngineMapper.updateProductStatusToCloseOut(resultList);
        }
    }


    public static void main(String[] args){
        List<CxEngineScheduleResult> dataList=new ArrayList<>();
        dataList.add(createResult(1L,"001","GD0001","G1",100,200));
        dataList.add(createResult(2L,"002","GD0002","G2",110,210));
        for (CxEngineScheduleResult cxEngineScheduleResult:
             dataList) {
            System.out.println("修改前：》》》》"+cxEngineScheduleResult.tailInfo());
        }
        CxEngineScheduleResult cx1=getByIndex(dataList,0);
        cx1.setClass1PlanQty(222);
        cx1.setClass2PlanQty(333);
        CxEngineScheduleResult cx2=getByIndex(dataList,1);
        cx2.setClass1PlanQty(444);
        cx2.setClass2PlanQty(555);

        for (CxEngineScheduleResult cxEngineScheduleResult:
                dataList) {
            System.out.println("修改后：》》》》"+cxEngineScheduleResult.tailInfo());
        }
    }

    private static CxEngineScheduleResult getByIndex(List<CxEngineScheduleResult> dataList, int index) {
        return dataList.get(index);
    }

    public static CxEngineScheduleResult createResult(Long id,String batchNo,String orderNo,String machineCode,Integer plan1,int plan2){
        CxEngineScheduleResult result=new CxEngineScheduleResult();
        result.setId(id);
        result.setCxBatchNo(batchNo);
        result.setOrderNo(orderNo);
        result.setCxMachineCode(machineCode);
        result.setClass1PlanQty(plan1);
        result.setClass2PlanQty(plan2);
        return result;
    }

    /**
     * 计算单班硫化量测试
     */
    @Test
    public  void testCalcSingleMoldQty(){
      double shiftTime=480D;
      double singleLhTime=12.8D;
      double brushBagTime=2D;
      double moldNum=10D;
      BigDecimal moldNumDecimal=BigDecimal.valueOf(moldNum);
      Double divisor=singleLhTime + brushBagTime;
      if(Double.valueOf(0).equals(divisor)){
          throw new CxScheduleEngineException("计算异常：除数为0！");
      }
      BigDecimal singleShiftLhQtyDecimal=BigDecimal.valueOf((shiftTime/divisor)).setScale(2, BigDecimal.ROUND_DOWN).multiply(moldNumDecimal);
      System.out.println("保留两位向下取整："+singleShiftLhQtyDecimal.intValue());
        singleShiftLhQtyDecimal=BigDecimal.valueOf(((double)shiftTime/(singleLhTime + brushBagTime))).setScale(0, BigDecimal.ROUND_DOWN).multiply(moldNumDecimal);
        System.out.println("直接向下向下取整："+singleShiftLhQtyDecimal.intValue());
    }

    @Test
    public void testCxChangeLhMachineService(){
        String dateStr="2022-04-14";
        Date scheduleDate=DateUtils.parseDate(dateStr);
        Map<String,String> cxOrderMap=cxEngineChangeLhMachineService.splitCxOrderWithLhMachines(scheduleDate, CxEngineConstants.CHANGE_MACHINE_DATA_SOURCE_SCHEDULE,"CXGD202204140001");
        for(Map.Entry<String,String> entry:cxOrderMap.entrySet()){
            System.out.println(StringUtils.format("成型工单号：【{}】,硫化机台名称：{}",entry.getKey(),entry.getValue()));
        }
   }

    @Test
    public void delCxChangeLhMachineService(){
        String dateStr="2022-04-14";
        Date scheduleDate=DateUtils.parseDate(dateStr);
        cxEngineChangeLhMachineService.deleteChangeLhMachineByScheduleDate(scheduleDate,CxEngineConstants.CHANGE_MACHINE_DATA_SOURCE_SCHEDULE,"");
    }

    @Test
    public void testBuildChangeMachine(){
        String dateStr="2022-04-14";
        Date scheduleDate=DateUtils.parseDate(dateStr);
        cxEngineChangeLhMachineService.buildSuppleCxChangeLhMachine(scheduleDate,CxEngineConstants.CHANGE_MACHINE_DATA_SOURCE_SUPPLE);
    }


    /**
     * 测试成型任务时间基础类代码
     */
    @Test
    public void testCxScheduleTaskTime(){
        String dateStr="2022-02-12";
        Date scheduleDate=DateUtils.parseDate(dateStr);
        CxScheduleTaskTime cxScheduleTaskTime=new CxScheduleTaskTime();
        cxScheduleTaskTime.setScheduleDate(scheduleDate);
        cxScheduleTaskTime.setCxOrderNo("CXGD2022051600001");
        cxScheduleTaskTime.setProductOrder(1);
        cxScheduleTaskTime.setDataSource("0");
        cxScheduleTaskTime.setMachineCode("67");
        cxScheduleTaskTime.setEstimateStartTime(DateUtils.getNowDate());
        cxScheduleTaskTime.setEstimateEndTime(DateUtils.addHours(DateUtils.getNowDate(),3));
        List<CxScheduleTaskTime> list = new ArrayList<>();
        list.add(cxScheduleTaskTime);
        cxScheduleTaskTimeService.batchInsertCxScheduleTaskTime(list);
    }

    @Test
    public void testTime(){
        String dateStr="2022-02-12";
        Date scheduleDate=DateUtils.parseDate(dateStr);
        Date estimateStartTime= DateUtils.addHours(CxScheduleUtils.formatDateByZero(scheduleDate),8);
        System.out.println("时间："+DateUtil.formatDatetime(estimateStartTime));
        estimateStartTime=addHour(estimateStartTime,8);
        System.out.println("之后时间："+DateUtil.formatDatetime(estimateStartTime));
    }

    private Date addHour(Date estimateStartTime, int hour) {
        estimateStartTime= DateUtils.addHours(estimateStartTime,hour);
        return estimateStartTime;
    }

    /**
     * 测试自动匹配硫化机
     */
    @Test
    public void testAutoMachLhMachine(){
        String dateStr="2022-03-22";
        Date scheduleDate=DateUtils.parseDate(dateStr);
        cxLhEngineCommonService.cxScheduleAutoMachLhMachine(scheduleDate,null);
    }


}
