package com.zlt.aps.cx.engine;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.service.EngineConstructionInfoService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.domain.MdmMonthPlanAnalysis;
import com.zlt.aps.common.engine.domain.TGdcdMonthPlanSurplus;
import com.zlt.aps.common.engine.planmain.MdmMonthPlanAmountSumService;
import com.zlt.aps.common.engine.service.MdmMonthPlanMainService;
import com.zlt.aps.common.engine.service.TGdcdMonthPlanSurplusService;
import org.apache.ibatis.javassist.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Gim
 */
@SpringBootTest
public class MdmMonthPlanAmountSumTest {

    private static final String APS_MAIN_PLAN = "APS";

    @Resource(name="cxMonthAmountService")
    private MdmMonthPlanAmountSumService amountSumService;

    @Resource(name="commonEngineConstructionInfoService")
    private EngineConstructionInfoService infoService;

    @Autowired
    private TGdcdMonthPlanSurplusService gdcdMonthPlanSurplusService;

    @Autowired
    private MdmMonthPlanMainService planMainService;


    @Resource
    private IncrementService versionService;

    /**
     * 输入胎胚代码和数量计算半部件需要
     */
    @Test
    public void halfPartTest() {
        String embryoCode = "EHETB0269";
        Integer num = 5;
//        MdmMonthPlanAnalysis embryoUnit = amountSumService.getEmbryoConsumption(embryoCode, num);
//        System.out.println("embryoUnit = " + embryoUnit);
    }

    @Test
    public void monthPlanAmountSumTest() throws NotFoundException {
        String planMainVersion = "202006010001";
        String year = "2020";
        String month = "07";
        Integer isFinal = 0;
        amountSumService.monthPlanAmountSum(planMainVersion, year, month, isFinal);
    }

    @Test
    public void serviceTest() {
        List<String> embryoList = new ArrayList<>();
        embryoList.add("EHETB0269");
        List<EngineConstructionInfo> infoList = infoService.selectEngineConstructionInfoListBatch(embryoList);
        infoList.forEach(obj -> System.out.println("obj.getEmbryoCode() = " + obj.getEmbryoCode()));
    }

    @Test
    public void gdcdServiceTest() {
        List<TGdcdMonthPlanSurplus> list = new ArrayList<>();
        TGdcdMonthPlanSurplus entity1 = new TGdcdMonthPlanSurplus();
        entity1.setBaseVale(null);
        entity1.setYear("2020");
        entity1.setMonth("06");
        list.add(entity1);
        TGdcdMonthPlanSurplus entity2 = new TGdcdMonthPlanSurplus();
        entity2.setBaseVale(null);
        entity2.setYear("2020");
        entity2.setMonth("07");
        list.add(entity2);
        gdcdMonthPlanSurplusService.addBatch(list);
    }

    @Test
    public void timeTest() {
        Date now = DateUtils.getNowDate();
        String apsYear=String.format("%tY", now);
//        System.out.println("apsYear = " + apsYear);
        String apsMonth=String .format("%tm", now);
//        System.out.println("apsMonth = " + apsMonth);
        String apsDay=String .format("%td", now);
//        System.out.println("apsDay = " + apsDay);
        String versionPre = APS_MAIN_PLAN + apsYear + apsMonth + apsDay;
        String version = versionService.getSequence(versionPre, 2);
        System.out.println("version = " + version);
    }

    @Test
    public void deletePlanMainTest() {
//        planMainService.deleteMdmMonthPlanMainById(30L);
        planMainService.deleteByApsVersion("APS0000000036");
    }

//    @Test
//    public void updateCxTest() throws NotFoundException {
////        amountSumService.updateCx("APS2021071201", "EHETB0269", "EHETB076P", 0, -1);
//    }
//
//    @Test
//    public void updateTmTest() throws NotFoundException {
//        amountSumService.updateTm("APS2021071201", "HT4574-076P", 1.1d);
//    }
//
//    @Test
//    public void updateTcTest() throws NotFoundException {
//        amountSumService.updateTc("APS2021071201", "EHF0584", 1.11d);
//    }
//
//    @Test
//    public void updateCd15Test() throws NotFoundException {
//        amountSumService.updateCd15("APS2021071201", "HU2325", 1.111d);
//    }
//
//    @Test
//    public void updateGdyyTest() throws NotFoundException {
//        amountSumService.updateGdyy("APS2021071201", "EHETB076P", "HASTLL", 1.11d);
//    }

    @Test
    public void bigDecimalTest() {
        BigDecimal bigDecimal1 = BigDecimal.valueOf(43647.78).subtract(BigDecimal.valueOf(1.111)).setScale(3, RoundingMode.UP);
        System.out.println("bigDecimal1 = " + bigDecimal1);
        BigDecimal bigDecimal = BigDecimal.valueOf(43647.78 - 1.111).setScale(3, RoundingMode.UP);
        System.out.println("bigDecimal = " + bigDecimal);
    }

}
