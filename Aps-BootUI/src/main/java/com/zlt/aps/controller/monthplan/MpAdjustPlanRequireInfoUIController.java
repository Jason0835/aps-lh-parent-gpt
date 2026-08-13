package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.MpAdjustPlanRequireInfo;
import com.zlt.aps.mp.api.service.IMpAdjustPlanRequireInfoRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 计划调整需求信息 UI 控制层。
 */
@Api(tags = "计划调整需求信息")
@Controller
@RequestMapping("/monthplan/adjustPlanRequireInfo")
public class MpAdjustPlanRequireInfoUIController extends BaseUIController<MpAdjustPlanRequireInfo> {

    @Resource
    private IMpAdjustPlanRequireInfoRemoteService mpAdjustPlanRequireInfoRemoteService;

    /** 查询计划调整需求信息列表 */
    @ApiOperation("查询计划调整需求信息列表")
    @RequiresPermissions("monthplan:adjustPlanRequireInfo:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MpAdjustPlanRequireInfo queryVO) {
        return mpAdjustPlanRequireInfoRemoteService.list(queryVO);
    }

    /** 获取计划调整需求信息详情 */
    @ApiOperation("获取计划调整需求信息详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public MpAdjustPlanRequireInfo getInfo(@PathVariable("id") Long id) {
        return mpAdjustPlanRequireInfoRemoteService.getInfo(id);
    }

    /** 新增计划调整需求信息 */
    @ApiOperation("新增计划调整需求信息")
    @RequiresPermissions("monthplan:adjustPlanRequireInfo:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody MpAdjustPlanRequireInfo entity) {
        return mpAdjustPlanRequireInfoRemoteService.add(entity);
    }

    /** 编辑计划调整需求信息 */
    @ApiOperation("编辑计划调整需求信息")
    @RequiresPermissions("monthplan:adjustPlanRequireInfo:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody MpAdjustPlanRequireInfo entity) {
        return mpAdjustPlanRequireInfoRemoteService.edit(entity);
    }

    /** 删除计划调整需求信息 */
    @ApiOperation("删除计划调整需求信息")
    @RequiresPermissions("monthplan:adjustPlanRequireInfo:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return mpAdjustPlanRequireInfoRemoteService.removeByIds(Arrays.asList(arr));
    }

    /** 产品结构下拉数据（来源 mdmSkuStructureRef，去重） */
    @ApiOperation("产品结构下拉数据")
    @RequiresPermissions("monthplan:adjustPlanRequireInfo:list")
    @GetMapping("/structureOptions")
    @ResponseBody
    public AjaxResult structureOptions(String factoryCode, String structureName) {
        return mpAdjustPlanRequireInfoRemoteService.structureOptions(factoryCode, structureName);
    }

    /** 物料编码下拉数据（来源 mdmSkuStructureRef，含物料描述反显） */
    @ApiOperation("物料编码下拉数据")
    @RequiresPermissions("monthplan:adjustPlanRequireInfo:list")
    @GetMapping("/materialOptions")
    @ResponseBody
    public AjaxResult materialOptions(String factoryCode, String structureName, String materialCode) {
        return mpAdjustPlanRequireInfoRemoteService.materialOptions(factoryCode, structureName, materialCode);
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.mpAdjustPlanInfo.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载计划调整需求信息导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<MpAdjustPlanRequireInfo> util = new ExcelUtil<>(MpAdjustPlanRequireInfo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出计划调整需求信息 */
    @ApiOperation("导出计划调整需求信息")
    @RequiresPermissions("monthplan:adjustPlanRequireInfo:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MpAdjustPlanRequireInfo entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = mpAdjustPlanRequireInfoRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入计划调整需求信息 */
    @ApiOperation("导入计划调整需求信息")
    @RequiresPermissions("monthplan:adjustPlanRequireInfo:import")
    @PostMapping("/importData")
    @ResponseBody
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(getFunctionName());
        context.setProcedureCode(getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return mpAdjustPlanRequireInfoRemoteService.importData(context, updateSupport);
    }
}
