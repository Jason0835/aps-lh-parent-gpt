package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.common.enums.MaintenancePlanTypeEnum;
import com.zlt.aps.mp.api.domain.entity.MdmDeviceMaintenancePlan;
import com.zlt.aps.mp.api.domain.vo.MdmDeviceMaintenancePlanVo;
import com.zlt.aps.mp.api.service.IMdmDeviceMaintenancePlanRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 基础数据-设备维护计划Controller
 */
@Api(tags = "基础数据-设备维修计划")
@Controller
@RequestMapping("/fac/docDeviceMaintenancePlan/repair")
public class MdmDeviceMaintenancePlanRepairUIController extends BaseUIController<MdmDeviceMaintenancePlanVo> {

    @Autowired
    private IMdmDeviceMaintenancePlanRemoteService iMdmDeviceMaintenancePlanRemoteService;

    /**
     * 根据条件查询基础数据-设备维护计划列表
     */
    @ApiOperation("根据条件查询基础数据-设备维护计划列表")
    @RequiresPermissions("maindata:deviceRepairPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmDeviceMaintenancePlanVo entity) {
        entity.setPlanType(MaintenancePlanTypeEnum.REPAIR.getCode());
        return iMdmDeviceMaintenancePlanRemoteService.list(entity);
    }

    /**
     * 修改或新增基础数据-设备维护计划
     */
    @ApiOperation("修改或新增基础数据-设备维护计划")
    @RequiresPermissions(value = {"maindata:deviceRepairPlan:edit", "maindata:deviceRepairPlan:add"}, logical = Logical.OR)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MdmDeviceMaintenancePlan docDeviceMaintenancePlan) {
        if (docDeviceMaintenancePlan.getBeginDate() != null
                && docDeviceMaintenancePlan.getEndDay() != null
                && docDeviceMaintenancePlan.getBeginDate().after(docDeviceMaintenancePlan.getEndDay())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.timeCheck"));
        }
        docDeviceMaintenancePlan.setPlanType(MaintenancePlanTypeEnum.REPAIR.getCode());
        AjaxResult ajaxResult = null;
        if (docDeviceMaintenancePlan.getId() != null) {
            ajaxResult = iMdmDeviceMaintenancePlanRemoteService.edit(docDeviceMaintenancePlan);
        } else {
            ajaxResult = iMdmDeviceMaintenancePlanRemoteService.add(docDeviceMaintenancePlan);
        }
        return ajaxResult;
    }

    /**
     * 删除基础数据-设备维护计划
     */
    @ApiOperation("删除基础数据-设备维护计划（id不为空）")
    @RequiresPermissions("maindata:deviceRepairPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmDeviceMaintenancePlanRemoteService.remove(arr);
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }

    // @Override
    // public AjaxResult importDataByFeign(List list, boolean updateSupport, Long importLogId) {
    //     List<MdmDeviceMaintenancePlanVo> importList = (List<MdmDeviceMaintenancePlanVo>) list;
    //     if (CollectionUtils.isNotEmpty(importList)) {
    //         for (MdmDeviceMaintenancePlanVo vo : importList) {
    //             vo.setPlanType(MaintenancePlanTypeEnum.REPAIR.getCode());
    //         }
    //     }
    //     return iMdmDeviceMaintenancePlanRemoteService.importData(importList, updateSupport, importLogId);
    // }

    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.docDeviceMaintenancePlan.repairModelName");
    }

    @Override
    public String getExportTemplateFileNameForce() {
        return I18nUtil.getMessage("ui.data.column.docDeviceMaintenancePlan.repairModelName", Locale.SIMPLIFIED_CHINESE);
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmDeviceMaintenancePlan> util = new ExcelUtil<>(MdmDeviceMaintenancePlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @Override
    public String getFunctionName() {
        return getExportTemplateFileName();
    }

    @Override
    public List<MdmDeviceMaintenancePlanVo> exportDataByFeign(MdmDeviceMaintenancePlanVo entity) {
        entity.setPlanType(MaintenancePlanTypeEnum.REPAIR.getCode());
        return iMdmDeviceMaintenancePlanRemoteService.getList(entity);
    }

    @RequiresPermissions("maindata:deviceRepairPlan:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmDeviceMaintenancePlanVo entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        entity.setPlanType(MaintenancePlanTypeEnum.REPAIR.getCode());
        byte[] excelBytes = iMdmDeviceMaintenancePlanRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("maindata:deviceRepairPlan:import")
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
        return iMdmDeviceMaintenancePlanRemoteService.importData(context, true, 0);
    }
}
