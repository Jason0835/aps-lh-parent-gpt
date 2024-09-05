package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.utils.StringUtils;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.zlt.aps.common.constant.ApsBootConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.entity.Gante;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.api.service.ILhScheduleResultService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.ObjectUtils;
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
 * 硫化排程结果Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@Api(tags = "硫化排程结果")
@Controller
@RequestMapping("/lh/scheduleResult")
public class LhScheduleResultController extends BaseController {

    private final String prefix = "lh/scheduleResult";

    @Autowired
    private ILhScheduleResultService iLhScheduleResultService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("lh:scheduleResult:view")
    @ApiOperation("跳转到硫化排程结果首页")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/scheduleResult";
    }

    /**
     * 跳转至插单页面
     */
    @RequiresPermissions("lh:scheduleResult:insertOrder")
    @ApiOperation("跳转到硫化排程结果插单页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("minDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));  //当前日期
        mmap.put("scheduleResult", new LhScheduleResultDto());
        return prefix + "/insertOrder";
    }

    /**
     * 跳转至修改页面
     */
    @RequiresPermissions("lh:scheduleResult:edit")
    @ApiOperation("获取硫化排程结果信息详细信息,跳转到编辑页面")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("scheduleResult", iLhScheduleResultService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 跳转到转机台页面
     *
     * @return 结果
     */
    @ApiOperation("转机台")
    @RequiresPermissions("lh:scheduleResult:changeMachine")
    @GetMapping("/changeMachine/{id}")
    public String changeMachine(@PathVariable("id") Long id, ModelMap mmap) {
        // 编辑类型为转机台
        mmap.put("editType", "1");
        mmap.put("scheduleResult", iLhScheduleResultService.getInfo(id));
        return prefix + "/changeQtyOrMachine";
    }


    /**
     * 跳转至甘特图
     */
    @GetMapping("/gantt/{flag}")
    public String gantt(ModelMap mmap,@PathVariable("flag") int flag) {
        mmap.put("scheduleDate",DateUtils.getDate());
        if (flag == 1 ){
            mmap.put("scheduleDate",DateUtils.getDate());
            return prefix + "/machineGant";
        }else {
            mmap.put("scheduleDate", DateUtils.getDate());
            return prefix + "/specGant";
        }
    }

    /**
     * 获取甘特图数据
     */
    @PostMapping("/getGantData")
    @ResponseBody
    public AjaxResult getGantData(Gante gante) {
        int flag = gante.getFlag();
        Date scheduleDate = gante.getScheduleDate()==null?new Date():gante.getScheduleDate();
        gante.setScheduleDate(scheduleDate);
        if (flag == 2 ){ //规格
            gante.setStartDay(DateUtils.getFirstDayByDate(scheduleDate));
            gante.setEndDay(DateUtils.getLastDayByDate(scheduleDate));
        }
        List<Gante> cxGanteDataList = iLhScheduleResultService.getLhGanteData(gante);
        return AjaxResult.success(cxGanteDataList);
    }



    /**
     * 跳转到调计划量页面
     *
     * @return 结果
     */
    @ApiOperation("调计划量")
    @RequiresPermissions("lh:scheduleResult:changeQty")
    @GetMapping("/changeQty/{id}")
    public String changeQty(@PathVariable("id") Long id, ModelMap mmap) {
        // 编辑类型为调计划量
        mmap.put("editType", "2");
        mmap.put("scheduleResult", iLhScheduleResultService.getInfo(id));
        return prefix + "/changeQtyOrMachine";
    }

    /**
     * 硫化自动排程
     */
    @GetMapping("/toLhAutoPlan")
    public String lhAutoPlan(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/autoPlan";
    }

    /**
     * 根据条件查询硫化排程结果列表
     */
    @ApiOperation("根据条件查询硫化排程结果列表")
    @RequiresPermissions("lh:scheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhScheduleResultDto dto) {
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iLhScheduleResultService.list(dto);
    }

    /**
     * 修改或新增硫化排程结果
     */
    @ApiOperation("修改或新增硫化排程结果")
    @RequiresPermissions({"lh:scheduleResult:edit", "lh:scheduleResult:insertOrder"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(LhScheduleResultDto lhScheduleResult) {
        /*String unique = checkLhScheduleResultUnique(lhScheduleResult);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }*/
        AjaxResult ajaxResult = null;
        if (lhScheduleResult.getId() != null) {
            ajaxResult = iLhScheduleResultService.edit(lhScheduleResult);
        } else {
            double class1PlanQty = lhScheduleResult.getClass1PlanQty() == null ? 0d : lhScheduleResult.getClass1PlanQty();
            double class2PlanQty = lhScheduleResult.getClass2PlanQty() == null ? 0d : lhScheduleResult.getClass2PlanQty();
            double class3PlanQty = lhScheduleResult.getClass3PlanQty() == null ? 0d : lhScheduleResult.getClass3PlanQty();
            // 若插单量为0报错
            if ((class1PlanQty + class2PlanQty + class3PlanQty) == 0d) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.qty.zero"));
            }
            lhScheduleResult.setDataSource("1");
            ajaxResult = iLhScheduleResultService.add(lhScheduleResult);
        }
        return ajaxResult;
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @RequiresPermissions("lh:scheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(LhScheduleResultDto lhScheduleResult) {
        String unique = checkLhScheduleResultUnique(lhScheduleResult);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        return iLhScheduleResultService.changeMachine(lhScheduleResult);
    }

    /**
     * 调量
     */
    @ApiOperation("调量")
    @RequiresPermissions("lh:scheduleResult:changeQty")
    @PostMapping("/changeQty")
    @ResponseBody
    public AjaxResult changeQty(LhScheduleResultDto lhScheduleResult) {
        return iLhScheduleResultService.changeQty(lhScheduleResult);
    }

    /**
     * 删除硫化排程结果
     */
    @ApiOperation("删除硫化排程结果（id不为空）")
    @RequiresPermissions("lh:scheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        String newIds = "";
        String scheduleDate = "";
        if (StringUtils.isNotBlank(ids)) {
            newIds = ids.substring(0, ids.indexOf("|"));
            scheduleDate = ids.substring(ids.indexOf("|") + 1);
        }
        Long[] arr = Convert.toLongArray(newIds);
        return iLhScheduleResultService.remove(arr);
    }

    /**
     * 校验记录唯一性
     */
    @ApiOperation("校验硫化排程结果唯一性")
    @PostMapping("/checkLhScheduleResultUnique")
    @ResponseBody
    public String checkLhScheduleResultUnique(LhScheduleResultDto lhScheduleResult) {
        LhScheduleResultDto checkUniqueDto = new LhScheduleResultDto();
        checkUniqueDto.setScheduleDate(lhScheduleResult.getScheduleDate());
        checkUniqueDto.setSapCode(lhScheduleResult.getSapCode());
        checkUniqueDto.setLhMachineCode(lhScheduleResult.getLhMachineCode());
        checkUniqueDto.setStockArea(lhScheduleResult.getStockArea());
        checkUniqueDto.setId(ObjectUtils.isEmpty(lhScheduleResult.getId()) ? null : lhScheduleResult.getId());
        return iLhScheduleResultService.checkLhScheduleResultUnique(checkUniqueDto);
    }

    /**
     * 导出硫化排程结果
     */
    @ApiOperation("导出硫化排程结果")
    @RequiresPermissions("lh:scheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, LhScheduleResultDto lhScheduleResult) throws IOException {
        //若是没传日期则默认查询当日排程
        if (lhScheduleResult.getScheduleDate() == null) {
            lhScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        //获取字节流数据
        byte[] data = iLhScheduleResultService.export(lhScheduleResult);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.lh.scheduleResult.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, lhScheduleResult.toString(), ApsConstant.PROCEDURE_CODE_LH);
        iExportLogService.add(exportLog);
    }

    /**
     * 发布排程
     */
    @ApiOperation("发布排程")
    @RequiresPermissions("lh:scheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(LhScheduleResultDto dto) {
        // 默认发布当天排程结果
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iLhScheduleResultService.publish(dto);
    }

    /**
     * 插单校验
     */
    @PostMapping("/validateAdd")
    @ResponseBody
    public AjaxResult validateAdd(LhScheduleResultDto entity) {
        return iLhScheduleResultService.validateAdd(entity);
    }

    /**
     * 转机台校验
     */
    @PostMapping("/validateChangeMachine")
    @ResponseBody
    public AjaxResult validateChangeMachine(LhScheduleResultDto dto) {
        String unique = checkLhScheduleResultUnique(dto);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        return iLhScheduleResultService.validateChangeMachine(dto);
    }

    /**
     * 下载模板
     *
     * @param response
     * @throws IOException
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {

        String lang = AuthorizationUtils.getLang();
        String tempName = (ApsBootConstant.EN_US.equals(lang) ? ApsBootConstant.LH_EN_TEMP : ApsBootConstant.LH_ZH_TEMP);
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "lh/" + tempName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        String fileName = I18nUtil.getMessage("ui.data.column.lh.scheduleResult.modelName");
        ExcelUtil.setResponseHeader(response, fileName);
        FileUtils.writeInputStream(in, response.getOutputStream());
        return AjaxResult.success();
    }

    /**
     * 跳转到公共排程结果导入页面
     *
     * @param mmap 用于存放当前模块前缀路径
     */
    @GetMapping("/toImport")
    public String toImport(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("prefix", prefix);
        return "common/importData";
    }

    /**
     * 跳转至导入页面
     *
     * @param mop
     * @return
     */
    @GetMapping("/importData2")
    public String importDate2(ModelMap mop) {
        mop.put("prefix", prefix);
        mop.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return "common/importData2";
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("lh:scheduleResult:import")
    @PostMapping("/importScheduleData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date scheduleDate) throws Exception {
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功的记录则不予导入
        LhScheduleResultDto entity = new LhScheduleResultDto();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iLhScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iLhScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_LH,
                I18nUtil.getMessage("ui.data.column.lh.scheduleResult.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<LhScheduleResultDto> util = new ExcelUtil<>(LhScheduleResultDto.class);
        List<LhScheduleResultDto> list = util.importExcel(in, 2);

        AjaxResult ajaxResult = iLhScheduleResultService.importData(list, importLog.getId(), DateFormatUtils.format(scheduleDate, "yyyy-MM-dd"));
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("lh:scheduleResult:import")
    @PostMapping("/importScheduleData2")
    @ResponseBody
    public AjaxResult importData2(MultipartFile file, Date scheduleDate) throws Exception {
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功的记录则不予导入
        LhScheduleResultDto entity = new LhScheduleResultDto();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iLhScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iLhScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_LH,
                I18nUtil.getMessage("ui.data.column.lh.scheduleResult.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        List<LhScheduleResultDto> list = parseObject(in);

        AjaxResult ajaxResult = iLhScheduleResultService.importData(list, importLog.getId(), DateFormatUtils.format(scheduleDate, "yyyy-MM-dd"));
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 跳转到更改发布状态页面
     */
    @GetMapping("/toChangeReleaseStatus")
    public String changeReleaseStatus(ModelMap map, Date scheduleDate) {
        map.put("prefix", prefix);
        map.put("scheduleDate", scheduleDate);
        return "common/changeReleaseStatus";
    }

    /**
     * 更改发布状态
     */
    @ApiOperation("更改发布状态")
    @RequiresRoles("admin")
    @PostMapping("/changeReleaseStatus")
    @ResponseBody
    public AjaxResult changeReleaseStatus(LhScheduleResultDto entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iLhScheduleResultService.changeReleaseStatus(entity);
    }

    /**
     * 硫化自动排程校验(此处用成型实体来接受硫化排程结果)
     */
    @PostMapping("/lhValidateAutoPlan")
    @ResponseBody
    public AjaxResult lhValidateAutoPlan(LhScheduleResultDto dto) {
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = iLhScheduleResultService.isReleasingOrTimeoutByDate(dto);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        List<LhScheduleResultDto> list = iLhScheduleResultService.getList(dto);
        String msg = "";
        if (CollectionUtils.isEmpty(list)) {
            //未生成，直接生成
            msg = "2";
        } else {
            //未投产
            msg = "1";
            Boolean isPublish = iLhScheduleResultService.isPublish(dto);
            if (isPublish) {
                //已投产
                msg = "3";
            }
        }
        return AjaxResult.success(msg);
    }

    /**
     * 硫化自动排程
     */
    @ApiOperation("硫化自动排程")
    @RequiresPermissions("lh:scheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    public AjaxResult lhAutoPlan(LhScheduleResultDto dto) {
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iLhScheduleResultService.autoPlan(dto);
    }

    public List<LhScheduleResultDto> parseObject(InputStream in) throws Exception {

        List<LhScheduleResultDto> list = new ArrayList<>();
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
            for (int i = 2; i < rows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String val2 = getCellValue(row.getCell(2));    //2    硫化机台名称
                String val3 = getCellValue(row.getCell(3));    //3    左右模
                String val4 = getCellValue(row.getCell(4));    //4    SAP品号
                String val5 = getCellValue(row.getCell(5));    //5    型号
                String val6 = getCellValue(row.getCell(6));    //6    日计划数量
                String val8 = getCellValue(row.getCell(8));    //8    一班计划量
                String val10 = getCellValue(row.getCell(10));  //10    一班原因分析
                String val11 = getCellValue(row.getCell(11));  //11   二班计划量
                String val13 = getCellValue(row.getCell(13));  //13   二班原因分析
                String val14 = getCellValue(row.getCell(14));  //14   三班计划量
                String val16 = getCellValue(row.getCell(16));  //16   三班原因分析

                LhScheduleResultDto entity = new LhScheduleResultDto();
                entity.setLhMachineName(getStringValue(val2));        //2    硫化机台名称
                entity.setLeftRightMold(getStringValue(val3));        //3    左右模
                entity.setSapCode(getStringValue(val4));              //4    SAP品号
                entity.setSpecDesc(getStringValue(val5));             //5    型号
                entity.setDailyPlanQty(getIntegerValue(val6));        //6    日计划数量
                entity.setClass1PlanQty(getIntegerValue(val8));       //8    一班计划量
                entity.setClass1AnalysisInput(getStringValue(val10));  //10    一班原因分析
                entity.setClass2PlanQty(getIntegerValue(val11));      //11   二班计划量
                entity.setClass2AnalysisInput(getStringValue(val13)); //13   二班原因分析
                entity.setClass3PlanQty(getIntegerValue(val14));      //14   三班计划量
                entity.setClass3AnalysisInput(getStringValue(val16)); //16   三班原因分析

                list.add(entity);
            }
        }
        return list;
    }

    public Integer getIntegerValue(String val) {
        Integer integerVal = null;
        if (val == null) {
            return integerVal;
        }
        try {
            if (val.endsWith(".0")) {
                val = val.substring(0, val.indexOf(".0"));
            }
            integerVal = Integer.parseInt(val);
        } catch (Exception e) {
            return null;
        }
        return integerVal;
    }

    public String getStringValue(String val) {
        String stringVal = null;
        if (val == null) {
            return stringVal;
        }
        try {
            stringVal = val + "";
        } catch (Exception e) {
            return null;
        }
        return stringVal;
    }

    public String getCellValue(Cell cell) {
        Object val = null;
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
            val = cell.getNumericCellValue();
            //当val值为大值时，很可能解析为科学计数法显示
            String valStr = val + "";
            if (valStr.indexOf("E") >= 0) {
                BigDecimal realValue = new BigDecimal(valStr);
                val = realValue.toPlainString();
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
