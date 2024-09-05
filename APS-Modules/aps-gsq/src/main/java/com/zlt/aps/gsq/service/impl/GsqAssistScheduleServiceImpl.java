package com.zlt.aps.gsq.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqAssistSchedule;
import com.zlt.aps.gsq.mapper.GsqAssistScheduleMapper;
import com.zlt.aps.gsq.service.GsqAssistScheduleService;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * 钢丝圈外协排程结果Service业务层处理
 *
 * @author chen
 * @date 2022-02-15
 */
@Service
public class GsqAssistScheduleServiceImpl implements GsqAssistScheduleService {
    @Autowired
    private GsqAssistScheduleMapper gsqAssistScheduleMapper;
    @Value("${excelModelPath}")
    private String excelModelPath;
    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    /**
     * 查询钢丝圈外协排程结果列表
     *
     * @param gsqAssistSchedule 钢丝圈外协排程结果
     * @return 钢丝圈外协排程结果
     */
    @Override
    public List<GsqAssistSchedule> selectGsqAssistScheduleList(GsqAssistSchedule gsqAssistSchedule) {
        return gsqAssistScheduleMapper.selectGsqAssistScheduleList(gsqAssistSchedule);
    }

    /**
     * 导出excel
     *
     * @param list 要导出的数据集合
     * @return 数据数组
     */
    @Override
    public byte[] export(List<GsqAssistSchedule> list) {
        // 按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream inputStream = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "gsqAssistSchedule.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "gsqAssistSchedule_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(inputStream);
        CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
        DataFormat format = webBook.createDataFormat();
        cellStyle.setDataFormat(format.getFormat("[=0]\"\""));
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Sheet sheet = webBook.getSheetAt(0);
            webBook.setSheetName(0, I18nUtil.getMessage("ui.data.column.gsq.assistSchedule.modelName"));
            int month = com.zlt.aps.common.engine.utils.DateUtil.getMonth(list.get(0).getScheduleDate());
            int day = com.zlt.aps.common.engine.utils.DateUtil.getDay(list.get(0).getScheduleDate());
            Row row1 = sheet.getRow(0);
            BigDecimal midPlan = BigDecimal.ZERO;
            BigDecimal nightPlan = BigDecimal.ZERO;
            BigDecimal dayPlan = BigDecimal.ZERO;
            for (int i = 0; i < list.size(); i++) {
                int cellNum = 0;
                GsqAssistSchedule scheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
//                row.createCell(cellNum++).setCellValue(DateFormatUtils.format(scheduleResult.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(scheduleResult.getSteelType() == null ? "" : scheduleResult.getSteelType());
                row.createCell(cellNum++).setCellValue(scheduleResult.getSteelRingCode() == null ? "" : scheduleResult.getSteelRingCode());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDimension() == null ? 0 : Double.parseDouble(scheduleResult.getDimension()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getRank() == null ? "" : scheduleResult.getRank());

                row.createCell(cellNum++).setCellValue(scheduleResult.getMonthPlanOs() == null ? 0 : Double.parseDouble(scheduleResult.getMonthPlanOs()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getStockQty() == null ? 0 : scheduleResult.getStockQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDailyTotalQty() == null ? 0 : scheduleResult.getDailyTotalQty());
                Double midPlanQty = scheduleResult.getMidPlanQty();
                row.createCell(cellNum++).setCellValue(midPlanQty == null ? 0 : midPlanQty);
                midPlan = midPlan.add(BigDecimal.valueOf(midPlanQty == null ? 0 : midPlanQty));
                String midSysAnalysis = scheduleResult.getMidSysAnalysis();
                String midHandAnalysis = scheduleResult.getMidHandAnalysis();
                String midAnalysis = "";
                if (StringUtils.isNotEmpty(midSysAnalysis)) {
                    midAnalysis = midAnalysis + midSysAnalysis;
                }
                if (StringUtils.isNotEmpty(midHandAnalysis)) {
                    if (StringUtils.isNotEmpty(midAnalysis)) {
                        midAnalysis = midAnalysis + "," + midHandAnalysis;
                    } else {
                        midAnalysis = midHandAnalysis;
                    }
                }
                row.createCell(cellNum++).setCellValue(midAnalysis);
                row.createCell(cellNum++).setCellValue(scheduleResult.getNightPlanQty() == null ? 0 : scheduleResult.getNightPlanQty());
                nightPlan = nightPlan.add(BigDecimal.valueOf(scheduleResult.getNightPlanQty() == null ? 0 : scheduleResult.getNightPlanQty()));
                String nightSysAnaly = scheduleResult.getNightSysAnalysis();
                String nightHandAnaly = scheduleResult.getNightHandAnalysis();
                String nightAnly = "";
                if (StringUtils.isNotEmpty(nightSysAnaly)) {
                    nightAnly = nightAnly + nightSysAnaly;
                }
                if (StringUtils.isNotEmpty(nightHandAnaly)) {
                    if (StringUtils.isNotEmpty(nightAnly)) {
                        nightAnly = nightAnly + "," + nightHandAnaly;
                    } else {
                        nightAnly = nightHandAnaly;
                    }
                }
                row.createCell(cellNum++).setCellValue(nightAnly);
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayPlanQty() == null ? 0 : scheduleResult.getDayPlanQty());
                dayPlan = dayPlan.add(BigDecimal.valueOf(scheduleResult.getDayPlanQty() == null ? 0 : scheduleResult.getDayPlanQty()));
                String sysAnaly = scheduleResult.getDaySysAnalysis();
                String handAnaly = scheduleResult.getDayHandAnalysis();
                String anly = "";
                if (StringUtils.isNotEmpty(sysAnaly)) {
                    anly = anly + sysAnaly;
                }
                if (StringUtils.isNotEmpty(handAnaly)) {
                    if (StringUtils.isNotEmpty(anly)) {
                        anly = anly + "," + handAnaly;
                    } else {
                        anly = handAnaly;
                    }
                }
                row.createCell(cellNum++).setCellValue(anly);
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass1Plan() == null ? 0 : scheduleResult.getCxClass1Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass2Plan() == null ? 0 : scheduleResult.getCxClass2Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass3Plan() == null ? 0 : scheduleResult.getCxClass3Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass4Plan() == null ? 0 : scheduleResult.getCxClass4Plan());
                row.createCell(cellNum).setCellValue(scheduleResult.getCxClass5Plan() == null ? 0 : scheduleResult.getCxClass5Plan());
                setCellStyle(row, row.getPhysicalNumberOfCells(), cellStyle);
            }

