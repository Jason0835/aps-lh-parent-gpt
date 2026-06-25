package com.zlt.aps.controller.dj;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.dj.api.domain.entity.DjMachineMaintenance;
import com.zlt.aps.dj.api.service.IDjMachineMaintenanceRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 垫胶机台维修计划Controller
 *
 * @author zlt
 */
@Controller
@RequestMapping("/dj/machineMaintenance")
@Api(tags = { "垫胶机台维修计划维护接口" })
public class DjMachineMaintenanceUIController extends BaseUIController<DjMachineMaintenance> {

    @Autowired
    private IDjMachineMaintenanceRemoteService iDjMachineMaintenanceService;

    private final String prefix = "aps/dj/machineMaintenance";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("dj:machineMaintenance:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/index";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("djMachineMaintenance", new DjMachineMaintenance());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("djMachineMaintenance", iDjMachineMaintenanceService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("dj:machineMaintenance:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(DjMachineMaintenance djMachineMaintenance) {
        return iDjMachineMaintenanceService.list(djMachineMaintenance);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("dj:machineMaintenance:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(DjMachineMaintenance djMachineMaintenance) {
        return iDjMachineMaintenanceService.save(djMachineMaintenance);
    }

    /**
     * 删除
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("dj:machineMaintenance:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iDjMachineMaintenanceService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(DjMachineMaintenance djMachineMaintenance) {
        return iDjMachineMaintenanceService.checkUnique(djMachineMaintenance);
    }

    @Override
    public String getExportTemplateFileName(){
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "DJ1001";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.dj.machineMaintenance.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<DjMachineMaintenance> util = new ExcelUtil<>(DjMachineMaintenance.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, DjMachineMaintenance entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iDjMachineMaintenanceService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
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
        AjaxResult ajaxResult = iDjMachineMaintenanceService.importData(context, updateSupport);
        return ajaxResult;
    }
}