package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.config.Global;
import com.ruoyi.common4ui.exception.BusinessException;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdResultDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanProductionFinalResultVo;
import com.zlt.aps.monthplan.api.service.IFactoryMonthPlanProdFinalRemoteService;
import com.zlt.aps.monthplan.api.service.IFactoryMonthPlanProductionFinalRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 分厂月生产计划最终版-按SKU前端业务接口服务类
 *
 * @author ZLT
 * @date 20250924
 */
@Controller
@RequestMapping("/factory/monthPlanProductionFinal")
@Api(tags = "分厂月生产计划-按SKU前端业务接口服务类")
public class FactoryMonthPlanProductionResultUIController extends BaseController {

    private final IFactoryMonthPlanProdFinalRemoteService factoryMonthPlanProdFinalService;

    private final IFactoryMonthPlanProductionFinalRemoteService factoryMonthPlanProductionFinalService;

    public FactoryMonthPlanProductionResultUIController(IFactoryMonthPlanProdFinalRemoteService factoryMonthPlanProdFinalService,
                                                        IFactoryMonthPlanProductionFinalRemoteService factoryMonthPlanProductionFinalService) {
        this.factoryMonthPlanProdFinalService = factoryMonthPlanProdFinalService;
        this.factoryMonthPlanProductionFinalService = factoryMonthPlanProductionFinalService;
    }