            //重置表头基本信息
            String dateStr = "";
            if ("zh_CN".equals(lang.toString())) {
                dateStr = DateUtils.parseDateToStr("MM月dd日", list.get(0).getScheduleDate());
            } else {
                String monthStr = month + "";
                String dayStr = day + "";
                if (monthStr.length() <= 1) {
                    monthStr = "0" + month;
                }
                if (dayStr.length() <= 1) {
                    dayStr = "0" + day;
                }
                dateStr = DateUtil.getEngMonthDay(monthStr + dayStr) + " ";
            }
            String baseInfo = I18nUtil.getMessage("ui.data.column.scheduleResult.gsq.baseInfo");
            String class1Plan = I18nUtil.getMessage("ui.data.column.scheduleResult.heji.zhongban");
            String class2Plan = I18nUtil.getMessage("ui.data.column.scheduleResult.heji.yeban");
            String class3Plan = I18nUtil.getMessage("ui.data.column.scheduleResult.heji.baiban");
            String totalQty = I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty");
            String planInfo = '：' + class1Plan + '：' + midPlan.setScale(0, BigDecimal.ROUND_HALF_UP) + '，' + class2Plan + '：' + nightPlan.setScale(0, BigDecimal.ROUND_HALF_UP) + '，' + class3Plan + '：' + dayPlan.setScale(0, BigDecimal.ROUND_HALF_UP) + '，' + totalQty + '：' + (midPlan.add(nightPlan).add(dayPlan)).setScale(0, BigDecimal.ROUND_HALF_UP);
            baseInfo = dateStr + baseInfo + planInfo;
            Cell cell0 = sheet.getRow(0).getCell(0);
            CellStyle cellStyle0 = cell0.getCellStyle();
            cell0.setCellValue(baseInfo);
            cell0.setCellStyle(cellStyle0);

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
            try {
                assert out != null;
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    /**
     * 设置单元格样式
     *
     * @param row       单元行对象
     * @param cellNum   列数
     * @param cellStyle 样式
     */
    private void setCellStyle(Row row, int cellNum, CellStyle cellStyle) {
        for (int i = 0; i < cellNum; i++) {
            row.getCell(i).setCellStyle(cellStyle);
        }
    }
}
