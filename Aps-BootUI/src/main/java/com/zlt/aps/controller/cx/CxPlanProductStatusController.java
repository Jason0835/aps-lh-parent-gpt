package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.cx.api.domain.entity.CxPlanProductStatus;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.service.ICxPlanProductStatusService;
import com.zlt.aps.cx.api.service.ICxScheduleResultService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 成型计划投产状态Controller
 *
 * @author zlt
 * @date 2021-07-21
 */
@Api(tags = "成型计划投产状态")
@Controller
@RequestMapping("/cx/productStatus")
public class CxPlanProductStatusController extends BaseController {

    @Autowired
    private ICxPlanProductStatusService iCxPlanProductStatusService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private ICxScheduleResultService iCxScheduleResultService;

    private String prefix = "cx/productStatus";

    /**
     * 跳转至主页面
     */
    @GetMapping()
    public String operlog() {
        return prefix + "/productStatus";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxPlanProductStatus", new CxPlanProductStatus());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("editType", "1");
        mmap.put("cxPlanProductStatus", iCxPlanProductStatusService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 跳转至计划量修改页面
     */
    @GetMapping("/modifyQty/{id}")
    public String modifyQty(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("editType", "2");
        CxPlanProductStatus cxPlanProductStatus = iCxPlanProductStatusService.getInfo(id);
        //调整来源：0:投产列表，1：成型排程
        cxPlanProductStatus.setAdjustSource("0");
        mmap.put("cxPlanProductStatus", cxPlanProductStatus);
        return prefix + "/edit";
    }

    /**
     * 投产页面
     */
    @GetMapping("/production/{ids}")
    public String finishedProduction(@PathVariable("ids") String ids, ModelMap mmap) {
        Long[] arr = Convert.toLongArray(ids);
        CxPlanProductStatus cxPlanProductStatus = iCxPlanProductStatusService.getInfo(arr[0]);
        cxPlanProductStatus.setIds(ids);
        mmap.put("cxPlanProductStatus", cxPlanProductStatus);
        return prefix + "/finishedProduction";
    }


    /**
     * 标记不投产
     */
    @ApiOperation("标记不投产")
    @RequiresPermissions("cx:productStatus:markUnProduct")
    @PostMapping("/markUnProduct")
    @ResponseBody
    public AjaxResult markUnProduct(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxPlanProductStatusService.markUnProduct(arr);
    }


    /**
     * 根据条件查询成型计划投产状态列表
     */
    @ApiOperation("根据条件查询成型计划投产状态列表")
    @RequiresPermissions("cx:productStatus:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxPlanProductStatus entity) {
        if (StringUtils.isNotBlank(entity.getBeginDate())) {
            entity.setBeginDate(entity.getBeginDate().replaceAll("-", ""));
        }
        if (StringUtils.isNotBlank(entity.getEndDate())) {
            entity.setEndDate(entity.getEndDate().replaceAll("-", ""));
        }
        return iCxPlanProductStatusService.list(entity);
    }

    /**
     * 修改或新增成型计划投产状态
     */
    @ApiOperation("修改或新增成型计划投产状态")
    @RequiresPermissions("cx:productStatus:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxPlanProductStatus cxPlanProductStatus) {
        AjaxResult ajaxResult = null;
        if (cxPlanProductStatus.getId() != null) {
            ajaxResult = iCxPlanProductStatusService.edit(cxPlanProductStatus);
        } else {
            ajaxResult = iCxPlanProductStatusService.add(cxPlanProductStatus);
        }
        return ajaxResult;
    }

    /**
     * 修改计划总量
     */
    @ApiOperation("修改计划总量")
    @PostMapping("/modifyQty")
    @ResponseBody
    public AjaxResult modifyQty(CxPlanProductStatus cxPlanProductStatus) {
        return iCxPlanProductStatusService.modifyQty(cxPlanProductStatus);
    }

    /**
     * 删除成型计划投产状态
     */
    @ApiOperation("删除成型计划投产状态（id不为空）")
    @RequiresPermissions("cx:productStatus:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxPlanProductStatusService.remove(arr);
    }


    @ApiOperation("校验成型计划投产状态唯一性")
    @PostMapping("/checkCxPlanProductStatusUnique")
    @ResponseBody
    public String checkCxPlanProductStatusUnique(CxPlanProductStatus cxPlanProductStatus) {
        return iCxPlanProductStatusService.checkCxPlanProductStatusUnique(cxPlanProductStatus);
    }

    /**
     * 导出成型计划投产状态
     */
    @ApiOperation("导出成型计划投产状态")
    @RequiresPermissions("cx:productStatus:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxPlanProductStatus cxPlanProductStatus) throws IOException {
        if (StringUtils.isNotBlank(cxPlanProductStatus.getBeginDate())) {
            cxPlanProductStatus.setBeginDate(cxPlanProductStatus.getBeginDate().replaceAll("-", ""));
        }
        if (StringUtils.isNotBlank(cxPlanProductStatus.getEndDate())) {
            cxPlanProductStatus.setEndDate(cxPlanProductStatus.getEndDate().replaceAll("-", ""));
        }
        List<CxPlanProductStatus> list = iCxPlanProductStatusService.getList(cxPlanProductStatus);
        ExcelUtil<CxPlanProductStatus> util = new ExcelUtil(CxPlanProductStatus.class);
        String fileName = I18nUtil.getMessage("ui.data.column.productStatus.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxPlanProductStatus.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }

    /**
     * 投产校验
     */
    @PostMapping("/validateProduction")
    @ResponseBody
    public AjaxResult validateProduction(CxPlanProductStatus cxPlanProductStatus) {
        CxScheduleResult cxScheduleResult = new CxScheduleResult();
        cxScheduleResult.setScheduleDate(cxPlanProductStatus.getScheduleDate());
        int releasingOrTimeoutByDate = iCxScheduleResultService.isReleasingOrTimeoutByDate(cxScheduleResult);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        return iCxPlanProductStatusService.validateProduction(cxPlanProductStatus);
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/editRemark/{id}")
    public String editRemark(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxProdutStatus", iCxPlanProductStatusService.getInfo(id));
        return prefix + "/editRemark";
    }

    /**
     * 修改或新增成型计划投产状态
     */
    @ApiOperation("修改成型计划投产状态备注")
    @PostMapping("/editRemark")
    @ResponseBody
    public AjaxResult editRemarkSave(CxPlanProductStatus cxPlanProductStatus) {
        if(cxPlanProductStatus==null||cxPlanProductStatus.getId()==null){
            return AjaxResult.error();
        }

        return iCxPlanProductStatusService.editRemark(cxPlanProductStatus);
    }

}
