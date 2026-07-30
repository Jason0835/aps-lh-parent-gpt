package com.zlt.aps.controller.gsq;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineMaintenancePlan;
import com.zlt.aps.gsq.api.service.IGsqMachineMaintenancePlanService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 钢丝圈机台维修计划UIController
 * 继承BaseUIController，通过Feign接口调用后端微服务
 *
 * @author zlt
 * @date 2026-07-29
 */
@Slf4j
@Api(tags = "钢丝圈机台维修计划")
@Controller
@RequestMapping("/gsq/machineMaintenancePlan")
public class GsqMachineMaintenancePlanUIController extends BaseUIController<GsqMachineMaintenancePlan> {

    @Autowired
    private IGsqMachineMaintenancePlanService gsqMachineMaintenancePlanService;

    private final String prefix = "gsq/machineMaintenancePlan";

    /**
     * 列表跳转至machineMaintenancePlan页面
     */
    @RequiresPermissions("gsq:machineMaintenancePlan:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/machineMaintenancePlan";
    }

    /**
     * 新增跳转至add页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("machineMaintenancePlan", new GsqMachineMaintenancePlan());
        return prefix + "/edit";
    }

    /**
     * 修改跳转至edit页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("machineMaintenancePlan", gsqMachineMaintenancePlanService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 查询钢丝圈机台维修计划列表
     */
    @ApiOperation("查询钢丝圈机台维修计划列表")
    @RequiresPermissions("gsq:machineMaintenancePlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqMachineMaintenancePlan entity) {
        return gsqMachineMaintenancePlanService.list(entity);
    }

    /**
     * 保存钢丝圈机台维修计划（id为空则新增，id不为空则修改）
     */
    @ApiOperation("保存钢丝圈机台维修计划")
    @RequiresPermissions("gsq:machineMaintenancePlan:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(GsqMachineMaintenancePlan entity) {
        return gsqMachineMaintenancePlanService.save(entity);
    }

    /**
     * 删除钢丝圈机台维修计划（逻辑删除）
     */
    @ApiOperation("删除钢丝圈机台维修计划")
    @RequiresPermissions("gsq:machineMaintenancePlan:remove")
    @PostMapping("/delete/{ids}")
    @ResponseBody
    public AjaxResult remove(@PathVariable("ids") String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(java.util.stream.Collectors.toList());
        return gsqMachineMaintenancePlanService.removeByIds(idList);
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "GSQ";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.gsq.machineMaintenancePlan.modalName");
    }

    /**
     * 导出钢丝圈机台维修计划
     */
    @ApiOperation("导出钢丝圈机台维修计划")
    @GetMapping("/exportData/{fileName}")
    @RequiresPermissions("gsq:machineMaintenancePlan:export")
    @ResponseBody
    public void export(HttpServletResponse response, GsqMachineMaintenancePlan entity, @PathVariable("fileName") String fileName) throws IOException {
        byte[] excelBytes = gsqMachineMaintenancePlanService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        org.apache.commons.io.IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 下载导入模板
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<GsqMachineMaintenancePlan> util = new ExcelUtil<>(GsqMachineMaintenancePlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 导入钢丝圈机台维修计划数据
     */
    @ApiOperation("导入钢丝圈机台维修计划数据")
    @RequiresPermissions("gsq:machineMaintenancePlan:import")
    @PostMapping("/importData")
    @ResponseBody
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return gsqMachineMaintenancePlanService.importData(context, updateSupport);
    }
}
