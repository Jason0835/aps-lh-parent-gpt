package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxTransferDeskDTO;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxOnlineImport;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxGanttVo;
import com.zlt.aps.cxlh.cx.api.service.ICxScheduleResultService;
import com.zlt.aps.cxlh.cx.api.service.ICxSchedulingAlgorithmService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * 成型排程结果Controller
 *
 * @author zlt
 * @date 2021-07-12
 */
@Api(tags = "成型排程结果")
@Controller
@RequestMapping("/cx/cxScheduleResult")
public class CxScheduleResultUIController extends BaseUIController<CxScheduleResult> {
    @Autowired
    public ICxScheduleResultService iCxScheduleResultService;
    @Autowired
    public ICxSchedulingAlgorithmService iCxSchedulingAlgorithmService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("cx:cxScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxScheduleResult cxScheduleResult) {
        return iCxScheduleResultService.list(cxScheduleResult);
    }

    /**
     * 根据条件查询主表数据-大屏使用(无权限)
     */
    @ApiOperation("根据条件查询主表数据-大屏使用(无权限)")
    @PostMapping("/list4BigScreen")
    @ResponseBody
    public TableDataInfo list4BigScreen(CxScheduleResult cxScheduleResult) {
        return iCxScheduleResultService.list(cxScheduleResult);
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
        return I18nUtil.getMessage("ui.data.column.result.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {

        String fileName = this.getExportTemplateFileName();
        ExcelUtil<CxScheduleResult> util = new ExcelUtil<>(CxScheduleResult.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, CxScheduleResult entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iCxScheduleResultService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }


    @ApiOperation("数据现场计划")
    @GetMapping({"/export2"})
    @ResponseBody
    public void export2(HttpServletResponse response, CxScheduleResult entity) throws IOException {
        String fileName ="现场计划"+ DateUtils.parseDateToStr("yyyyMMddHHmmss",new Date())+".xlsx";
        byte[] excelBytes = iCxScheduleResultService.exportData2(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData2"})
    @ResponseBody
    @ApiOperation("数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport ) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iCxScheduleResultService.importData(context, false);
        return ajaxResult;
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入2")
    public AjaxResult importData2(@RequestPart("file") MultipartFile file, boolean updateSupport, String scheduleDate) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iCxScheduleResultService.importData2(context,1L,scheduleDate);
        return ajaxResult;
    }

    @PostMapping({"/importData3"})
    @ResponseBody
    @ApiOperation("数据导入3")
    public AjaxResult importData3(@RequestPart("file") MultipartFile file, boolean updateSupport, String scheduleDate) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iCxScheduleResultService.importData3(context,1L,scheduleDate);
        return ajaxResult;
    }

    /**
     * 修改或新增成型排程结果
     */
    @ApiOperation("修改或新增成型排程结果")
    @RequiresPermissions("cx:cxScheduleResult:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxScheduleResult cxScheduleResult) {
        return iCxScheduleResultService.save(cxScheduleResult);
    }

    /**
     * 成型自动排程校验
     */
    @PostMapping("/validateAutoPlan")
    @ResponseBody
    public AjaxResult validateAutoPlan(com.zlt.aps.cx.api.domain.entity.CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        String msg = "2";
        return AjaxResult.success(msg);
    }

    /**
     * 成型自动排程
     */
    @ApiOperation("成型自动排程")
    @RequiresPermissions("cx:cxScheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    public AjaxResult autoPlan( @RequestBody CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iCxSchedulingAlgorithmService.calculateCarbonationPlan(entity);
    }

    /**
     * 成型插单算法校验
     */
    @PostMapping("/validateAdd")
    @ResponseBody
    public AjaxResult validateAdd(CxScheduleResult entity) {
        return iCxScheduleResultService.validateAdd(entity);
    }

    @ApiOperation("转机台")
    @RequiresPermissions("cx:cxScheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(@RequestBody CxTransferDeskDTO dto){
        return iCxScheduleResultService.changeMachine(dto);
    }

    /**
     * 转机台校验
     */
    @PostMapping("/validateChangeMachine")
    @ResponseBody
    public AjaxResult validateChangeMachine(CxScheduleResult entity) {
        return iCxScheduleResultService.validateChangeMachine(entity);
    }

    @ApiOperation(value = "模板下载2", notes = "导入模板下载2")
    @GetMapping("/downloadTemplate2")
    @ResponseBody
    public void downloadTemplate2(HttpServletResponse response) throws IOException {
        String fileName = "现场计划模板";
        ExcelUtil<CxOnlineImport> util = new ExcelUtil<>(CxOnlineImport.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    /**
     * 调量校验
     */
    @PostMapping("/validateChangeQty")
    @ResponseBody
    public AjaxResult validateChangeQty(CxScheduleResult entity) {
        return iCxScheduleResultService.validateChangeQty(entity);
    }

    /**
     * 获取Bom版本信息
     */
    @PostMapping("/getBomData")
    @ResponseBody
    public AjaxResult getBomData(CxScheduleResult entity) {
        return iCxScheduleResultService.getBomData(entity);
    }



    /**
     * 排程发布校验
     */
    @ApiOperation("排程发布校验")
    @PostMapping("/publishValidate")
    @ResponseBody
    public AjaxResult publishValidate(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iCxScheduleResultService.publishValidate(entity);
    }

    /**
     * 排程发布
     */
    @ApiOperation("排程发布")
    @RequiresPermissions("cx:cxScheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iCxScheduleResultService.publish(entity);
    }

    /**
     * 删除成型排程结果
     */
    @ApiOperation("删除成型排程结果（id不为空）")
    @RequiresPermissions("cx:cxScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxScheduleResultService.remove(arr);
    }

    /**
     * 更改发布状态
     */
    @ApiOperation("更改发布状态")
    @RequiresRoles("admin")
    @PostMapping("/changeReleaseStatus")
    @ResponseBody
    public AjaxResult changeReleaseStatus(CxScheduleResult entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iCxScheduleResultService.changeReleaseStatus(entity);
    }


    /**
     * 将成型排程解析成月度剩余量，胎胚库存，月度完成量
     */
    @ApiOperation("解析现场计划")
    @PostMapping("/parseCxScheduleResult")
    @ResponseBody
    public AjaxResult parseCxScheduleResult(CxScheduleResult cxScheduleResult) {
        return iCxScheduleResultService.parseCxScheduleResult(cxScheduleResult);
    }

    /**
     * 查询成型机台甘特图
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    @ApiOperation("查询成型机台甘特图")
    @PostMapping("/selectMachineGantt")
    @ResponseBody
    public AjaxResult selectMachineGantt(CxGanttVo queryVO) throws ParseException {
//        queryVO.setScheduleDate(DateUtils.addDays(DateUtils.getNowDate("yyyy-MM-dd"), 1));
        return iCxScheduleResultService.selectMachineGantt(queryVO);
    }

    /**
     * 成型调整硫化
     */
    @ApiOperation("成型调整硫化")
    @PostMapping("/updateLhScheduleResult")
    @ResponseBody
    public AjaxResult updateCxScheduleResult(@RequestBody CxScheduleResult cxScheduleResult) {
        return iCxScheduleResultService.updateCxScheduleResult(cxScheduleResult);
    }
}