    /**
     * 根据条件查询分厂月生产计划排产结果-排产结果列表
     */
    @ResponseBody
    @RequiresPermissions("monthplan:monthPlanProductionFinal:list")
    @PostMapping("/list")
    @ApiOperation("根据条件查询分厂月生产计划排产结果列表")
    public TableDataInfo list(FactoryMonthPlanProdResultDto param) {
        if (checkParamEmpty(param)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        MonthPlanProductionFinalResult queryCondition = new MonthPlanProductionFinalResult();
        BeanUtils.copyProperties(param, queryCondition);
        return factoryMonthPlanProductionFinalService.list(queryCondition);
    }

    @ResponseBody
    @PostMapping("/statistics")
    @ApiOperation("统计分厂月生产计划排产结果-排产结果列表")
    public AjaxResult statistics(FactoryMonthPlanProdResultDto param) {
        if (checkParamEmpty(param)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        MonthPlanProductionFinalResult queryCondition = new MonthPlanProductionFinalResult();
        BeanUtils.copyProperties(param, queryCondition);
        return factoryMonthPlanProductionFinalService.statistics(queryCondition);
    }

    @ResponseBody
    @PostMapping("/statisticsDay")
    @ApiOperation("统计分厂月生产计划日排产规格数及日排产总量")
    public AjaxResult statisticsByDay(@RequestBody FactoryMonthPlanProdResultDto param) {
        if (checkParamEmpty(param)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        MonthPlanProductionFinalResult queryCondition = new MonthPlanProductionFinalResult();
        BeanUtils.copyProperties(param, queryCondition);
        return factoryMonthPlanProductionFinalService.getStatisticsDay(queryCondition);
    }

    @ResponseBody
    @PostMapping("/getProductionMonthType")
    @ApiOperation("获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产")
    public AjaxResult getProductionMonthType(@RequestBody FactoryMonthPlanProdResultDto param) {
        if (checkParamEmpty(param)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        FactoryMonthPlanProdFinal prodFinal = new FactoryMonthPlanProdFinal();
        BeanUtils.copyProperties(param, prodFinal);
        return factoryMonthPlanProdFinalService.getProductionMonthType(prodFinal);
    }

    /**
     * 下载试制量试计划导入模板
     */
    @ApiOperation("下载试制量试计划导入模板")
    @GetMapping({"/importTemplate"})
    @ResponseBody
    public void importTemplate(HttpServletResponse response, FactoryMonthPlanProdResultDto param) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.trialProductionPlan");
        MonthPlanProductionFinalResult queryCondition = new MonthPlanProductionFinalResult();
        BeanUtils.copyProperties(param, queryCondition);
        byte[] excelBytes = factoryMonthPlanProductionFinalService.importTemplate(queryCondition, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("monthplan:monthPlanProductionFinal:import")
    @ApiOperation("导入")
    @PostMapping({"/importData"})
    @ResponseBody
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        String functionName = I18nUtil.getMessage("ui.data.adjust.monthPlan.import.modelName");
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(Global.getUploadPath());
        context.setFunctionName(functionName);
        context.setProcedureCode("0901");
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = factoryMonthPlanProductionFinalService.importData(context, false);
        return ajaxResult;
    }

    @ApiOperation("导出下载错误日志")
    @GetMapping({"/exportImportErrorLog"})
    @ResponseBody
    public void exportImportErrorLog(HttpServletResponse response, ImportErrorLog entity) throws IOException {
        String fileName = "导入错误日志";
        byte[] excelBytes = factoryMonthPlanProductionFinalService.exportImportErrorLog(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 试制量试计划导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("monthplan:monthPlanProductionFinal:trialProductionPlan")
    @ApiOperation("试制量试计划导入")
    @PostMapping({"/importTrialProductionPlan"})
    @ResponseBody
    public AjaxResult importTrialProductionPlan(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        String functionName = I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.trialProductionPlan");
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(Global.getUploadPath());
        context.setFunctionName(functionName);
        context.setProcedureCode("0901");
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = factoryMonthPlanProductionFinalService.importTrialProductionPlan(context, false);
        return ajaxResult;
    }

    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.modelName");
    }

    @ApiOperation("数据导出")
    @RequiresPermissions("monthplan:monthPlanProductionFinal:export")
    @GetMapping({"/export"})
    @ResponseBody
    public void export(HttpServletResponse response, FactoryMonthPlanProdResultDto param) throws IOException {
        String fileName = this.getExportTemplateFileName();
        if (checkParamEmpty(param)) {
            // 分厂、年份、月份不可为空
            ExcelUtil<MonthPlanProductionFinalResultVo> util = new ExcelUtil<>(MonthPlanProductionFinalResultVo.class);
            util.exportExcel(response, null, fileName, fileName);
            return;
        }
        MonthPlanProductionFinalResult queryCondition = new MonthPlanProductionFinalResult();
        BeanUtils.copyProperties(param, queryCondition);
        byte[] excelBytes = factoryMonthPlanProductionFinalService.exportData(queryCondition, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }


    @ResponseBody
    @PostMapping("/linkProductInfoByProductCode")
    @ApiOperation("输入SAP代码后自动关联出字段")
    public AjaxResult linkProductInfoByProductCode(@RequestBody MonthPlanProductionFinalResult param) {
        return factoryMonthPlanProductionFinalService.linkProductInfoByProductCode(param);
    }

    @ResponseBody
    @PostMapping("/calculateByOrderQty")
    @ApiOperation("输入订单数量后系统自动计算")
    public AjaxResult calculateByOrderQty(@RequestBody MonthPlanProductionFinalResult param) {
        return factoryMonthPlanProductionFinalService.calculateByOrderQty(param);
    }

    @ResponseBody
    @RequiresPermissions("monthplan:monthPlanProductionFinal:addSpecifications")
    @PostMapping("/addSpecifications")
    @ApiOperation("月计划手动调整-新增规格的增量")
    public AjaxResult addSpecifications(@RequestBody MonthPlanProductionFinalResult param) {
        return factoryMonthPlanProductionFinalService.addSpecifications(param);
    }

    @ResponseBody
    @RequiresPermissions("monthplan:monthPlanProductionFinal:editPlan")
    @PostMapping("/editPlan")
    @ApiOperation("月计划手动调整-编辑计划")
    public AjaxResult editPlan(@RequestBody MonthPlanProductionFinalResult param) {
        return factoryMonthPlanProductionFinalService.editPlan(param);
    }

    @ResponseBody
    @RequiresPermissions("monthplan:monthPlanProductionFinal:subtractSpecification")
    @PostMapping("/subtractSpecification")
    @ApiOperation("规格直接减量为零")
    public AjaxResult subtractSpecification(@RequestBody MonthPlanProductionFinalResult param) {
        return factoryMonthPlanProductionFinalService.subtractSpecification(param);
    }

    /**
     * 校验分厂、年、月份不可为空
     *
     * @param param
     * @return
     */
    private boolean checkParamEmpty(FactoryMonthPlanProdResultDto param) {
        if (null == param) {
            return true;
        }
        if (null == param.getMonth()) {
            return true;
        }
        if (null == param.getYear()) {
            return true;
        }
        if (StringUtils.isBlank(param.getFactoryCode())) {
            return true;
        }
        return false;
    }
}
