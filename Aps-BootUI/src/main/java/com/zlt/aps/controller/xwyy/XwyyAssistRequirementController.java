package com.zlt.aps.controller.xwyy;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyAssistRequirement;
import com.zlt.aps.xwyy.api.service.IXwyyAssistRequirementService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 纤维压延外厂需求Controller
 * @author chen
 * @date 2022-03-14
 */
@Api(tags = "纤维压延外厂需求")
@Controller
@RequestMapping("/xwyy/assistRequirement")
public class XwyyAssistRequirementController extends BaseController {

    @Autowired
    private IXwyyAssistRequirementService iXwyyAssistRequirementService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "xwyy/assistRequirement";

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("xwyy:assistRequirement:view")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/assistRequirement";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("xwyyAssistRequirement", new XwyyAssistRequirement());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("xwyyAssistRequirement", iXwyyAssistRequirementService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 跳转到公共排程结果导入页面
     * @param mmap 用于存放当前模块前缀路径
     */
    @GetMapping("/toImport")
    public String toImport(ModelMap mmap){
        mmap.put("initDate", DateUtils.parseDateToStr( "yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        mmap.put("prefix", prefix);
        return "common/importData";
    }

    /**
     * 根据条件查询纤维压延外厂需求列表
     */
    @ApiOperation("根据条件查询纤维压延外厂需求列表")
    @RequiresPermissions("xwyy:assistRequirement:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyAssistRequirement entity) {
        return iXwyyAssistRequirementService.list(entity);
    }

    /**
     * 修改或新增纤维压延外厂需求
     */
    @ApiOperation("修改或新增纤维压延外厂需求")
    @RequiresPermissions("xwyy:assistRequirement:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(XwyyAssistRequirement xwyyAssistRequirement) {
        AjaxResult ajaxResult = null;
        if (xwyyAssistRequirement.getId() != null){
            ajaxResult = iXwyyAssistRequirementService.edit(xwyyAssistRequirement);
        } else{
            ajaxResult = iXwyyAssistRequirementService.add(xwyyAssistRequirement);
        }
        return ajaxResult;
    }

    /**
     * 删除纤维压延外厂需求
     */
    @ApiOperation("删除纤维压延外厂需求（id不为空）")
    @RequiresPermissions("xwyy:assistRequirement:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iXwyyAssistRequirementService.remove(arr);
    }

    /**
     * 校验纤维压延外厂需求唯一性
     */
    @ApiOperation("校验纤维压延外厂需求唯一性")
    @PostMapping("/checkXwyyAssistRequirementUnique")
    @ResponseBody
    public String checkXwyyAssistRequirementUnique(XwyyAssistRequirement xwyyAssistRequirement) {
        return iXwyyAssistRequirementService.checkXwyyAssistRequirementUnique(xwyyAssistRequirement);
    }

    /**
     * 导出纤维压延外厂需求
     */
    @ApiOperation("导出纤维压延外厂需求")
    @RequiresPermissions("xwyy:assistRequirement:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,XwyyAssistRequirement xwyyAssistRequirement) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.assistRequirement.modelName");
        List<XwyyAssistRequirement> list = iXwyyAssistRequirementService.getList(xwyyAssistRequirement);
        ExcelUtil<XwyyAssistRequirement> util = new ExcelUtil<>(XwyyAssistRequirement. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, xwyyAssistRequirement.toString(),ApsConstant.PROCEDURE_CODE_XWYY);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载导入模板
     *
     * @param response 下载的模板文件
     * @throws IOException 异常
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String tempName = I18nUtil.getMessage("ui.data.column.assistRequirement.modelName");  //根据国际化获取导入模板名称
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "xwyy/" + tempName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        ExcelUtil.setResponseHeader(response, tempName);
        FileUtils.writeInputStream(in, response.getOutputStream());
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file 要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("xwyy:assistRequirement:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importScheduleData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date scheduleDate) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_XWYY,
                I18nUtil.getMessage("ui.data.column.assistRequirement.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        InputStream in = new ByteArrayInputStream(data);
        List<XwyyAssistRequirement> list = parseObject(in, scheduleDate);
        AjaxResult ajaxResult = iXwyyAssistRequirementService.importData(list, importLog.getId(), DateFormatUtils.format(scheduleDate,"yyyy-MM-dd"));
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 根据excel的io流解析成对应集合对象
     * @param in io
     * @return 解析后集合对象
     * @throws Exception 异常
     */
    private List<XwyyAssistRequirement> parseObject(InputStream in, Date scheduleDate) throws Exception {

        List<XwyyAssistRequirement> list = new ArrayList<>();
        if (in == null) {
            return list;
        }
        Workbook workbook = WorkbookFactory.create(in);
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        }
        int rows = sheet.getPhysicalNumberOfRows();

        if (rows > 0) {
            for (int i = 3; i < rows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String val0 = getCellValue(row.getCell(0));    //0   帘布大卷编号
                String val3 = getCellValue(row.getCell(3));    //3   中班(12点-24点)计划量
                String val13 = getCellValue(row.getCell(13));  //13  夜班(0点-12点)计划量
                String val22 = getCellValue(row.getCell(22));  //22  当天库存
                String val24 = getCellValue(row.getCell(24));  //24  白班外厂应支
                String val25 = getCellValue(row.getCell(25));  //25  5厂中班
                String val26 = getCellValue(row.getCell(26));  //26  5厂夜班
                String val27 = getCellValue(row.getCell(27));  //27  5厂白班

                XwyyAssistRequirement entity = new XwyyAssistRequirement();
                entity.setBigRollCode(StringUtils.trim(val0));				//0   帘布大卷编号
                entity.setDayPlanQty(this.convertToBigDecimal(val3));		//3   中班(12点-24点)计划量
                entity.setNightPlanQty(this.convertToBigDecimal(val13));	//13  夜班(0点-12点)计划量
                entity.setTodayStock(this.convertToBigDecimal(val22));		//22  当日库存
                entity.setDayOut(this.convertToBigDecimal(val24));			//24 白班外厂应支 
                entity.setFac5Class1Plan(this.convertToBigDecimal(val25));	//25 5厂中班
                entity.setFac5Class2Plan(this.convertToBigDecimal(val26));	//26 5厂夜班
                entity.setFac5Class3Plan(this.convertToBigDecimal(val27));	//27 5厂白班
                entity.setScheduleDate(scheduleDate);

                list.add(entity);
            }
        }
        return list;
    }
    
    /**
     * 将字符串转换成数字，为空转成0，非数字转成空
     * @param value
     * @return
     */
    private BigDecimal convertToBigDecimal(String value) {
    	String strValue = StringUtils.trim(value);
    	if (StringUtils.isEmpty(strValue)) {
    		return BigDecimal.ZERO;
    	}
        try
        {
            return new BigDecimal(strValue);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String getCellValue(Cell cell) {
        Object val = null;
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
            val = cell.getNumericCellValue();
            //当val值为大值时，很可能解析为科学计数法显示
            String valStr = val + "";
            if (valStr.contains("E")) {
                BigDecimal realValue = new BigDecimal(valStr);
                val = realValue.toPlainString();
            }
            if (valStr.endsWith(".0")) {
                val = valStr.substring(0, valStr.indexOf(".0"));
            }
        } else if (cell.getCellType() == CellType.STRING) {
            val = cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            val = cell.getBooleanCellValue();
        }
        if (val == null) {
            return null;
        }
        return val + "";
    }
}
