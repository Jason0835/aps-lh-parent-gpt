package com.zlt.aps.monthplan.setting.controller;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.service.IMdmDeviceMaintenancePlanService;
import com.zlt.aps.monthplan.api.domain.entity.MdmDeviceMaintenancePlan;
import com.zlt.aps.monthplan.api.domain.vo.MdmDeviceMaintenancePlanVo;
import com.zlt.common.controller.BusiController;
import com.zlt.common.utils.ImportExcelUtils;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 基础数据-设备维护计划Controller
 *
 * @author chen
 * @date 2021-09-26
 */
@RestController
@RequestMapping("/docDeviceMaintenancePlan")
@RequiredArgsConstructor
public class MdmDeviceMaintenancePlanController extends BusiController<MdmDeviceMaintenancePlanVo> {
    private final IMdmDeviceMaintenancePlanService docDeviceMaintenancePlanService;

    private final IExportLogService iExportLogService;
    private final IImportLogService iImportLogService;
    private final IImportErrorLogService iImportErrorLogService;

    /**
     * 查询基础数据-设备维护计划列表
     */
    // @PreAuthorize(hasPermi = "fac:docDeviceMaintenancePlan:list")
//     @DataAuth(docFields = {"dmm.PRODUCT_TYPE_CODE", "ddmp.FACTORY_CODE"}, docTypes = {DocTypeEnum.PRODUCT_NAME, DocTypeEnum.FACTORY_CODE})
    @ApiOperation("查询基础数据-设备维护计划列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmDeviceMaintenancePlanVo docDeviceMaintenancePlan) {
        startPage("create_time desc");
        List<MdmDeviceMaintenancePlan> list = docDeviceMaintenancePlanService.selectDocDeviceMaintenancePlanList(docDeviceMaintenancePlan);
        return getDataTable(list);
    }

    /**
     * 新增基础数据-设备维护计划
     */
    @Log(title = "ui.data.column.docDeviceMaintenancePlan.modelName", businessType = BusinessType.INSERT)
    // @PreAuthorize(hasPermi = "fac:docDeviceMaintenancePlan:add")
    @ApiOperation("新增基础数据-设备维护计划")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MdmDeviceMaintenancePlan docDeviceMaintenancePlan) {
        return toAjax(docDeviceMaintenancePlanService.insert(docDeviceMaintenancePlan));
    }

    /**
     * 修改基础数据-设备维护计划
     */
    @Log(title = "ui.data.column.docDeviceMaintenancePlan.modelName", businessType = BusinessType.UPDATE)
    // @PreAuthorize(hasPermi = "fac:docDeviceMaintenancePlan:edit")
    @ApiOperation("修改基础数据-设备维护计划")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MdmDeviceMaintenancePlan docDeviceMaintenancePlan) {
        return toAjax(docDeviceMaintenancePlanService.updateByPrimaryKey(docDeviceMaintenancePlan));
    }

    /**
     * 删除基础数据-设备维护计划
     */
    @Log(title = "ui.data.column.docDeviceMaintenancePlan.modelName", businessType = BusinessType.DELETE)
    // @PreAuthorize(hasPermi = "fac:docDeviceMaintenancePlan:remove")
    @ApiOperation("删除基础数据-设备维护计划")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(docDeviceMaintenancePlanService.deleteByIds(Arrays.asList(ids)));
    }

    @ApiOperation("根据id获取数据")
    @GetMapping("/getById/{id}")
    public MdmDeviceMaintenancePlan getById(@PathVariable("id") Long id) {
        return docDeviceMaintenancePlanService.selectByPrimaryKey(id);
    }

    @Log(title = "ui.data.column.docDeviceMaintenancePlan.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出查询设备维护计划")
    @PostMapping("/getList")
    List<MdmDeviceMaintenancePlanVo> getList(@RequestBody MdmDeviceMaintenancePlanVo entity) {
        List<MdmDeviceMaintenancePlan> list = docDeviceMaintenancePlanService.selectDocDeviceMaintenancePlanList(entity);
        List<MdmDeviceMaintenancePlanVo> docDeviceMaintenancePlanVos = new ArrayList<>(list.size());
        if (!list.isEmpty()) {
            list.forEach(l -> {
                MdmDeviceMaintenancePlanVo docDeviceMaintenancePlanVo = new MdmDeviceMaintenancePlanVo();
                BeanUtils.copyProperties(l, docDeviceMaintenancePlanVo);
                docDeviceMaintenancePlanVos.add(docDeviceMaintenancePlanVo);
            });
        }
        return docDeviceMaintenancePlanVos;
    }


    // @Log(title = "ui.data.column.docDeviceMaintenancePlan.modelName", businessType = BusinessType.IMPORT)
    // @ApiOperation("导入设备维护计划")
    // @PostMapping("/importData/{updateSupport}/{importLogId}")
    // public AjaxResult importData(@RequestBody List<MdmDeviceMaintenancePlanVo> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId) {
    //     if (CollectionUtils.isEmpty(list)) {
    //         return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
    //     }
    //     return docDeviceMaintenancePlanService.importData(list, updateSupport, importLogId);
    // }

    @Log(title = "ui.data.column.docDeviceMaintenancePlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入设备维护计划")
    @PostMapping("/importData/{updateSupport}/{planType}")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("planType") Integer planType) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<MdmDeviceMaintenancePlanVo> util = new ExcelUtil<>(MdmDeviceMaintenancePlanVo.class);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<MdmDeviceMaintenancePlanVo> list = util.importExcel(is);
        // 填充计划类型
        if (CollectionUtils.isNotEmpty(list)) {
            list.forEach(item -> item.setPlanType(planType));
        }
        AjaxResult ajaxResult = this.doImportData(list, updateSupport, importLog.getId());
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    @Override
    public AjaxResult doImportData(List<MdmDeviceMaintenancePlanVo> list, boolean updateSupport, long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return docDeviceMaintenancePlanService.importData(list, updateSupport, importLogId);
    }

    /**
     * 导出列表
     */
    @Log(title = "预计超欠产", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MdmDeviceMaintenancePlanVo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return commonExport(queryVO, fileName, response);
    }

    @Override
    public List<MdmDeviceMaintenancePlanVo> listExportData(MdmDeviceMaintenancePlanVo vo) {
        return this.getList(vo);
    }
}
