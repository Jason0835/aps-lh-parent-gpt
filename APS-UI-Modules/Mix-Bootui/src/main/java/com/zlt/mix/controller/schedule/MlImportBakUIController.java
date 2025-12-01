package com.zlt.mix.controller.schedule;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.schedule.api.domain.vo.MlImportBak;
import com.zlt.mix.schedule.api.service.IMlImportBakRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MlImportBakUIController.java
 * 描    述：密炼线下计划操作功能 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-09-05
 */
@Slf4j
@Api(tags = "密炼线下计划操作功能")
@Controller
@RequestMapping("/schedule/mlImportBak")
public class MlImportBakUIController extends BaseUIController<MlImportBak> {

    private final String prefix = "mix/schedule/mlImportBak";
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;
    @Autowired
    private IMlImportBakRemoteService iMlImportBakService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("schedule:mlImportBak:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mlImportBak";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mlImportBak", new MlImportBak());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mlImportBak", iMlImportBakService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("schedule:mlImportBak:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MlImportBak mlImportBak) {
        return iMlImportBakService.list(mlImportBak);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("schedule:mlImportBak:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MlImportBak mlImportBak) {
        if (UserConstants.NOT_UNIQUE.equals(iMlImportBakService.checkUnique(mlImportBak))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mlImportBak.checkUnique"));
        }

        return iMlImportBakService.save(mlImportBak);
    }

    /**
     * 删除密炼线下计划操作功能
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("schedule:mlImportBak:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMlImportBakService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验密炼线下计划操作功能唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MlImportBak mlImportBak) {
        return iMlImportBakService.checkUnique(mlImportBak);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.no.export.sheetName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MlImportBak> util = new ExcelUtil<>(MlImportBak.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MlImportBak entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMlImportBakService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importDataOld"})
    @ResponseBody
    @ApiOperation("数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iMlImportBakService.importData(context, false);
        return ajaxResult;
    }

    @RequiresPermissions("schedule:mlImportBak:importOfflineData")
    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("线下排程数据导入")
    public AjaxResult importOfflineData(@RequestPart("file") MultipartFile file, Date scheduleDate, String mixArea, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_MIX,
                I18nUtil.getMessage("schedule.glueScheduleResult.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        List<MlImportBak> list = genMlImportBakList(in, scheduleDate, mixArea);
        //导入数据
        AjaxResult ajaxResult = iMlImportBakService.importOfflineData(list, DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate), mixArea, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    private List<MlImportBak> genMlImportBakList(InputStream in, Date scheduleDate, String mixArea) throws Exception {
        List<MlImportBak> list = new ArrayList<>();
        if (in == null) {
            return list;
        }
        Workbook workbook = WorkbookFactory.create(in);
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        }
        int rows = sheet.getPhysicalNumberOfRows();

        if (rows > 2) {

            int startCellNum = 2;
            int endCellNum = 39;

            List<Field> classField = getClassField(MlImportBak.class);
            List<Field> baseEntityField = getClassField(BaseEntity.class);
            List<String> filterNameList = baseEntityField.stream().map(Field::getName).collect(Collectors.toList());
            for (int i = 0; i < classField.size(); i++) {
                Field field = classField.get(i);
                String fieldName = field.getName();
                if (filterNameList.contains(fieldName)) {
                    classField.remove(field);
                    i--;
                }
            }
            for (int rowNum = startCellNum; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }
                MlImportBak mlImportBak = new MlImportBak();
                for (int i = 0; i < classField.size(); i++) {
                    Field field = classField.get(i);
                    String fieldName = field.getName();
                    Object cellValue = getCellValue(row, i + startCellNum);
                    if (ObjectUtils.isEmpty(cellValue)) {
                        continue;
                    }
                    if (StringUtils.isNotBlank(cellValue.toString()) && !"rq".equals(fieldName)) {
                        if (field.getType() == Integer.class) {
                            cellValue = Integer.parseInt(cellValue.toString());
                        } else if (field.getType() == Double.class) {
                            try {
                                cellValue = Double.parseDouble(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常,{}", e.getMessage());
                                continue;
                            }
                        }
                        if (field.getType() == cellValue.getClass()) {
                            ReflectUtils.setFieldValue(mlImportBak, fieldName, cellValue);
                        }
                    }
                }
                mlImportBak.setRq(scheduleDate);
                list.add(mlImportBak);
            }
        }

        return list;
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

    public List<Field> getClassField(Class<? super MlImportBak> tClass) {
        List<Field> tempFields = new ArrayList<>();

        while (tClass != null) {
            tempFields.addAll(Arrays.asList(tClass.getDeclaredFields()));
            tClass = tClass.getSuperclass();
            if (tClass == null) {
                break;
            }
            if (StringUtils.equals(tClass.getSimpleName(), BaseEntity.class.getSimpleName())) {
                break;
            }
        }
        return tempFields;
    }
}
