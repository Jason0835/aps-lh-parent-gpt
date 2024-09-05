package com.zlt.aps.cx.service.impl;

import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.cx.api.domain.dto.ReportClassAccuracyDto;
import com.zlt.aps.cx.mapper.ReportClassAccuracyMapper;
import com.zlt.aps.cx.service.ReportClassAccuracyService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 班次完成统计报表Service业务层处理
 *
 * @author chen
 * @date 2022-05-23
 */
@Service
public class ReportClassAccuracyServiceImpl implements ReportClassAccuracyService {
    @Autowired
    private ReportClassAccuracyMapper reportClassAccuracyMapper;
    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    @Value("${excelModelPath}")
    public String excelModelPath;

    /**
     * 查询班次完成统计报表列表
     *
     * @param reportClassAccuracy 班次完成统计报表
     * @return 班次完成统计报表集合
     */
    @Override
    public List<ReportClassAccuracyDto> selectReportClassAccuracyList(ReportClassAccuracyDto reportClassAccuracy) {
        String scheduleDate = reportClassAccuracy.getScheduleDate();  //排程日期
        Map<String, Integer> allTotalMap = new HashMap<>();  //全部工序的汇总map
        Map<String, Integer> halfPartSummaryMap = new HashMap<>();  //半部件工序的汇总map
        List<ReportClassAccuracyDto> reportList = new ArrayList<>();
        //统计硫化工序班次完成数据
        List<ReportClassAccuracyDto> lhList = reportClassAccuracyMapper.listLhClassAccuracy(scheduleDate);
        this.summary(lhList,allTotalMap, halfPartSummaryMap);
        //统计成型工序班次完成数据
        List<ReportClassAccuracyDto> cxList = reportClassAccuracyMapper.listCxClassAccuracy(scheduleDate);
        this.summary(cxList,allTotalMap, halfPartSummaryMap);
        //统计胎面工序班次完成数据
        List<ReportClassAccuracyDto> tmList = reportClassAccuracyMapper.listTmClassAccuracy(scheduleDate);
        this.summary(tmList,allTotalMap,halfPartSummaryMap);
        //统计胎侧工序班次完成数据
        List<ReportClassAccuracyDto> tcList = reportClassAccuracyMapper.listTcClassAccuracy(scheduleDate);
        this.summary(tcList,allTotalMap,halfPartSummaryMap);
        //统计内衬工序班次完成数据
        List<ReportClassAccuracyDto> ncList = reportClassAccuracyMapper.listNcClassAccuracy(scheduleDate);
        this.summary(ncList,allTotalMap,halfPartSummaryMap);
        //统计胎圈工序班次完成数据
        List<ReportClassAccuracyDto> tqList = reportClassAccuracyMapper.listTqClassAccuracy(scheduleDate);
        this.summary(tqList,allTotalMap,halfPartSummaryMap);
        //统计钢丝圈工序班次完成数据
        List<ReportClassAccuracyDto> gsqList = reportClassAccuracyMapper.listGsqClassAccuracy(scheduleDate);
        this.summary(gsqList,allTotalMap,halfPartSummaryMap);
        //统计15度裁断工序班次完成数据
        List<ReportClassAccuracyDto> cd15List = reportClassAccuracyMapper.listCd15ClassAccuracy(scheduleDate);
        this.summary(cd15List,allTotalMap,halfPartSummaryMap);
        //统计90度裁断工序班次完成数据
        List<ReportClassAccuracyDto> cd90List = reportClassAccuracyMapper.listCd90ClassAccuracy(scheduleDate);
        this.summary(cd90List,allTotalMap,halfPartSummaryMap);
        //统计纤维压延工序班次完成数据
        List<ReportClassAccuracyDto> xwyyList = reportClassAccuracyMapper.listXwyyClassAccuracy(scheduleDate);
        this.summary(xwyyList,allTotalMap,halfPartSummaryMap);

        reportList.addAll(lhList);
        reportList.addAll(cxList);
        reportList.addAll(tmList);
        reportList.addAll(tcList);
        reportList.addAll(ncList);
        reportList.addAll(tqList);
        reportList.addAll(gsqList);
        reportList.addAll(cd15List);
        reportList.addAll(cd90List);
        reportList.addAll(xwyyList);

        //创建半部件汇总记录
        if(halfPartSummaryMap.size() > 0) {
            ReportClassAccuracyDto halfPartsummary = new ReportClassAccuracyDto("-1", halfPartSummaryMap.get("class1PlanMaterialCount") + "", halfPartSummaryMap.get("planClass1"), halfPartSummaryMap.get("class1ActualMaterialCount") + "", halfPartSummaryMap.get("actualClass1"),
                    halfPartSummaryMap.get("class2PlanMaterialCount") + "", halfPartSummaryMap.get("planClass2"), halfPartSummaryMap.get("class2ActualMaterialCount") + "", halfPartSummaryMap.get("actualClass2"),
                    halfPartSummaryMap.get("class3PlanMaterialCount") + "", halfPartSummaryMap.get("planClass3"), halfPartSummaryMap.get("class3ActualMaterialCount") + "", halfPartSummaryMap.get("actualClass3"),1);
            reportList.add(halfPartsummary);
        }

        //给第一条记录设置 全工序的汇总数据值
        if(reportList != null && reportList.size() > 0) {
            ReportClassAccuracyDto firstClassAccuracy = reportList.get(0);
            firstClassAccuracy.setPlanSpecNum(allTotalMap.get("planSpecNum"));
            firstClassAccuracy.setPlanTotalNum(allTotalMap.get("planTotalNum"));
            firstClassAccuracy.setActualSpecNum(allTotalMap.get("actualSpecNum"));
            firstClassAccuracy.setActualTotalNum(allTotalMap.get("actualTotalNum"));
        }

        return reportList;
    }

