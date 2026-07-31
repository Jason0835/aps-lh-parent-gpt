package com.zlt.aps.xwyy.controller;

import cn.hutool.core.date.DateException;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.utils.AppUtils;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleImportDTO;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.domain.vo.XwyyScheduleResultTemplateImportVO;
import com.zlt.aps.xwyy.mapper.XwyyScheduleResultMapper;
import com.zlt.aps.xwyy.service.IXwyyScheduleResultService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Api(tags = "纤维压延排程结果")
@RestController
@RequestMapping("/xwyyScheduleResult")
public class XwyyScheduleResultController extends AbstractDocBizController<XwyyScheduleResult> {
    @Resource
    private IXwyyScheduleResultService service;
    @Resource
    private XwyyScheduleResultMapper mapper;
    @Resource
    private IImportLogService iImportLogService;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody XwyyScheduleResult query) {
        return super.list(query);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public XwyyScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.OTHER)
    @ApiOperation("自动排程")
    @PostMapping("/autoSchedule")
    public AjaxResult autoSchedule(@RequestBody XwyyScheduleResult entity) {
        return service.autoSchedule(entity);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.OTHER)
    @ApiOperation("插单")
    @PostMapping("/insert")
    public AjaxResult insert(@RequestBody XwyyScheduleResult entity) {
        return service.insert(entity);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody XwyyScheduleResult entity) {
        return service.changeMachine(entity);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("调量")
    @PostMapping("/adjustQty")
    public AjaxResult adjustQty(@RequestBody XwyyScheduleResult entity) {
        return service.adjustQty(entity);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.OTHER)
    @ApiOperation("发布")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody XwyyScheduleResult entity) {
        return service.publish(entity);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext c, @RequestParam("updateSupport") boolean u) throws Exception {
        return super.importData(c, u);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("按固定模板导入纤维压延排程结果")
    @PostMapping("/importDataByCust/{updateSupport}")
    public AjaxResult importDataByCust(@PathVariable("updateSupport") boolean updateSupport,
                                       @RequestBody XwyyScheduleImportDTO importDTO) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportContext importContext = importDTO.getImportContext();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(
                importContext.getFileBytes(), importContext.getImportFilePath(),
                importContext.getProcedureCode(), importContext.getFunctionName(),
                importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        List<XwyyScheduleResultTemplateImportVO> rows = new ArrayList<>();
        AjaxResult ajaxResult;
        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(importContext.getFileBytes()))) {
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Row dateRow = sheet.getRow(1);
            Cell scheduleDateCell = dateRow == null ? null : dateRow.getCell(13);
            Date scheduleDate = null;
            if (scheduleDateCell != null && scheduleDateCell.getCellType() == CellType.NUMERIC) {
                scheduleDate = org.apache.poi.ss.usermodel.DateUtil.getJavaDate(
                        scheduleDateCell.getNumericCellValue());
            } else if (scheduleDateCell != null) {
                String dateText = formatter.formatCellValue(scheduleDateCell, evaluator).trim();
                if (PubUtil.isNotEmpty(dateText)) {
                    scheduleDate = DateUtil.parse(dateText);
                }
            }
            if (scheduleDate == null) {
                ajaxResult = AjaxResult.error(I18nUtil.getMessage(
                        "ui.data.column.xwyyScheduleResult.importDateRequired"));
            } else {
                int[] planQuantityColumns = {10, 12, 14, 16, 18, 21, 23, 25};
                for (int rowIndex = 3; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    String bigRollCode = formatter.formatCellValue(
                            row.getCell(0), evaluator).trim();
                    if (!PubUtil.isNotEmpty(bigRollCode) || "合计".equals(bigRollCode)) {
                        break;
                    }
                    XwyyScheduleResultTemplateImportVO item =
                            new XwyyScheduleResultTemplateImportVO();
                    item.setExcelRowNum(rowIndex + 1);
                    item.setBigRollCode(bigRollCode);
                    for (int classIndex = 1; classIndex <= 8; classIndex++) {
                        String value = formatter.formatCellValue(
                                row.getCell(planQuantityColumns[classIndex - 1]), evaluator)
                                .replace(",", "").trim();
                        BigDecimal quantity = PubUtil.isNotEmpty(value)
                                ? new BigDecimal(value) : null;
                        item.setFieldValueByFieldName(
                                String.format("class%dPlanQty", classIndex), quantity);
                    }
                    rows.add(item);
                }
                XwyyScheduleResult condition = importDTO.getScheduleResult();
                if (condition != null) {
                    condition.setScheduleDate(DateUtil.beginOfDay(scheduleDate));
                }
                ajaxResult = this.service.importScheduleTemplate(
                        rows, condition, updateSupport);
            }
        } catch (DateException exception) {
            ajaxResult = AjaxResult.error(I18nUtil.getMessage(
                    "ui.data.column.xwyyScheduleResult.importDateRequired"));
        } catch (NumberFormatException exception) {
            ajaxResult = AjaxResult.error(I18nUtil.getMessage(
                    "ui.data.column.xwyyScheduleResult.importNumberInvalid"));
        }
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(rows.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(
                importLog, ajaxResult, this.iImportLogService);
        return ajaxResult;
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody XwyyScheduleResult query, @PathVariable("fileName") String fileName, HttpServletResponse r) throws IOException {
        return this.service.exportData(this.listExportData(query), query);
    }

    @Override
    protected List<XwyyScheduleResult> listExportData(XwyyScheduleResult output) {
        QueryWrapper<XwyyScheduleResult> w = new QueryWrapper<>();
        builderCondition(w, output);
        List<XwyyScheduleResult> l = mapper.selectList(w);
        AppUtils.formatData(l, getQueryFormulas());
        return l;
    }

    @Override
    protected IDocService getDocService() {
        return service;
    }

    @Override
    protected void builderCondition(QueryWrapper<XwyyScheduleResult> qw, XwyyScheduleResult vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getScheduleDate() != null, "SCHEDULE_DATE", vo.getScheduleDate());
        qw.like(PubUtil.isNotEmpty(vo.getBigRollCode()), "BIG_ROLL_CODE", vo.getBigRollCode());
        qw.like(PubUtil.isNotEmpty(vo.getMachineId()), "MACHINE_ID", vo.getMachineId());
        qw.eq(PubUtil.isNotEmpty(vo.getIsRelease()), "IS_RELEASE", vo.getIsRelease());
        qw.eq(PubUtil.isNotEmpty(vo.getBatchNo()), "BATCH_NO", vo.getBatchNo());
        qw.eq(PubUtil.isNotEmpty(vo.getOrderNo()), "ORDER_NO", vo.getOrderNo());
    }

    @Override
    protected String getTypeCode() {
        return "XWYY_SCHEDULE_RESULT";
    }

    @Override
    protected String getOrderBy() {
        return "SCHEDULE_DATE desc, BIG_ROLL_CODE asc";
    }
}
