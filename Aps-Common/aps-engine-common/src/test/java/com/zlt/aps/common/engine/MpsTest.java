package com.zlt.aps.common.engine;

import com.zlt.aps.common.engine.domain.*;
import com.zlt.aps.common.engine.mapper.CxMonthPlanSurplusLogMapper;
import com.zlt.aps.common.engine.mapper.ProcedureSurplusLogMapper;
import com.zlt.aps.common.engine.mapper.TMonthSumProcessLogMapper;
import com.zlt.aps.common.engine.planmain.MdmMonthPlanAmountSumService;
import com.zlt.aps.common.engine.service.TGdcdMonthPlanSurplusService;
import com.zlt.aps.common.engine.service.TLbcdMonthPlanSurplusService;
import com.zlt.aps.common.engine.service.TTmMonthPlanSurplusService;
import com.zlt.aps.common.engine.utils.DateUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

/**
 * @author Gim
 */
@SpringBootTest
public class MpsTest {

    @Autowired
    private TGdcdMonthPlanSurplusService cd15Service;
    @Autowired
    private MdmMonthPlanAmountSumService sumService;

    @Autowired
    private TTmMonthPlanSurplusService tmMonthPlanSurplusService;

    @Autowired
    private TLbcdMonthPlanSurplusService lbcdMonthPlanSurplusService;

    @Resource
    private ProcedureSurplusLogMapper halfPartLogMapper;

    @Resource
    private CxMonthPlanSurplusLogMapper lhLogMapper;

    @Resource
    private TMonthSumProcessLogMapper logMapper;

    @Test
    public void timeTest() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MMdd");
        Date parse = sdf.parse("0102");
        sdf = new SimpleDateFormat("MMMMM d",Locale.ENGLISH);
        System.out.println("sdf.format(parse) = " + sdf.format(parse));
    }
    @Test
    public void logTest() {
        MonthSumProcessLog log = new MonthSumProcessLog();
        log.setBaseVale(null);
        log.setTitle("测试插入");
        log.setMonthPlanApsVersion("202109180001");
        log.setLogDetail("【测试插入】");
        logMapper.insert(log);
    }
    @Test
    public void selectCd15Test() {
        String apsVersion = "APS0000000033";
        List<String> codeList = new ArrayList<>();
        codeList.add("HK2224");
        codeList.add("HU2325");
        List<TGdcdMonthPlanSurplus> list = cd15Service.getByCodeList(apsVersion, codeList);
        for (TGdcdMonthPlanSurplus tGdcdMonthPlanSurplus : list) {
            System.out.println("tGdcdMonthPlanSurplus = " + tGdcdMonthPlanSurplus);
        }
    }

    @Test
    public void buildHalfTest() {
        String apsVersion = "APS202107090001";
        List<TCxEmbryoMonthPlanSurplus> list = new ArrayList<>();
        TCxEmbryoMonthPlanSurplus em = new TCxEmbryoMonthPlanSurplus();

//        sumService.buildHalfPartByEmbryoCastor();
    }

    @Test
    public void deleteTest() {
//        List<TLbcdMonthPlanSurplus> list = lbcdMonthPlanSurplusService.getByApsVersion("APS202107090001");
//        for (TLbcdMonthPlanSurplus tLbcdMonthPlanSurplus : list) {
//            System.out.println("tLbcdMonthPlanSurplus = " + tLbcdMonthPlanSurplus);
//        }
        lbcdMonthPlanSurplusService.deleteByApsVersion("APS202107090001");
    }

    @Test
    public void test() {
        List<TGdcdMonthPlanSurplus> list = new ArrayList<>();
        TGdcdMonthPlanSurplus one = new TGdcdMonthPlanSurplus();
        one.setMaterialCode("one");
        TGdcdMonthPlanSurplus two = new TGdcdMonthPlanSurplus();
        two.setMaterialCode("two");
        TGdcdMonthPlanSurplus three = new TGdcdMonthPlanSurplus();
        three.setMaterialCode("three");
        list.add(one);
        list.add(two);
        list.add(three);
        ListIterator<TGdcdMonthPlanSurplus> iterator = list.listIterator();
        TGdcdMonthPlanSurplus twoTwo = new TGdcdMonthPlanSurplus();
        twoTwo.setMaterialCode("two");
        twoTwo.setYear("twoTwo");
        while (iterator.hasNext()) {
            TGdcdMonthPlanSurplus next = iterator.next();
            if (next.getMaterialCode().equals(twoTwo.getMaterialCode())) {
                iterator.remove();
                twoTwo.setId(1L);
                next = twoTwo;
                iterator.add(next);
            }
            next.setMaterialCode("twoTwo");
        }
        for (TGdcdMonthPlanSurplus i : list) {
            System.out.println("i = " + i);
        }
    }

    @Test
    public void halfPartLogTest() {
        List<ProcedureSurplusLog> procedureSurplusLogCollection = new ArrayList<>();
        ProcedureSurplusLog log1 = new ProcedureSurplusLog();
        log1.setBaseVale(null);
        log1.setYear("2021");
        log1.setMaterialCode("123");
        ProcedureSurplusLog log2 = new ProcedureSurplusLog();
        log2.setBaseVale(null);
        log2.setYear("2022");
        log2.setMaterialCode("321");
        procedureSurplusLogCollection.add(log1);
        procedureSurplusLogCollection.add(log2);
        halfPartLogMapper.insertBatch(procedureSurplusLogCollection);
    }

    @Test
    public void lhLogTest() {
        List<CxMonthPlanSurplusLog> cxMonthPlanSurplusLogCollection = new ArrayList<>();
        CxMonthPlanSurplusLog log1 = new CxMonthPlanSurplusLog();
        log1.setBaseVale(null);
        log1.setYear("2021");
        log1.setSapCode("123");
        CxMonthPlanSurplusLog log2 = new CxMonthPlanSurplusLog();
        log2.setBaseVale(null);
        log2.setYear("2022");
        log2.setSapCode("321");
        cxMonthPlanSurplusLogCollection.add(log1);
        cxMonthPlanSurplusLogCollection.add(log2);
        lhLogMapper.insertBatch(cxMonthPlanSurplusLogCollection);
    }

    @Test
    public void recalculateByApsVersion(){
        String apsVersion="APS2021101314";
        sumService.recalculateByApsVersion(apsVersion);
    }
}