    /**
     * 创建每个工序的汇总记录，并累计汇总值
     * @param list  各工序
     * @param allTotalMap  全部工序的汇总map
     * @param halfPartSummaryMap  半部件工序的汇总map
     */
    private void summary(List<ReportClassAccuracyDto> list, Map<String, Integer> allTotalMap,  Map<String, Integer> halfPartSummaryMap) {
        String procedureCode = "";  //工序代码
        int class1PlanMaterialCount = 0;  //白班计划完成规格个数汇总
        int planClass1 = 0;    //白班计划产量汇总
        int class1ActualMaterialCount = 0;  //白班实际完成规格个数汇总
        int actualClass1 = 0;     //白班实际产量汇总
        int class2PlanMaterialCount = 0;  //中班计划完成规格个数汇总
        int planClass2 = 0;    //中班计划产量汇总
        int class2ActualMaterialCount = 0;  //中班实际完成规格个数汇总
        int actualClass2 = 0;     //中班实际产量汇总
        int class3PlanMaterialCount = 0;  //夜班计划完成规格个数汇总
        int planClass3 = 0;    //夜班计划产量汇总
        int class3ActualMaterialCount = 0;  //夜班实际完成规格个数汇总
        int actualClass3 = 0;     //夜班实际产量汇总

        int planSpecNum = 0;  //计划总规格数
        int planTotalNum = 0;  //计划总产量
        int actualSpecNum = 0;  //实际总规格数
        int actualTotalNum = 0;  //实际总产量

        for (ReportClassAccuracyDto classAccuracyDto : list) {
            procedureCode = classAccuracyDto.getProcedureCode();
            //汇总白班数据
            if (StringUtils.isNotBlank(classAccuracyDto.getClass1PlanMaterial()) && classAccuracyDto.getPlanClass1() > 0) {
                class1PlanMaterialCount++;
            }
            if (StringUtils.isNotBlank(classAccuracyDto.getClass1ActualMaterial()) && classAccuracyDto.getActualClass1() > 0) {
                class1ActualMaterialCount++;
            }
            planClass1 += classAccuracyDto.getPlanClass1();
            actualClass1 += classAccuracyDto.getActualClass1();

            //汇总中班数据
            if (StringUtils.isNotBlank(classAccuracyDto.getClass2PlanMaterial()) && classAccuracyDto.getPlanClass2() > 0) {
                class2PlanMaterialCount++;
            }
            if (StringUtils.isNotBlank(classAccuracyDto.getClass2ActualMaterial()) && classAccuracyDto.getActualClass2() > 0) {
                class2ActualMaterialCount++;
            }
            planClass2 += classAccuracyDto.getPlanClass2();
            actualClass2 += classAccuracyDto.getActualClass2();

            //汇总夜班数据
            if (StringUtils.isNotBlank(classAccuracyDto.getClass3PlanMaterial()) && classAccuracyDto.getPlanClass3() > 0) {
                class3PlanMaterialCount++;
            }
            if (StringUtils.isNotBlank(classAccuracyDto.getClass3ActualMaterial()) && classAccuracyDto.getActualClass3() > 0) {
                class3ActualMaterialCount++;
            }
            planClass3 += classAccuracyDto.getPlanClass3();
            actualClass3 += classAccuracyDto.getActualClass3();

            //统计全工序的汇总数据
            if (StringUtils.isNotBlank(classAccuracyDto.getClass2PlanMaterial())) {
                planSpecNum++;
            }
            if (StringUtils.isNotBlank(classAccuracyDto.getClass2ActualMaterial())) {
                actualSpecNum++;
            }
            planTotalNum = planTotalNum + classAccuracyDto.getPlanClass1() + classAccuracyDto.getPlanClass2() + classAccuracyDto.getPlanClass3();
            actualTotalNum = actualTotalNum + classAccuracyDto.getActualClass1() + classAccuracyDto.getActualClass2() + classAccuracyDto.getActualClass3();
        }

        //集合数据不为空，则创建一条汇总记录
        if (list != null && !list.isEmpty()) {
            //创建各个工序的汇总记录start
            ReportClassAccuracyDto summaryDto = new ReportClassAccuracyDto(procedureCode, class1PlanMaterialCount + "", planClass1, class1ActualMaterialCount + "", actualClass1,
                    class2PlanMaterialCount + "", planClass2, class2ActualMaterialCount + "", actualClass2,
                    class3PlanMaterialCount + "", planClass3, class3ActualMaterialCount + "", actualClass3, 1);
            list.add(summaryDto);
            //创建各个工序的汇总记录end

            if(!"0".equals(procedureCode) && !"1".equals(procedureCode)) {
                //半部件工序累计汇总值
                halfPartSummaryMap.put("class1PlanMaterialCount", halfPartSummaryMap.getOrDefault("class1PlanMaterialCount",0) + class1PlanMaterialCount);  //白班计划完成规格个数汇总
                halfPartSummaryMap.put("planClass1", halfPartSummaryMap.getOrDefault("planClass1",0) + planClass1);  //白班计划产量汇总
                halfPartSummaryMap.put("class1ActualMaterialCount", halfPartSummaryMap.getOrDefault("class1ActualMaterialCount",0) + class1ActualMaterialCount);  //白班实际完成规格个数汇总
                halfPartSummaryMap.put("actualClass1", halfPartSummaryMap.getOrDefault("actualClass1",0) + actualClass1);  //白班实际产量汇总

                halfPartSummaryMap.put("class2PlanMaterialCount", halfPartSummaryMap.getOrDefault("class2PlanMaterialCount",0) + class2PlanMaterialCount);  //中班计划完成规格个数汇总
                halfPartSummaryMap.put("planClass2", halfPartSummaryMap.getOrDefault("planClass2",0) + planClass2);  //中班计划产量汇总
                halfPartSummaryMap.put("class2ActualMaterialCount", halfPartSummaryMap.getOrDefault("class2ActualMaterialCount",0) + class2ActualMaterialCount);  //中班实际完成规格个数汇总
                halfPartSummaryMap.put("actualClass2", halfPartSummaryMap.getOrDefault("actualClass2",0) + actualClass2);  //中班实际产量汇总

                halfPartSummaryMap.put("class3PlanMaterialCount", halfPartSummaryMap.getOrDefault("class3PlanMaterialCount",0) + class3PlanMaterialCount);  //夜班计划完成规格个数汇总
                halfPartSummaryMap.put("planClass3", halfPartSummaryMap.getOrDefault("planClass3",0) + planClass3);  //夜班计划产量汇总
                halfPartSummaryMap.put("class3ActualMaterialCount", halfPartSummaryMap.getOrDefault("class3ActualMaterialCount",0) + class3ActualMaterialCount);  //夜班实际完成规格个数汇总
                halfPartSummaryMap.put("actualClass3", halfPartSummaryMap.getOrDefault("actualClass3",0) + actualClass3);  //夜班实际产量汇总
            }

            //全部工序的汇总数据累计后，存入map
            allTotalMap.put("planSpecNum", allTotalMap.getOrDefault("planSpecNum", 0) + planSpecNum);
            allTotalMap.put("planTotalNum", allTotalMap.getOrDefault("planTotalNum", 0) + planTotalNum);
            allTotalMap.put("actualSpecNum", allTotalMap.getOrDefault("actualSpecNum", 0) + actualSpecNum);
            allTotalMap.put("actualTotalNum", allTotalMap.getOrDefault("actualTotalNum", 0) + actualTotalNum);
        }
    }

