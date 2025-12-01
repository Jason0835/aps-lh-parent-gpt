package com.zlt.mix.schedule.controller;

import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.common.core.utils.ExcelUtils;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlanInit;
import com.zlt.mix.schedule.service.GlueDemandPlanInitService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * 分厂胶料需求计划（初始表）Controller
 *
 * @author Gim
 * @date 2022-04-05
 */
@RestController
@RequestMapping("/factoryGluePlanStatistics")
public class GlueDemandPlanInitController extends BaseController {
    @Value("${excelModelPath}")
    public String excelModelPath;
    @Resource
    private GlueDemandPlanInitService glueDemandPlanInitService;

    /**
     * 查询分厂胶料需求计划（初始表）列表
     */
    @ApiOperation("查询分厂胶料需求计划（初始表）列表")
    @PostMapping("/list")
    public TableDataInfo listGlueDemandPlanInit(@RequestBody GlueDemandPlanInit glueDemandPlanInit) {
        startPage(false);
        glueDemandPlanInit.setOrderStr(orderStr());
        List<GlueDemandPlanInit> list = glueDemandPlanInitService.selectGlueDemandPlanInitList(glueDemandPlanInit);
        return getDataTable(list);
    }

    @Log(title = "schedule.factoryGluePlanStatistics.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出分厂胶料需求计划（初始表）列表")
    @PostMapping("/exportData")
    public byte[] exportData(@RequestBody GlueDemandPlanInit glueDemandPlanInit) {
        startPage(false);
        glueDemandPlanInit.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<GlueDemandPlanInit> list = glueDemandPlanInitService.selectGlueDemandPlanInitList(glueDemandPlanInit);
        //按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream in = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "factoryGluePlanStatistics.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "factoryGluePlanStatistics_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(in);
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            for (int i = 0; i < list.size(); i++) {
                int n = 0;
                GlueDemandPlanInit demandPlanInit = list.get(i);
                Row row = sheet.createRow(i + 2);
                row.createCell(n++).setCellValue(demandPlanInit.getPlanDate() == null ? "" : DateFormatUtils.format(demandPlanInit.getPlanDate(), "yyyy-MM-dd"));
                row.createCell(n++).setCellValue(demandPlanInit.getGlue() == null ? "" : demandPlanInit.getGlue());

                row.createCell(n++).setCellValue(demandPlanInit.getTotalPlanQty() == null ? BigDecimal.ZERO.doubleValue() : demandPlanInit.getTotalPlanQty().doubleValue());
                row.createCell(n++).setCellValue(demandPlanInit.getMidPlanQty() == null ? BigDecimal.ZERO.doubleValue() : demandPlanInit.getMidPlanQty().doubleValue());
                row.createCell(n++).setCellValue(demandPlanInit.getMidRemark() == null ? "" : demandPlanInit.getMidRemark());
                row.createCell(n++).setCellValue(demandPlanInit.getNightPlanQty() == null ? BigDecimal.ZERO.doubleValue() : demandPlanInit.getNightPlanQty().doubleValue());
                row.createCell(n++).setCellValue(demandPlanInit.getNightRemark() == null ? "" : demandPlanInit.getNightRemark());
                row.createCell(n++).setCellValue(demandPlanInit.getDayPlanQty() == null ? BigDecimal.ZERO.doubleValue() : demandPlanInit.getDayPlanQty().doubleValue());
                row.createCell(n).setCellValue(demandPlanInit.getDayRemark() == null ? "" : demandPlanInit.getDayRemark());
                for (int j = 0; j <= n; j++) {
                    row.getCell(j).setCellStyle(cellStyle);
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
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }

}
