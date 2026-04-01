package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.service.ILhMachineInfoRemoteService;
import com.zlt.aps.lh.api.service.IMdmMouldCleanPlanRemoteService;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldCleanPlan;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Api(tags = "模具清洗计划")
@Controller
@RequestMapping("/lh/mouldCleanPlan")
public class MdmMouldCleanPlanUIController extends BaseUIController<MdmMouldCleanPlan> {

    @Autowired
    private IMdmMouldCleanPlanRemoteService iMdmMouldCleanPlanService;

    @Autowired
    private ILhMachineInfoRemoteService iLhMachineInfoService;

    @ApiOperation("根据条件查询数据")
    @RequiresPermissions("lh:mouldCleanPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list( MdmMouldCleanPlan query) {
        return iMdmMouldCleanPlanService.list(query);
    }

    @ApiOperation("获取详细信息")
    @RequiresPermissions("lh:mouldCleanPlan:list")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iMdmMouldCleanPlanService.getInfo(id));
    }

    @ApiOperation("修改或新增")
    @RequiresPermissions("lh:mouldCleanPlan:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MdmMouldCleanPlan mdmMouldCleanPlan) {
        return iMdmMouldCleanPlanService.save(mdmMouldCleanPlan);
    }

    @ApiOperation("删除")
    @RequiresPermissions("lh:mouldCleanPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestBody List<Long> ids) {
        return iMdmMouldCleanPlanService.removeByIds(ids);
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmMouldCleanPlan entity) throws IOException {
        String fileName = "模具清洗计划";
        byte[] excelBytes = iMdmMouldCleanPlanService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入")
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(null);
        context.setFunctionName("模具清洗计划");
        context.setProcedureCode("0");
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iMdmMouldCleanPlanService.importData(context,updateSupport);
        return ajaxResult;
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = "模具清洗计划";
        ExcelUtil<MdmMouldCleanPlan> util = new ExcelUtil<>(MdmMouldCleanPlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("获取机台下拉列表")
    @PostMapping("/getMachineList")
    @ResponseBody
    public AjaxResult getMachineList(LhMachineInfo query) {
        TableDataInfo tableDataInfo = iLhMachineInfoService.list(query);
        return AjaxResult.success(tableDataInfo.getRows());
    }
}
