package com.zlt.aps.xwyy.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleAssist;
import com.zlt.aps.xwyy.mapper.XwyyScheduleAssistMapper;
import com.zlt.aps.xwyy.service.XwyyScheduleAssistService;
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
 * 纤维压延外协排程结果Service业务层处理
 *
 * @author chen
 * @date 2022-02-16
 */
@Service
public class XwyyScheduleAssistServiceImpl implements XwyyScheduleAssistService {

    @Value("${excelModelPath}")
    private String excelModelPath;
    @Autowired
    private XwyyScheduleAssistMapper xwyyScheduleAssistMapper;

    /**
     * 查询纤维压延外协排程结果列表
     *
     * @param xwyyScheduleAssist 纤维压延外协排程结果
     * @return 纤维压延外协排程结果
     */
    @Override
    public List<XwyyScheduleAssist> selectXwyyScheduleAssistList(XwyyScheduleAssist xwyyScheduleAssist) {
        return xwyyScheduleAssistMapper.selectXwyyScheduleAssistList(xwyyScheduleAssist);
    }

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    @Override
    public byte[] export(List<XwyyScheduleAssist> list) {
        // 按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream inputStream = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "xwyyScheduleAssist.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "xwyyScheduleAssist_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(inputStream);
        CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
        DataFormat format = webBook.createDataFormat();
        cellStyle.setDataFormat(format.getFormat("[=0]\"\""));  //导出的单元格如果值为0，则显示空白
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Sheet sheet = webBook.getSheetAt(0);
            webBook.setSheetName(0, I18nUtil.getMessage("ui.data.column.xwyy.assistSchedule.modelName"));
            int month = com.zlt.aps.common.engine.utils.DateUtil.getMonth(list.get(0).getScheduleDate());
            int day = com.zlt.aps.common.engine.utils.DateUtil.getDay(list.get(0).getScheduleDate());
            Row row1 = sheet.getRow(0);

            BigDecimal midPlan = BigDecimal.ZERO;
            BigDecimal nightPlan = BigDecimal.ZERO;

            for (int i = 0; i < list.size(); i++) {
                int cellNum = 0;
                XwyyScheduleAssist scheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
//                row.createCell(cellNum++).setCellValue(DateFormatUtils.format(scheduleResult.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(scheduleResult.getBigRollCode() == null ? "" : scheduleResult.getBigRollCode());
                row.createCell(cellNum++).setCellValue(scheduleResult.getOriginalLineCode() == null ? "" : scheduleResult.getOriginalLineCode());

                row.createCell(cellNum++).setCellValue(scheduleResult.getMonthPlanOs() == null ? 0 : Double.parseDouble(scheduleResult.getMonthPlanOs()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getYesStock() == null ? 0 : scheduleResult.getYesStock());
                row.createCell(cellNum++).setCellValue(scheduleResult.getTodayStock() == null ? 0 : scheduleResult.getTodayStock());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayUsed() == null ? 0 : scheduleResult.getDayUsed());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDailyTotalQty() == null ? 0 : scheduleResult.getDailyTotalQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDailyTotalQtyNum() == null ? 0d : scheduleResult.getDailyTotalQtyNum());
                midPlan = midPlan.add(BigDecimal.valueOf(scheduleResult.getDayPlanQty() == null ? 0 : scheduleResult.getDayPlanQty()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayPlanQty() == null ? 0 : scheduleResult.getDayPlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayPlanQtyNum() == null ? 0d : scheduleResult.getDayPlanQtyNum());
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
                nightPlan = nightPlan.add(BigDecimal.valueOf(scheduleResult.getNightPlanQty() == null ? 0 : scheduleResult.getNightPlanQty()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getNightPlanQty() == null ? 0 : scheduleResult.getNightPlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getNightPlanQtyNum() == null ? 0d : scheduleResult.getNightPlanQtyNum());
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
                row.createCell(cellNum++).setCellValue(scheduleResult.getTotalPlan() == null ? 0 : scheduleResult.getTotalPlan());
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
            String baseInfo = I18nUtil.getMessage("ui.data.column.scheduleResult.xwyy.baseInfo");
            String class1Plan = I18nUtil.getMessage("ui.data.column.scheduleResult.heji.zhongban");
            String class2Plan = I18nUtil.getMessage("ui.data.column.scheduleResult.heji.yeban");
            String totalQty = I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty");
            String planInfo = '：' + class1Plan + '：' + midPlan.setScale(0, BigDecimal.ROUND_HALF_UP) + '，' + class2Plan + '：' + nightPlan.setScale(0, BigDecimal.ROUND_HALF_UP) + '，' + totalQty + '：' + (midPlan.add(nightPlan)).setScale(0, BigDecimal.ROUND_HALF_UP);
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
