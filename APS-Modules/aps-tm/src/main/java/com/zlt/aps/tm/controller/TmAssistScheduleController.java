package com.zlt.aps.tm.controller;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.tm.api.domain.dto.TmAssistScheduleDto;
import com.zlt.aps.tm.service.TmAssistScheduleService;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * 胎面外协排程结果Controller
 *
 * @author chen
 * @date 2022-02-15
 */
@RestController
@RequestMapping("/assistSchedule")
public class TmAssistScheduleController extends BaseController {

    @Value("${excelModelPath}")
    public String excelModelPath;
    @Autowired
    private TmAssistScheduleService tmAssistScheduleService;

    /**
     * 查询胎面外协排程结果列表
     */
    @ApiOperation("查询胎面外协排程结果列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TmAssistScheduleDto tmAssistSchedule) {
        tmAssistSchedule.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<TmAssistScheduleDto> list = tmAssistScheduleService.selectTmAssistScheduleList(tmAssistSchedule);
        return getDataTable(list);
    }

    /**
     * 导出列表
     */
    @Log(title = "ui.data.column.tm.assistSchedule.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public byte[] export(@RequestBody TmAssistScheduleDto assistScheduleDto) throws Exception {
        //查询数据
        assistScheduleDto.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<TmAssistScheduleDto> list = tmAssistScheduleService.selectTmAssistScheduleList(assistScheduleDto);
        //按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream in = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "tmAssistSchedule.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "tmAssistSchedule_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(in);
        webBook.setSheetName(0, I18nUtil.getMessage("ui.data.column.tm.assistSchedule.modelName"));
        //填充数据
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(list)) {
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            DataFormat format = webBook.createDataFormat();
            cellStyle.setDataFormat(format.getFormat("[=0]\"\""));  //导出的单元格如果值为0，则显示空白
            int month = DateUtil.getMonth(assistScheduleDto.getScheduleDate());
            int day = DateUtil.getDay(assistScheduleDto.getScheduleDate());
            Row row1 = sheet.getRow(0);
            BigDecimal midPlan = BigDecimal.ZERO;
            BigDecimal nightPlan = BigDecimal.ZERO;
            for (int i = 0; i < list.size(); i++) {
                int n = 0;
                TmAssistScheduleDto scheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
//                row.createCell(n++).setCellValue(DateUtils.parseDateToStr("yyyy-MM-dd",scheduleResult.getScheduleDate()));
//                row.createCell(n++).setCellValue(scheduleResult.getSpecDesc() == null ? "" : scheduleResult.getSpecDesc());
//                row.createCell(n++).setCellValue(scheduleResult.getWorkCode() == null ? "" : scheduleResult.getWorkCode());
                row.createCell(n++).setCellValue(scheduleResult.getTreadCode() == null ? "" : scheduleResult.getTreadCode());
                row.createCell(n++).setCellValue(scheduleResult.getGlueCode() == null ? "" : scheduleResult.getGlueCode());
//                row.createCell(n++).setCellValue(scheduleResult.getReinforceSealGlue() == null ? "" : scheduleResult.getReinforceSealGlue());
                row.createCell(n++).setCellValue(scheduleResult.getGlueSeq() == null ? 0 : Double.parseDouble(scheduleResult.getGlueSeq()));
                row.createCell(n++).setCellValue(scheduleResult.getMouthPlateCode() == null ? "" : scheduleResult.getMouthPlateCode());
                row.createCell(n++).setCellValue(scheduleResult.getMonthPlanOs() == null ? 0 : scheduleResult.getMonthPlanOs());
                row.createCell(n++).setCellValue(scheduleResult.getStockQty() == null ? 0 : scheduleResult.getStockQty());
                row.createCell(n++).setCellValue(scheduleResult.getDailyTotalQty() == null ? 0 : scheduleResult.getDailyTotalQty());
                row.createCell(n++).setCellValue(scheduleResult.getDayPlanQty() == null ? 0 : scheduleResult.getDayPlanQty());
                midPlan = midPlan.add(BigDecimal.valueOf(scheduleResult.getDayPlanQty() == null ? 0 : scheduleResult.getDayPlanQty()));

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
                row.createCell(n++).setCellValue(anly);
                row.createCell(n++).setCellValue(scheduleResult.getNightPlanQty() == null ? 0 : scheduleResult.getNightPlanQty());
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
                row.createCell(n++).setCellValue(nightAnly);
                row.createCell(n++).setCellValue(scheduleResult.getPrePlanQty() == null ? 0 : scheduleResult.getPrePlanQty());
                row.createCell(n++).setCellValue(scheduleResult.getCxClass1Plan() == null ? 0 : scheduleResult.getCxClass1Plan());
                row.createCell(n++).setCellValue(scheduleResult.getCxClass2Plan() == null ? 0 : scheduleResult.getCxClass2Plan());
                row.createCell(n++).setCellValue(scheduleResult.getCxClass3Plan() == null ? 0 : scheduleResult.getCxClass3Plan());
                row.createCell(n++).setCellValue(scheduleResult.getCxClass4Plan() == null ? 0 : scheduleResult.getCxClass4Plan());
                row.createCell(n).setCellValue(scheduleResult.getCxClass5Plan() == null ? 0 : scheduleResult.getCxClass5Plan());
                int a = row.getPhysicalNumberOfCells();
                for (int j = 0; j < a; j++) {
                    if (j == 0) {
                        CellStyle cellStyle1 = webBook.createCellStyle();
                        cellStyle1.setAlignment(HorizontalAlignment.LEFT);
                        cellStyle1.setVerticalAlignment(VerticalAlignment.CENTER);
                        cellStyle1.setBorderBottom(BorderStyle.THIN);
                        cellStyle1.setBorderTop(BorderStyle.THIN);
                        cellStyle1.setBorderLeft(BorderStyle.THIN);
                        cellStyle1.setBorderRight(BorderStyle.THIN);
                        row.getCell(j).setCellStyle(cellStyle1);
                    }else {
                        row.getCell(j).setCellStyle(cellStyle);
                    }
                }
            }

            //重置表头基本信息
            String dateStr="";
            if("zh_CN".equals(lang.toString())){
                dateStr= DateUtils.parseDateToStr("MM月dd日",assistScheduleDto.getScheduleDate());
            }else{
                String monthStr=month+"";
                String dayStr=day+"";
                if(monthStr.length()<=1){
                    monthStr="0"+month;
                }
                if(dayStr.length()<=1){
                    dayStr="0"+day;
                }
                dateStr=DateUtil.getEngMonthDay(monthStr+dayStr) + " ";
            }
            String baseInfo=I18nUtil.getMessage("ui.data.column.scheduleResult.tm.baseInfo");
            String class1Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.zhongban");
            String class2Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.yeban");
            String totalQty=I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty");
            String planInfo = '：'+class1Plan+'：'+midPlan.setScale(0, BigDecimal.ROUND_HALF_UP)+'，'+class2Plan+'：'+nightPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+totalQty+'：'+(midPlan.add(nightPlan)).setScale(0,BigDecimal.ROUND_HALF_UP);
            baseInfo=dateStr+baseInfo+planInfo;
            Cell cell0=sheet.getRow(0).getCell(0);
            CellStyle cellStyle0=cell0.getCellStyle();
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
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }
}
