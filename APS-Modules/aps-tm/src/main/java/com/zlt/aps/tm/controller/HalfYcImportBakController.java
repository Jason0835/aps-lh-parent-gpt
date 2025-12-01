package com.zlt.aps.tm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.service.TcCurlRollService;
import com.zlt.aps.tc.service.TcScheduleResultService;
import com.zlt.aps.tm.api.domain.entity.HalfYcImportBak;
import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.HalfYcImportBakExportVo;
import com.zlt.aps.tm.mapper.HalfYcImportBakEntityMapper;
import com.zlt.aps.tm.service.IHalfYcImportBakService;
import com.zlt.aps.tm.service.TmCurlRollService;
import com.zlt.aps.tm.service.TmScheduleResultService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：HalfYcImportBakController.java
 * 描    述：线下计划导入 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-26
 */
@Slf4j
@Api(tags = "线下计划导入")
@RestController
@RequestMapping("/halfYcImportBak")
public class HalfYcImportBakController extends AbstractDocBizController<HalfYcImportBak> {

    @Autowired
    private IHalfYcImportBakService halfYcImportBakService;

    @Autowired
    private HalfYcImportBakEntityMapper entityMapper;

    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    private final List<String> notExistFieldNameList = Arrays.asList("serialVersionUID", "id", "searchValue", "createBy", "createTime", "updateBy", "updateTime", "remark", "isDelete", "params", "rowState");

