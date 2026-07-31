package com.zlt.aps.cd15.service.impl;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.component.Cd15ScheduleResultExportAssembler;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineConstructionMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCxScheduleMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.mapper.Cd15StockMapper;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Cd15ScheduleResultExportServiceTest {

    @Mock
    private Cd15ScheduleResultMapper resultMapper;

    @Mock
    private Cd15StockMapper stockMapper;

    @Mock
    private Cd15EngineCxScheduleMapper cxScheduleMapper;

    @Mock
    private Cd15EngineConstructionMapper constructionMapper;

    @Spy
    private Cd15ScheduleResultExportAssembler exportAssembler =
            new Cd15ScheduleResultExportAssembler();

    @InjectMocks
    private Cd15ScheduleResultServiceImpl service;

    @Test
    void exportDataLoadsRelatedDataAndWritesTemplateSheet()
            throws Exception {
        Cd15ScheduleResult query = new Cd15ScheduleResult();
        query.setFactoryCode("116");
        query.setScheduleDate(DateUtil.parse("2026-06-06"));

        Cd15ScheduleResult current = new Cd15ScheduleResult();
        current.setMachineCode("G1101");
        current.setSteelStripCode("DLC13001_2#");
        current.setBigRollCode("CSS24524");
        current.setCuttingAngle("18");
        current.setUnitConsumeMillimeter(new BigDecimal("3124"));
        current.setClass1PlanQty(100D);

        Cd15Stock stock = new Cd15Stock();
        stock.setMaterialCode("DLC13001_2#");
        stock.setStockNum(500D);

        CxScheduleResult forming = new CxScheduleResult();
        forming.setEmbryoCode("E1");
        forming.setClass1RecipeNo("V1");
        forming.setClass1PlanQty(new BigDecimal("800"));

        MdmConstructionInfo construction = new MdmConstructionInfo();
        construction.setConstructionCode("E1");
        construction.setConstructionVersion("V1");
        construction.setBeltCode1("DLC13001_2#");
        construction.setBeltName1("385/65R22.5-JY598零度");

        when(this.resultMapper.selectList(any()))
                .thenReturn(Collections.emptyList());
        when(this.stockMapper.selectList(any()))
                .thenReturn(Collections.singletonList(stock));
        when(this.cxScheduleMapper.selectList(any()))
                .thenReturn(Collections.singletonList(forming));
        when(this.constructionMapper.selectList(any()))
                .thenReturn(Collections.singletonList(construction));

        byte[] bytes = this.service.exportData(
                Collections.singletonList(current), query);

        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(bytes))) {
            assertEquals(1, workbook.getNumberOfSheets());
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("裁断计划 SBC", sheet.getSheetName());
            assertEquals(
                    "2026年06月06日全钢裁断工程生产计划单",
                    sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("G1101",
                    sheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals("18",
                    sheet.getRow(4).getCell(4).getStringCellValue());
            assertEquals(500D,
                    sheet.getRow(4).getCell(6).getNumericCellValue());
            assertEquals(800D,
                    sheet.getRow(4).getCell(16).getNumericCellValue());
        }

        verify(this.resultMapper).selectList(any());
        verify(this.stockMapper).selectList(any());
        verify(this.cxScheduleMapper).selectList(any());
        verify(this.constructionMapper).selectList(any());
    }
}
