package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.api.service.ICxMachineInfoService;
import com.zlt.aps.cx.api.service.ICxPrecisionPlanRemoteService;
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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = "成型精度计划")
@Controller
@RequestMapping("/cx/cxPrecisionPlan")
public class CxPrecisionPlanUIController extends BaseUIController<CxPrecisionPlan> {

    @Autowired
    private ICxPrecisionPlanRemoteService cxPrecisionPlanRemoteService;

    @Resource
    private ICxMachineInfoService cxMachineInfoService;

    private final String prefix = "aps/cx/cxPrecisionPlan";

    @RequiresPermissions("cx:cxPrecisionPlan:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/cxPrecisionPlan";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxPrecisionPlan", new CxPrecisionPlan());
        return prefix + "/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxPrecisionPlan", cxPrecisionPlanRemoteService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询机台下拉")
    @PostMapping("/getMachineList")
    @ResponseBody
    public AjaxResult getMachineList( CxMachineInfo query) {
        CxMachineInfo machineQuery = query == null ? new CxMachineInfo() : query;
//        List<CxMachineInfo> machineInfos = cxPrecisionPlanRemoteService.list2(machineQuery);
//        if (machineInfos == null || machineInfos.isEmpty()) {
//            return AjaxResult.success();
//        }
//
//        List<Map<String, String>> machineCodeList = machineInfos.stream()
//            .filter(machineInfo -> machineInfo != null && machineInfo.getMachineCode() != null)
//            .map(machineInfo -> {
//                Map<String, String> map = new HashMap<>(1);
//                map.put("machineCode", machineInfo.getMachineCode());
//                return map;
//            })
//            .distinct()
//            .collect(Collectors.toList());
        return AjaxResult.success();
    }

    @ApiOperation("查询列表")
    @RequiresPermissions("cx:cxPrecisionPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxPrecisionPlan cxPrecisionPlan) {
        return cxPrecisionPlanRemoteService.list(cxPrecisionPlan);
    }

    @ApiOperation("保存单据")
    @RequiresPermissions("cx:cxPrecisionPlan:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(CxPrecisionPlan cxPrecisionPlan) {
        if (UserConstants.NOT_UNIQUE.equals(cxPrecisionPlanRemoteService.checkUnique(cxPrecisionPlan))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.notUnique"));
        }
        return cxPrecisionPlanRemoteService.save(cxPrecisionPlan);
    }

    @ApiOperation("删除单据")
    @RequiresPermissions("cx:cxPrecisionPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cxPrecisionPlanRemoteService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(CxPrecisionPlan entity) {
        return cxPrecisionPlanRemoteService.checkUnique(entity);
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.cxPrecisionPlan.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<CxPrecisionPlan> util = new ExcelUtil<>(CxPrecisionPlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("导出数据")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, CxPrecisionPlan entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = cxPrecisionPlanRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("导入数据")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return cxPrecisionPlanRemoteService.importData(context, updateSupport);
    }

    @ApiOperation("从MES同步生成计划")
    @RequiresPermissions("cx:cxPrecisionPlan:sync")
    @PostMapping("/generateFromMes")
    @ResponseBody
    public AjaxResult generateFromMes(@RequestParam(value = "year", required = false) Integer year) {
        return cxPrecisionPlanRemoteService.generatePlansFromMes(year);
    }

    @ApiOperation("自动生成年计划")
    @RequiresPermissions("cx:cxPrecisionPlan:generate")
    @PostMapping("/autoGenerateYearly")
    @ResponseBody
    public AjaxResult autoGenerateYearly(@RequestParam("year") Integer year) {
        return cxPrecisionPlanRemoteService.autoGenerateYearlyPlans(year);
    }

    @ApiOperation("批量更新到期天数")
    @PostMapping("/batchUpdateDaysToDue")
    @ResponseBody
    public AjaxResult batchUpdateDaysToDue() {
        return cxPrecisionPlanRemoteService.batchUpdateDaysToDue();
    }
}