    /**
     * 查询线下计划导入列表
     */
    @RequiresPermissions("tm:halfYcImportBak:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody HalfYcImportBak queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.halfYcImportBak.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions("tm:halfYcImportBak:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody HalfYcImportBak billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.halfYcImportBak.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("tm:halfYcImportBak:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取线下计划导入详细信息
     */
    @RequiresPermissions("tm:halfYcImportBak:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public HalfYcImportBak getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入线下计划导入数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.halfYcImportBak.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<HalfYcImportBak> list = this.importExcel("", is, 4, 5502);
        AjaxResult ajaxResult = halfYcImportBakService.importData(list);
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    public List<HalfYcImportBak> importExcel(String sheetName, InputStream is, Integer dataStartRowNum, Integer lastRowNum) throws Exception {
        // 设置更低的压缩率阈值（例如 0.005），避免Zip bomb detected! 报错
        ZipSecureFile.setMinInflateRatio(0.005);

        Workbook wb = WorkbookFactory.create(is);
        List<HalfYcImportBak> list = new LinkedList<>();
        Sheet sheet = null;
        if (StringUtils.isNotEmpty(sheetName)) {
            sheet = wb.getSheet(sheetName);
        } else {
            sheet = wb.getSheetAt(0);
        }

        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        } else {

            List<Field> classField = getClassField(HalfYcImportBak.class);
            classField = classField.stream().filter(item -> !notExistFieldNameList.contains(item.getName())).collect(Collectors.toList());

            int scheduleDateRowNum = 0;
            for (int i = 0; i < sheet.getLastRowNum(); i++) {
                Object firstDateCellValue = getCellValue(sheet.getRow(i), 0);
                if (firstDateCellValue instanceof Date) {
                    scheduleDateRowNum = i;
                    break;
                }
            }

            Object scheduleDate = getCellValue(sheet.getRow(scheduleDateRowNum), 0);
            for (int rowNum = dataStartRowNum; rowNum <= lastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                HalfYcImportBak halfYcImportBak = new HalfYcImportBak();
                for (int i = 0; i < classField.size(); i++) {
                    Field field = classField.get(i);
                    String fieldName = field.getName();
                    Object cellValue = getCellValue(row, i);
                    if (cellValue != null && StringUtils.isNotBlank(cellValue.toString()) && !"scheduleDate".equals(fieldName)) {
                        if (field.getType() == Integer.class) {
                            try {
                                cellValue = Integer.parseInt(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        } else if (field.getType() == Double.class) {
                            try {
                                cellValue = Double.parseDouble(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        }
                        ReflectUtils.setFieldValue(halfYcImportBak, fieldName, cellValue);
                    }
                }
                ReflectUtils.setFieldValue(halfYcImportBak, "scheduleDate", scheduleDate);
                list.add(halfYcImportBak);
            }
            return list;
        }
    }

    public Object getCellValue(Row row, int column) {
        if (row == null) {
            return row;
        } else {
            Object val = "";

            try {
                Cell cell = row.getCell(column);
                if (StringUtils.isNotNull(cell)) {
                    if (cell.getCellType() != CellType.NUMERIC && cell.getCellType() != CellType.FORMULA) {
                        if (cell.getCellType() == CellType.STRING) {
                            val = cell.getStringCellValue();
                        } else if (cell.getCellType() == CellType.BOOLEAN) {
                            val = cell.getBooleanCellValue();
                        } else if (cell.getCellType() == CellType.ERROR) {
                            val = cell.getErrorCellValue();
                        }
                    } else {
                        val = cell.getNumericCellValue();
                        if (DateUtil.isCellDateFormatted(cell)) {
                            val = DateUtils.getJavaDate((Double) val, TimeZone.getDefault());
                        } else if ((Double) val % 1.0 != 0.0) {
                            val = new BigDecimal(val.toString());
                        } else {
                            val = (new DecimalFormat("0")).format(val);
                        }
                    }
                }

                return val;
            } catch (Exception var5) {
                return val;
            }
        }
    }

    public List<Field> getClassField(Class<? super HalfYcImportBak> tClass) {
        List<Field> tempFields = new ArrayList<>();

        while (tClass != null) {
            tempFields.addAll(Arrays.asList(tClass.getDeclaredFields()));
            tClass = tClass.getSuperclass();
            if (StringUtils.equals(tClass.getSimpleName(), BaseEntity.class.getSimpleName())) {
                break;
            }
        }

        return tempFields;
    }

    /**
     * 将排程数据导出到文件
     *
     * @param importContext 导入上下文
     * @return 结果
     */
    @Log(title = "ui.data.column.halfYcImportBak.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("将排程数据导出到文件")
    @PostMapping("/importExcelToListAndExport")
    public byte[] importExcelToListAndExport(@RequestBody ImportContext importContext, HttpServletResponse response) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        String fileName = I18nUtil.getMessage("ui.data.column.halfYcImportBak.modelName");
        Workbook workbook = this.importExcelToListAndExport("", is, response, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    public Workbook importExcelToListAndExport(String sheetName, InputStream is, HttpServletResponse response, String fileName) throws Exception {
        // 设置更低的压缩率阈值（例如 0.005），避免Zip bomb detected! 报错
        ZipSecureFile.setMinInflateRatio(0.005);

        Workbook wb = WorkbookFactory.create(is);
        List<HalfYcImportBak> list = new LinkedList<>();
        List<HalfYcImportBak> nextDayList = new LinkedList<>();
        Sheet sheet = null;
        if (StringUtils.isNotEmpty(sheetName)) {
            sheet = wb.getSheet(sheetName);
        } else {
            sheet = wb.getSheetAt(0);
        }

        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        } else {
            int scheduleDateRowNum = 0;
            for (int i = 0; i < sheet.getLastRowNum(); i++) {
                Object firstDateCellValue = getCellValue(sheet.getRow(i), 0);
                if (firstDateCellValue instanceof Date) {
                    scheduleDateRowNum = i;
                    break;
                }
            }

            int dataStartRowNum = 3 + scheduleDateRowNum;
            int lastRowNum = 5501 + scheduleDateRowNum;

            // 查询成型计划
            int dataNum = 5505 + scheduleDateRowNum;
            int nextDayDateRowNum = dataNum + 1;
            String scheduleDateStr = DateFormatUtils.format((Date) getCellValue(sheet.getRow(nextDayDateRowNum), 0), "yyyy-MM-dd");
            List<HalfYcImportBakExportVo> cxPlanQtyList = entityMapper.selectCxScheduleResult(scheduleDateStr);
            Map<String, Integer> cxPlanQtyMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(cxPlanQtyList)) {
                cxPlanQtyMap = cxPlanQtyList.stream().collect(Collectors.toMap(HalfYcImportBakExportVo::getCode, HalfYcImportBakExportVo::getCxPlanQty));
            }
            // 先遍历所有的规格代码，重算计划用量，再写值到对应的计划量栏位
            for (int rowNum = dataStartRowNum; rowNum <= lastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                String cx3CellValue = getCellValue(row, 2).toString();
                if (cxPlanQtyMap.containsKey(cx3CellValue)) {
                    Integer cxTotalPlan = cxPlanQtyMap.get(cx3CellValue);
                    row.getCell(7).setBlank();
                    row.getCell(7).setCellValue(cxTotalPlan);
                }
            }
            for (int rowNum = dataStartRowNum + dataNum; rowNum <= lastRowNum + dataNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                String cx3CellValue = getCellValue(row, 2).toString();
                if (cxPlanQtyMap.containsKey(cx3CellValue)) {
                    Integer cxTotalPlan = cxPlanQtyMap.get(cx3CellValue);
                    row.getCell(7).setBlank();
                    row.getCell(7).setCellValue(cxTotalPlan);
                }
            }
            // 重算计划用量公式
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();

            List<Field> classField = getClassField(HalfYcImportBak.class);
            classField = classField.stream().filter(item -> !notExistFieldNameList.contains(item.getName())).collect(Collectors.toList());

            List<String> setFieldNameNight = Arrays.asList("tm10", "tm11", "tm18", "tm19", "tc10", "tc11", "tc14", "tc15");
            List<String> setFieldNameDay = Arrays.asList("tm8", "tm9", "tm16", "tm17", "tc8", "tc9", "tc12", "tc13");

            Object scheduleDateCellValue = getCellValue(sheet.getRow(scheduleDateRowNum), 0);
            for (int rowNum = dataStartRowNum; rowNum <= lastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                HalfYcImportBak halfYcImportBak = new HalfYcImportBak();
                for (int i = 0; i < classField.size(); i++) {
                    Field field = classField.get(i);
                    String fieldName = field.getName();
                    if (setFieldNameNight.contains(fieldName)) {
                        continue;
                    }
                    Object cellValue = getCellValue(row, i);
                    if (StringUtils.isNotBlank(cellValue.toString()) && !"scheduleDate".equals(fieldName)) {
                        if (field.getType() == Integer.class) {
                            try {
                                cellValue = Integer.parseInt(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        } else if (field.getType() == Double.class) {
                            try {
                                cellValue = Double.parseDouble(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        }
                        if (field.getType() == cellValue.getClass()) {
                            ReflectUtils.setFieldValue(halfYcImportBak, fieldName, cellValue);
                        }
                    }
                }
                ReflectUtils.setFieldValue(halfYcImportBak, "scheduleDate", scheduleDateCellValue);
                list.add(halfYcImportBak);
            }

            int nextDayDataStartRowNum = 5509 + scheduleDateRowNum;
            int nextDayLastRowNum = 11007 + scheduleDateRowNum;
            scheduleDateCellValue = getCellValue(sheet.getRow(nextDayDateRowNum), 0);
            for (int rowNum = nextDayDataStartRowNum; rowNum <= nextDayLastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }
                HalfYcImportBak halfYcImportBak = new HalfYcImportBak();
                for (int i = 0; i < classField.size(); i++) {
                    Field field = classField.get(i);
                    String fieldName = field.getName();
                    if (setFieldNameDay.contains(fieldName)) {
                        continue;
                    }
                    Object cellValue = getCellValue(row, i);
                    if (StringUtils.isNotBlank(cellValue.toString()) && !"scheduleDate".equals(fieldName)) {
                        if (field.getType() == Integer.class) {
                            try {
                                cellValue = Integer.parseInt(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        } else if (field.getType() == Double.class) {
                            try {
                                cellValue = Double.parseDouble(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        }
                        if (field.getType() == cellValue.getClass()) {
                            ReflectUtils.setFieldValue(halfYcImportBak, fieldName, cellValue);
                        }
                    }
                }
                ReflectUtils.setFieldValue(halfYcImportBak, "scheduleDate", scheduleDateCellValue);
                nextDayList.add(halfYcImportBak);
            }

            Date scheduleDate = null;
            if (CollectionUtils.isNotEmpty(nextDayList)) {
                scheduleDate = nextDayList.get(0).getScheduleDate();
            }
            halfYcImportBakService.exportDataToList(list, nextDayList, scheduleDate);
//            nextDayList = halfYcImportBakService.exportDataToList(nextDayList, scheduleDate, OpenMachineClassEnums.CLASS_THREE.getClassIndex());

            for (int i = 0; i < list.size(); i++) {
                HalfYcImportBak halfYcImportBak = list.get(i);
                Row row = sheet.getRow(i + dataStartRowNum);
                for (int j = 0; j < classField.size(); j++) {
                    Field field = classField.get(j);
                    String fieldName = field.getName();
                    if (setFieldNameNight.contains(fieldName)) {
                        Object fieldValue = ReflectUtils.getFieldValue(halfYcImportBak, fieldName);
                        if (Objects.nonNull(fieldValue)) {
                            double cellValue = Double.parseDouble(fieldValue.toString());
                            Cell cell = row.getCell(j);
                            // 原有单元格有值，重新写入值可能未覆盖，这里需要清空原有值
                            cell.setBlank();
                            if (cellValue > 0) {
                                cell.setCellValue(cellValue);
                            }
                        } else {
                            Cell cell = row.getCell(j);
                            cell.setBlank();
                        }
                    }
                }
            }


            for (int i = 0; i < nextDayList.size(); i++) {
                HalfYcImportBak halfYcImportBak = nextDayList.get(i);
                Row row = sheet.getRow(i + nextDayDataStartRowNum);
                for (int j = 0; j < classField.size(); j++) {
                    Field field = classField.get(j);
                    String fieldName = field.getName();
                    if (setFieldNameDay.contains(fieldName)) {
                        Object fieldValue = ReflectUtils.getFieldValue(halfYcImportBak, fieldName);
                        if (Objects.nonNull(fieldValue)) {
                            double cellValue = Double.parseDouble(fieldValue.toString());
                            Cell cell = row.getCell(j);
                            // 原有单元格有值，重新写入值可能未覆盖，这里需要清空原有值
                            cell.setBlank();
                            if (cellValue > 0) {
                                cell.setCellValue(cellValue);
                            }
                        } else {
                            Cell cell = row.getCell(j);
                            cell.setBlank();
                        }
                    }
                }
            }

            // 重算求和公式
            // 清除所有公式计算缓存结果
            evaluator.clearAllCachedResultValues();
            evaluator.evaluateAll();
        }
        return wb;
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("tm:halfYcImportBak:export")
    @Log(title = "线下计划导入", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody HalfYcImportBak queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<HalfYcImportBak> listExportData(HalfYcImportBak obj) {
        QueryWrapper<HalfYcImportBak> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return null;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<HalfYcImportBak> queryWrapper, HalfYcImportBak queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx1")), "CX1", queryVO.getFieldValueByFieldName("cx1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx2")), "CX2", queryVO.getFieldValueByFieldName("cx2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx3")), "CX3", queryVO.getFieldValueByFieldName("cx3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx4")), "CX4", queryVO.getFieldValueByFieldName("cx4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx5")), "CX5", queryVO.getFieldValueByFieldName("cx5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx6")), "CX6", queryVO.getFieldValueByFieldName("cx6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx7")), "CX7", queryVO.getFieldValueByFieldName("cx7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx8")), "CX8", queryVO.getFieldValueByFieldName("cx8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx9")), "CX9", queryVO.getFieldValueByFieldName("cx9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm1")), "TM1", queryVO.getFieldValueByFieldName("tm1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm2")), "TM2", queryVO.getFieldValueByFieldName("tm2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm3")), "TM3", queryVO.getFieldValueByFieldName("tm3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm4")), "TM4", queryVO.getFieldValueByFieldName("tm4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm5")), "TM5", queryVO.getFieldValueByFieldName("tm5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm6")), "TM6", queryVO.getFieldValueByFieldName("tm6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm7")), "TM7", queryVO.getFieldValueByFieldName("tm7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm8")), "TM8", queryVO.getFieldValueByFieldName("tm8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm9")), "TM9", queryVO.getFieldValueByFieldName("tm9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm10")), "TM10", queryVO.getFieldValueByFieldName("tm10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm11")), "TM11", queryVO.getFieldValueByFieldName("tm11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm12")), "TM12", queryVO.getFieldValueByFieldName("tm12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm13")), "TM13", queryVO.getFieldValueByFieldName("tm13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm14")), "TM14", queryVO.getFieldValueByFieldName("tm14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm15")), "TM15", queryVO.getFieldValueByFieldName("tm15"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm16")), "TM16", queryVO.getFieldValueByFieldName("tm16"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm17")), "TM17", queryVO.getFieldValueByFieldName("tm17"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm18")), "TM18", queryVO.getFieldValueByFieldName("tm18"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm19")), "TM19", queryVO.getFieldValueByFieldName("tm19"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm20")), "TM20", queryVO.getFieldValueByFieldName("tm20"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm21")), "TM21", queryVO.getFieldValueByFieldName("tm21"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm22")), "TM22", queryVO.getFieldValueByFieldName("tm22"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm23")), "TM23", queryVO.getFieldValueByFieldName("tm23"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm24")), "TM24", queryVO.getFieldValueByFieldName("tm24"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm25")), "TM25", queryVO.getFieldValueByFieldName("tm25"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm26")), "TM26", queryVO.getFieldValueByFieldName("tm26"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tm27")), "TM27", queryVO.getFieldValueByFieldName("tm27"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc1")), "TC1", queryVO.getFieldValueByFieldName("tc1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc2")), "TC2", queryVO.getFieldValueByFieldName("tc2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc3")), "TC3", queryVO.getFieldValueByFieldName("tc3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc4")), "TC4", queryVO.getFieldValueByFieldName("tc4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc5")), "TC5", queryVO.getFieldValueByFieldName("tc5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc6")), "TC6", queryVO.getFieldValueByFieldName("tc6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc7")), "TC7", queryVO.getFieldValueByFieldName("tc7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc8")), "TC8", queryVO.getFieldValueByFieldName("tc8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc9")), "TC9", queryVO.getFieldValueByFieldName("tc9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc10")), "TC10", queryVO.getFieldValueByFieldName("tc10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc11")), "TC11", queryVO.getFieldValueByFieldName("tc11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc12")), "TC12", queryVO.getFieldValueByFieldName("tc12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc13")), "TC13", queryVO.getFieldValueByFieldName("tc13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc14")), "TC14", queryVO.getFieldValueByFieldName("tc14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc15")), "TC15", queryVO.getFieldValueByFieldName("tc15"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc16")), "TC16", queryVO.getFieldValueByFieldName("tc16"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc17")), "TC17", queryVO.getFieldValueByFieldName("tc17"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc18")), "TC18", queryVO.getFieldValueByFieldName("tc18"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc19")), "TC19", queryVO.getFieldValueByFieldName("tc19"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc20")), "TC20", queryVO.getFieldValueByFieldName("tc20"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tc21")), "TC21", queryVO.getFieldValueByFieldName("tc21"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
    }

    @Override
    protected String getTypeCode() {
        return "TM0099";
    }

    @Autowired
    private TmScheduleResultService tmScheduleResultService;

    @Autowired
    private TcScheduleResultService tcScheduleResultService;

    @Resource
    private TmCurlRollService tmCurlRollService;

    @Resource
    private TcCurlRollService tcCurlRollService;

    /**
     * 导入线下模板，并覆盖原有排程数据
     *
     * @param importContext sheet名称
     * @return 结果
     * @throws Exception 异常
     */
    @Log(title = "ui.data.column.halfYcImportBak.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入线下模板调整")
    @PostMapping("/import4OfflineTemplate")
    public AjaxResult import4OfflineTemplate(@RequestBody ImportContext importContext, HttpServletResponse response) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        String sheetName = "";
        // 设置更低的压缩率阈值（例如 0.005），避免Zip bomb detected! 报错
        ZipSecureFile.setMinInflateRatio(0.005);

        Workbook wb = WorkbookFactory.create(is);
        List<HalfYcImportBak> list = new LinkedList<>();
        List<HalfYcImportBak> nextDayList = new LinkedList<>();
        Sheet sheet = null;
        if (StringUtils.isNotEmpty(sheetName)) {
            sheet = wb.getSheet(sheetName);
        } else {
            sheet = wb.getSheetAt(0);
        }

        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        } else {

            int scheduleDateRowNum = 0;
            for (int i = 0; i < sheet.getLastRowNum(); i++) {
                Object firstDateCellValue = getCellValue(sheet.getRow(i), 0);
                if (firstDateCellValue instanceof Date) {
                    scheduleDateRowNum = i;
                    break;
                }
            }

            Map<Integer, HalfYcImportBak> importBakHashMap = new HashMap<>(16);
            int dataStartRowNum = 3 + scheduleDateRowNum;
            int lastRowNum = 5501 + scheduleDateRowNum;
            int dataNum = 5505 + scheduleDateRowNum;
            int nextDayDateRowNum = dataNum + 1;

            List<Field> classField = getClassField(HalfYcImportBak.class);
            classField = classField.stream().filter(item -> !notExistFieldNameList.contains(item.getName())).collect(Collectors.toList());

            Object scheduleDateCellValue = getCellValue(sheet.getRow(scheduleDateRowNum), 0);
            for (int rowNum = dataStartRowNum; rowNum <= lastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                HalfYcImportBak halfYcImportBak = new HalfYcImportBak();
                for (int i = 0; i < classField.size(); i++) {
                    Field field = classField.get(i);
                    String fieldName = field.getName();
                    Object cellValue = getCellValue(row, i);
                    if (StringUtils.isNotBlank(cellValue.toString()) && !"scheduleDate".equals(fieldName)) {
                        if (field.getType() == Integer.class) {
                            try {
                                cellValue = Integer.parseInt(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        } else if (field.getType() == Double.class) {
                            try {
                                cellValue = Double.parseDouble(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        }
                        ReflectUtils.setFieldValue(halfYcImportBak, fieldName, cellValue);
                    }
                }
                ReflectUtils.setFieldValue(halfYcImportBak, "scheduleDate", scheduleDateCellValue);
//                list.add(halfYcImportBak);
                Double tm11 = ObjectUtils.defaultIfNull(halfYcImportBak.getTm11(), 0D);
                Double tm19 = ObjectUtils.defaultIfNull(halfYcImportBak.getTm19(), 0D);
                Double tc11 = ObjectUtils.defaultIfNull(halfYcImportBak.getTc11(), 0D);
                Double tc15 = ObjectUtils.defaultIfNull(halfYcImportBak.getTc15(), 0D);
                if (tm11 > 0 || tm19 > 0 || tc11 > 0 || tc15 > 0) {
                    importBakHashMap.put(rowNum - dataStartRowNum, halfYcImportBak);
                }
            }

            int nextDayDataStartRowNum = 5509 + scheduleDateRowNum;
            int nextDayLastRowNum = 11007 + scheduleDateRowNum;
            scheduleDateCellValue = getCellValue(sheet.getRow(nextDayDateRowNum), 0);
            for (int rowNum = nextDayDataStartRowNum; rowNum <= nextDayLastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }
                HalfYcImportBak halfYcImportBak = new HalfYcImportBak();
                for (int i = 0; i < classField.size(); i++) {
                    Field field = classField.get(i);
                    String fieldName = field.getName();
                    Object cellValue = getCellValue(row, i);
                    if (StringUtils.isNotBlank(cellValue.toString()) && !"scheduleDate".equals(fieldName)) {
                        if (field.getType() == Integer.class) {
                            try {
                                cellValue = Integer.parseInt(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        } else if (field.getType() == Double.class) {
                            try {
                                cellValue = Double.parseDouble(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        }
                        ReflectUtils.setFieldValue(halfYcImportBak, fieldName, cellValue);
                    }
                }
                ReflectUtils.setFieldValue(halfYcImportBak, "scheduleDate", scheduleDateCellValue);
//                nextDayList.add(halfYcImportBak);
                Double tm9 = ObjectUtils.defaultIfNull(halfYcImportBak.getTm9(), 0D);
                Double tm17 = ObjectUtils.defaultIfNull(halfYcImportBak.getTm17(), 0D);
                Double tc9 = ObjectUtils.defaultIfNull(halfYcImportBak.getTc9(), 0D);
                Double tc13 = ObjectUtils.defaultIfNull(halfYcImportBak.getTc13(), 0D);
                int key = rowNum - nextDayDataStartRowNum;
                if (importBakHashMap.containsKey(key)) {
                    halfYcImportBak = importBakHashMap.get(key);
                    halfYcImportBak.setTm9(tm9);
                    halfYcImportBak.setTm17(tm17);
                    halfYcImportBak.setTc9(tc9);
                    halfYcImportBak.setTc13(tc13);
                    ReflectUtils.setFieldValue(halfYcImportBak, "scheduleDate", scheduleDateCellValue);
                }
                if (tm9 > 0 || tm17 > 0 || tc9 > 0 || tc13 > 0) {
                    importBakHashMap.put(key, halfYcImportBak);
                }
            }

            Date scheduleDate = null;
            List<HalfYcImportBak> halfYcImportBakList = new ArrayList<>(importBakHashMap.values());
            if (CollectionUtils.isNotEmpty(halfYcImportBakList)) {
                scheduleDate = halfYcImportBakList.get(0).getScheduleDate();
            }
            List<TmScheduleResult> tmScheduleResultList = new ArrayList<>();
            List<TcScheduleResult> tcScheduleResultList = new ArrayList<>();

            // 查询胎面卷曲长度
            List<TmCurlRoll> tmCurlRollList = tmCurlRollService.listCurlRoll(new TmCurlRoll());
            Map<String, BigDecimal> tmCurlLengthMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(tmCurlRollList)) {
                tmCurlLengthMap = tmCurlRollList.stream().collect(Collectors.toMap(TmCurlRoll::getTreadCode, TmCurlRoll::getCurlLength));
            }

            // 查询胎侧卷曲长度
            List<TcCurlRoll> tcCurlRollList = tcCurlRollService.listCurlRoll(new TcCurlRoll());
            Map<String, BigDecimal> tcCurlLengthMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(tcCurlRollList)) {
                tcCurlLengthMap = tcCurlRollList.stream().collect(Collectors.toMap(TcCurlRoll::getSidewallCode, TcCurlRoll::getCurlLength));
            }

            List<String> embryoCodeList = halfYcImportBakList.stream().map(HalfYcImportBak::getCx3).collect(Collectors.toList());

            // 查询施工，赋值胶料、口型
            List<EngineConstructionInfo> constructionInfoList = tmScheduleResultService.listConstruction(embryoCodeList, "1");
            Map<String, EngineConstructionInfo> constructionInfoMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(constructionInfoList)) {
                constructionInfoMap = constructionInfoList.stream().collect(Collectors.toMap(EngineConstructionInfo::getEmbryoCode, Function.identity()));
            }

            // 导入操作
            for (HalfYcImportBak halfYcImportBak : halfYcImportBakList) {
                TmScheduleResult tmScheduleResult = new TmScheduleResult();
                tmScheduleResult.setScheduleDate(scheduleDate);
                String cx3 = halfYcImportBak.getCx3();
                String suffix = "";
                // 胎面
                if (StringUtils.isNotBlank(cx3) && cx3.contains("VM")) {
                    cx3 = cx3.substring(0, 4);
                    suffix = "(VMI)";
                }
                EngineConstructionInfo constructionInfo = null;
                if (constructionInfoMap.containsKey(cx3)) {
                    constructionInfo = constructionInfoMap.get(cx3);
                }
                tmScheduleResult.setTreadCode(cx3);
                // 赋值胶料、口型
                if (constructionInfo != null) {
                    tmScheduleResult.setGlueCode(constructionInfo.getTreadRubberCategory());
                    tmScheduleResult.setMouthPlateCode(constructionInfo.getTreadMouthPlate());
                }
                Double tm9 = ObjectUtils.defaultIfNull(halfYcImportBak.getTm9(), 0D);
                Double tm11 = ObjectUtils.defaultIfNull(halfYcImportBak.getTm11(), 0D);
                Double tm17 = ObjectUtils.defaultIfNull(halfYcImportBak.getTm17(), 0D);
                Double tm19 = ObjectUtils.defaultIfNull(halfYcImportBak.getTm19(), 0D);
                // 将卷转成米
                BigDecimal tmCurlLength = tmCurlLengthMap.getOrDefault(cx3, BigDecimal.valueOf(85));
                if (tm9 > 0 || tm11 > 0) {
                    tmScheduleResult.setMachineId("四复合3线");
                    tmScheduleResult.setDayPlanQty(Math.ceil(tm11 * tmCurlLength.doubleValue()));
                    tmScheduleResult.setNightPlanQty(Math.ceil(tm9 * tmCurlLength.doubleValue()));
                } else if (tm17 > 0 || tm19 > 0) {
                    tmScheduleResult.setMachineId("五复合4线");
                    tmScheduleResult.setDayPlanQty(Math.ceil(tm19 * tmCurlLength.doubleValue()));
                    tmScheduleResult.setNightPlanQty(Math.ceil(tm17 * tmCurlLength.doubleValue()));
                }
                if (StringUtils.isNotBlank(tmScheduleResult.getMachineId())) {
                    tmScheduleResultList.add(tmScheduleResult);
                }

                TcScheduleResult tcScheduleResult = new TcScheduleResult();
                tcScheduleResult.setScheduleDate(scheduleDate);
                String tc1 = StringUtils.defaultIfBlank(halfYcImportBak.getTc1(), "");
                if (StringUtils.isNotBlank(tc1)) {
                    tc1 = tc1.substring(0, 6) + suffix;
                }
                // 赋值胶料、口型
                if (constructionInfo != null) {
                    tcScheduleResult.setGlueCode(constructionInfo.getSidewallRubber());
                    tcScheduleResult.setMouthPlateCode(constructionInfo.getSidewallMouthPlate());
                }
                tcScheduleResult.setSidewallCode(tc1);
                Double tc9 = ObjectUtils.defaultIfNull(halfYcImportBak.getTc9(), 0D);
                Double tc11 = ObjectUtils.defaultIfNull(halfYcImportBak.getTc11(), 0D);
                Double tc13 = ObjectUtils.defaultIfNull(halfYcImportBak.getTc13(), 0D);
                Double tc15 = ObjectUtils.defaultIfNull(halfYcImportBak.getTc15(), 0D);
                // 将卷转成米
                BigDecimal tcCurlLength = tcCurlLengthMap.getOrDefault(tc1, BigDecimal.valueOf(50));
                if (tc9 > 0 || tc11 > 0) {
                    tcScheduleResult.setMachineId("三复合1号线");
                    tcScheduleResult.setDayPlanQty(Math.ceil(tc11 * tcCurlLength.doubleValue()));
                    tcScheduleResult.setNightPlanQty(Math.ceil(tc9 * tcCurlLength.doubleValue()));
                } else if (tc13 > 0 || tc15 > 0) {
                    tcScheduleResult.setMachineId("三复合2号线");
                    tcScheduleResult.setDayPlanQty(Math.ceil(tc15 * tcCurlLength.doubleValue()));
                    tcScheduleResult.setNightPlanQty(Math.ceil(tc13 * tcCurlLength.doubleValue()));
                }
                if (StringUtils.isNotBlank(tcScheduleResult.getMachineId())) {
                    tcScheduleResultList.add(tcScheduleResult);
                }
            }

            String scheduleDateStr = DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate);
            Map<String, List<TmScheduleResult>> tmScheduleMachineMap = tmScheduleResultList.stream().collect(Collectors.groupingBy(TmScheduleResult::getMachineId));
            Set<Map.Entry<String, List<TmScheduleResult>>> tmEntrySet = tmScheduleMachineMap.entrySet();
            for (Map.Entry<String, List<TmScheduleResult>> entry : tmEntrySet) {
                List<TmScheduleResult> value = entry.getValue();
                long dayProduceOrder = 1;
                long nightProduceOrder = 1;
                for (TmScheduleResult scheduleResult : value) {
                    Double dayPlanQty = scheduleResult.getDayPlanQty();
                    Long dayOrder = scheduleResult.getDayProduceOrder();
                    if (dayOrder == null && dayPlanQty != null && dayPlanQty > 0) {
                        scheduleResult.setDayProduceOrder(dayProduceOrder);
                        dayProduceOrder++;
                    }
                    Double nightPlanQty = scheduleResult.getNightPlanQty();
                    Long nightOrder = scheduleResult.getDayProduceOrder();
                    if (nightOrder == null && nightPlanQty != null && nightPlanQty > 0) {
                        scheduleResult.setNightProduceOrder(nightProduceOrder);
                        nightProduceOrder++;
                    }
                }
            }
            AjaxResult tmAjaxResult = tmScheduleResultService.importData(tmScheduleResultList, importLog.getId(), scheduleDateStr);
            List<ImportErrorLog> importErrorLogs = new ArrayList<>();
            if (tmAjaxResult.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
                List<ImportErrorLog> tmImportErrorLog = (List<ImportErrorLog>) tmAjaxResult.get(AjaxResult.DATA_TAG);
                importErrorLogs.addAll(tmImportErrorLog);
            }
            importLog.setRowCount(tmScheduleResultList.size());
            importLog.setBeginTime(beginTime);
            Date endTime = DateUtils.getNowDate();
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
            ImportExcelUtils.updateImportLogAndFormatMsg(importLog, tmAjaxResult, this.iImportLogService);
            ImportExcelUtils.saveImportErrorLogs(tmAjaxResult, this.iImportErrorLogService);

            importLog = this.iImportLogService.add(importLog);

            Map<String, List<TcScheduleResult>> tcScheduleMachineMap = tcScheduleResultList.stream().collect(Collectors.groupingBy(TcScheduleResult::getMachineId));
            Set<Map.Entry<String, List<TcScheduleResult>>> tcEntrySet = tcScheduleMachineMap.entrySet();
            for (Map.Entry<String, List<TcScheduleResult>> entry : tcEntrySet) {
                List<TcScheduleResult> value = entry.getValue();
                long dayProduceOrder = 1;
                long nightProduceOrder = 1;
                for (TcScheduleResult scheduleResult : value) {
                    Double dayPlanQty = scheduleResult.getDayPlanQty();
                    Long dayOrder = scheduleResult.getDayProduceOrder();
                    if (dayOrder == null && dayPlanQty != null && dayPlanQty > 0) {
                        scheduleResult.setDayProduceOrder(dayProduceOrder);
                        dayProduceOrder++;
                    }
                    Double nightPlanQty = scheduleResult.getNightPlanQty();
                    Long nightOrder = scheduleResult.getDayProduceOrder();
                    if (nightOrder == null && nightPlanQty != null && nightPlanQty > 0) {
                        scheduleResult.setNightProduceOrder(nightProduceOrder);
                        nightProduceOrder++;
                    }
                }
            }
            AjaxResult tcAjaxResult = tcScheduleResultService.importData(tcScheduleResultList, importLog.getId(), scheduleDateStr);
            if (tcAjaxResult.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
                List<ImportErrorLog> tcImportErrorLog = (List<ImportErrorLog>) tcAjaxResult.get(AjaxResult.DATA_TAG);
                importErrorLogs.addAll(tcImportErrorLog);
            }
            importLog.setRowCount(tcScheduleResultList.size());
            importLog.setBeginTime(beginTime);
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
//            ImportExcelUtils.updateImportLogAndFormatMsg(importLog, tcAjaxResult, this.iImportLogService);
//            ImportExcelUtils.saveImportErrorLogs(tcAjaxResult, this.iImportErrorLogService);
            if (CollectionUtils.isNotEmpty(importErrorLogs)) {
                return AjaxResult.error("导入失败", importErrorLogs);
            }
        }
        return AjaxResult.success();
    }
}
