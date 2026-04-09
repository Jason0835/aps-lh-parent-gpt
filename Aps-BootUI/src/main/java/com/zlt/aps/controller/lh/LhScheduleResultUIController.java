package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertDTO;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertParamDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleRequestDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResponseDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultUpdateDTO;
import com.zlt.aps.lh.api.domain.dto.LhTransferDeskDTO;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.service.ILhScheduleResultRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Api(tags = "硫化排程结果")
@Controller
@RequestMapping("/lh/lhScheduleResult")
public class LhScheduleResultUIController extends BaseUIController<LhScheduleResult> {

    @Autowired
    private ILhScheduleResultRemoteService iLhScheduleResultRemoteService;


    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("lh:lhScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhScheduleResult lhScheduleResult) {
        return iLhScheduleResultRemoteService.list(lhScheduleResult);
    }


    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<LhScheduleResult> util = new ExcelUtil<>(LhScheduleResult.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @PostMapping({"/importData"})
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
        AjaxResult ajaxResult = iLhScheduleResultRemoteService.importData(context, false);
        return ajaxResult;
    }


    @ApiOperation("插单查询可用机台列表")
    @RequiresPermissions("lh:lhScheduleResult:getScheduleMachineInfo")
    @PostMapping("/getScheduleMachineInfo")
    @ResponseBody
    public AjaxResult getScheduleMachineInfo(@RequestBody LhOrderInsertParamDTO insertParamDTO) {
        List<LhMachineInfo> resultList = iLhScheduleResultRemoteService.getScheduleMachineInfo(insertParamDTO);
        return AjaxResult.success(resultList);
    }


    @ApiOperation("插单")
    @RequiresPermissions("lh:lhScheduleResult:insertOrder")
    @PostMapping("/insertOrder")
    @ResponseBody
    public AjaxResult insertOrder(@RequestBody LhOrderInsertDTO insertDTO) {
        return iLhScheduleResultRemoteService.insertOrder(insertDTO);
    }

    /**
     * 自动排程
     *
     * @return
     */
    @ApiOperation("硫化自动排程")
    @RequiresPermissions("lh:lhScheduleResult:autoLhScheduleResult")
    @PostMapping("/execute")
    @ResponseBody
    public LhScheduleResponseDTO execute(@RequestBody LhScheduleRequestDTO lhScheduleRequestDTO) {
        LhScheduleResponseDTO lhScheduleResponseDTO = iLhScheduleResultRemoteService.execute(lhScheduleRequestDTO);
        return lhScheduleResponseDTO;
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, LhScheduleResult entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iLhScheduleResultRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 保存
     */
    @ApiOperation("保存")
    @RequiresPermissions({"lh:lhScheduleResult:save"})
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult editSave(LhScheduleResult lhScheduleResult) {
        AjaxResult ajaxResult = iLhScheduleResultRemoteService.save(lhScheduleResult);
        return ajaxResult;
    }

    /**
     * 删除
     */
    @ApiOperation("删除")
    @RequiresPermissions("lh:lhScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhScheduleResultRemoteService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("硫化排程结果转机台校验")
    //@RequiresPermissions("lh:lhScheduleResult:validateChangeMachine")
    @PostMapping("/validateChangeMachine")
    @ResponseBody
    public AjaxResult validateChangeMachine(@RequestBody LhTransferDeskDTO dto) {
        return iLhScheduleResultRemoteService.validateChangeMachine(dto);
    }

    @ApiOperation("转机台")
    @RequiresPermissions("lh:lhScheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(@RequestBody LhTransferDeskDTO dto) {
        return iLhScheduleResultRemoteService.changeMachine(dto);
    }

    @ApiOperation("调量")
    @RequiresPermissions("lh:lhScheduleResult:adjustQuantity")
    @PostMapping("/adjustQuantity")
    @ResponseBody
    public AjaxResult adjustQuantity(@RequestBody LhScheduleResultUpdateDTO dto) {
        return iLhScheduleResultRemoteService.adjustQuantity(dto);
    }

    @ApiOperation("文字示方调整")
    @PostMapping("/adjustTextNo")
    @ResponseBody
    public AjaxResult adjustTextNo(@RequestBody LhTransferDeskDTO dto) {
        return iLhScheduleResultRemoteService.adjustTextNo(dto);
    }


    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 生产控制台导入
     */
    @Override
    public String getFunctionName() {
        return getExportTemplateFileName();
    }

    /**
     * 文件模板文件名
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.lh.scheduleResult.modelName");
    }


    /**
     * 发布排程
     */
    @ApiOperation("发布排程")
    @RequiresPermissions("lh:scheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(LhScheduleResult dto) {
        // 默认发布当天排程结果
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iLhScheduleResultRemoteService.publish(dto);
    }

}