    /**
     * 导出班次完成统计报表列表
     */
    @Override
    public byte[] export(ReportClassAccuracyDto reportClassAccuracy) {
        //查询数据
        List<ReportClassAccuracyDto> list = this.selectReportClassAccuracyList(reportClassAccuracy);
        String tempName = I18nUtil.getMessage("ui.data.column.reportClassAccuracy.modelName");
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + tempName + ".xlsx");
        Workbook webBook = ExcelUtils.readExcel(in);

        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Sheet sheet = webBook.getSheetAt(0);

            if (list.size() > 0) {
                //重置表头基本信息
                ReportClassAccuracyDto dto = list.get(0);
                String title = I18nUtil.getMessage("ui.data.column.reportClassAccuracy.firstTitle");
                title = String.format(title, dto.getPlanSpecNum(), dto.getPlanTotalNum(), dto.getActualSpecNum(), dto.getActualTotalNum());
                Cell cell0 = sheet.getRow(0).getCell(0);
                CellStyle cellStyle0 = cell0.getCellStyle();
                cell0.setCellValue(title);
                cell0.setCellStyle(cellStyle0);
            }
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            CellStyle yellowCellStyle = ExcelUtils.createCellStyle(webBook);
            yellowCellStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            yellowCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Map<String, String> procedureCodeMap = reportClassAccuracy.getProcedureCodeMap();
            String summary = I18nUtil.getMessage("ui.data.column.reportStatistics.summary");
            for (int i = 0; i < list.size(); i++) {
                ReportClassAccuracyDto dto = list.get(i);
                Row row = sheet.createRow(i + 3);
                int rowNum = 0;
                boolean isSummary = (dto.getIsSummary() != null && dto.getIsSummary() == 1);

                String procedure = procedureCodeMap.getOrDefault(dto.getProcedureCode(), I18nUtil.getMessage("ui.data.column.reportClassAccuracy.procedureCodeDefault"));

                row.createCell(rowNum++).setCellValue(isSummary ? procedure + summary : procedure);
                row.createCell(rowNum++).setCellValue(dto.getClass3PlanMaterial() == null ? "" : dto.getClass3PlanMaterial());
                row.createCell(rowNum++).setCellValue(dto.getPlanClass3() == null ? 0 : dto.getPlanClass3());
                row.createCell(rowNum++).setCellValue(dto.getClass3ActualMaterial() == null ? "" : dto.getClass3ActualMaterial());
                row.createCell(rowNum++).setCellValue(dto.getActualClass3() == null ? 0 : dto.getActualClass3());
                row.createCell(rowNum++).setCellValue(dto.getClass1PlanMaterial() == null ? "" : dto.getClass1PlanMaterial());
                row.createCell(rowNum++).setCellValue(dto.getPlanClass1() == null ? 0 : dto.getPlanClass1());
                row.createCell(rowNum++).setCellValue(dto.getClass1ActualMaterial() == null ? "" : dto.getClass1ActualMaterial());
                row.createCell(rowNum++).setCellValue(dto.getActualClass1() == null ? 0 : dto.getActualClass1());
                row.createCell(rowNum++).setCellValue(dto.getClass2PlanMaterial() == null ? "" : dto.getClass2PlanMaterial());
                row.createCell(rowNum++).setCellValue(dto.getPlanClass2() == null ? 0 : dto.getPlanClass2());
                row.createCell(rowNum++).setCellValue(dto.getClass2ActualMaterial() == null ? "" : dto.getClass2ActualMaterial());
                row.createCell(rowNum).setCellValue(dto.getActualClass2() == null ? 0 : dto.getActualClass2());

                //设置单元格样式
                int a = row.getPhysicalNumberOfCells();
                for (int j = 0; j < a; j++) {
                    row.getCell(j).setCellStyle(isSummary ? yellowCellStyle : cellStyle);
                }
            }
        }
        //写出字节流
        ByteArrayOutputStream out = null;
        byte[] data = null;
        try {
            out = new ByteArrayOutputStream();
            webBook.write(out);
            data = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
        }
        return data;
    }
}
