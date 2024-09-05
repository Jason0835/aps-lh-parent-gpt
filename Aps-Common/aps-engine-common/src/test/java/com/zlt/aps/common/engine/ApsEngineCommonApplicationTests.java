package com.zlt.aps.common.engine;


import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.DateUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@SpringBootTest
class ApsEngineCommonApplicationTests {

    @Autowired
    private IncrementService incrementService;

/*    @Autowired
    private CxEngineQuotaSettingService cxEngineQuotaSettingService;

    @Autowired
    private EngineConstructionInfoService engineConstructionInfoService;*/

    @Autowired
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;



    @Test
    public void testQuota() {
        /*Map<String, List<CxEngineQuotaSetting>> quotaSettingMap=cxEngineQuotaSettingService.listCxMachineQuotaSettingMap();
        Map<String, BaseCxConstructionInfo> embryoCodeConstructionMap =this.engineConstructionInfoService.loadConstructionInfo();
        BaseCxConstructionInfo cxInfo=embryoCodeConstructionMap.get("EHETB0269");
        if(cxInfo!=null){
            cxInfo.setCxMachineCode("E05");
            String cxMachineCode=cxInfo.getCxMachineCode();//成型机台编号
            String specDimension=cxInfo.getSpecDimension()==null?"":""+cxInfo.getSpecDimension();//外胎规格尺寸信息
            String carcassBothLayer=cxInfo.getCarcassBothLayer()==null?"":""+cxInfo.getCarcassBothLayer();//胎体布层数
            String reinforce=cxInfo.getReinforce();//是否补强
            String tireType=cxInfo.getTireType();//轮胎类型
            String mapKey= GenerageMapKeyUtils.createMapKey(cxMachineCode,specDimension,carcassBothLayer,reinforce,tireType);
            if(quotaSettingMap.containsKey(mapKey)){
                Integer sectionWidth=cxInfo.getSectionWidth();//断面宽
                List<CxEngineQuotaSetting> quotaSettingList=quotaSettingMap.get(mapKey);
                for (CxEngineQuotaSetting cxEngineQuotaSetting:quotaSettingList){
                    if(sectionWidth>=cxEngineQuotaSetting.getSectionWidthMinimum()&&sectionWidth<=cxEngineQuotaSetting.getSectionWidthMaximum()){
                        System.out.println("最终定额："+cxEngineQuotaSetting.getFinalQuota());
                    }
                }

            }
        }*/

        Integer finalQuota=cxEngineQuotaCommonService.getCxMachineQuota("48","EHETB0850","A");
        System.out.println("获取到的最终定额："+finalQuota);

    }

    @Test
    public void testWorkDate(){
        String dateStr="2021-07-31 15:59:59";
        try {
           Date realDate= DateUtils.parseDate(dateStr,"yyyy-MM-dd HH:mm:ss");
            System.out.println(getWorkDate(realDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取实际工作日
     * @param date
     * @return
     */
    public String getWorkDate(Date date){
        Calendar calendar=Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY,8);
        Date afterDate=calendar.getTime();
        return new SimpleDateFormat("yyyy-MM-dd").format(afterDate);
    }

    @Test
    public void  getLhTime(){
        Double lhTime=lhEngineTireConstructionInfoService.getLhTireTimeBySapCode("1111","YHETB4458");
    }

}
