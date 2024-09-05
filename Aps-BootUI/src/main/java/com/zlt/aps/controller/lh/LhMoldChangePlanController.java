package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.api.service.ILhMoldChangePlanService;
import com.zlt.aps.template.lh.LhMoldChangePlanTemp;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * 模具变动单Controller
 *
 * @author zlt
 * @date 2021-06-17
 */
@Api(tags = "模具变动单维护接口")
@Controller
@RequestMapping("/lh/moldChange")
public class LhMoldChangePlanController extends BaseController {

    private final String prefix = "lh/moldChange";

    @Autowired
    private ILhMoldChangePlanService lhMoldChangePlanService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    /**
     * 跳转至主页面
     *
     * @return
     */
    @RequiresPermissions("lh:moldChange:view")
    @GetMapping()
    public String operlog(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/moldChange";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("lhMoldChangePlan", new LhMoldChangePlan());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhMoldChangePlan", lhMoldChangePlanService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 查询模具变动单列表
     */
    @ApiOperation("根据条件查询模具变动单")
    @RequiresPermissions("lh:moldChange:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhMoldChangePlan lhMoldChangePlan) {
        if (lhMoldChangePlan.getScheduleDate() == null) {
            lhMoldChangePlan.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return lhMoldChangePlanService.list(lhMoldChangePlan);
    }

    @ApiOperation("修改模具变动单（id不为空）")
    @RequiresPermissions({"lh:moldChange:edit", "lh:moldChange:add"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(LhMoldChangePlan lhMoldChangePlan) {
        AjaxResult ajaxResult = null;
        if (lhMoldChangePlan.getId() != null) {
            ajaxResult = lhMoldChangePlanService.edit(lhMoldChangePlan);
        } else {
            ajaxResult = lhMoldChangePlanService.add(lhMoldChangePlan);
        }
        return ajaxResult;
    }

    /**
     * 删除模具变动单
     */
    @ApiOperation("删除模具变动单（id不为空）")
    @RequiresPermissions("lh:moldChange:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return lhMoldChangePlanService.remove(arr);
    }

    /**
     * 导出模具变动单
     */
    @ApiOperation("导出模具变动单")
    @RequiresPermissions("lh:moldChange:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, LhMoldChangePlan lhMoldChangePlan) throws IOException {
        if (lhMoldChangePlan.getScheduleDate() == null) {
            lhMoldChangePlan.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        List<LhMoldChangePlan> list = lhMoldChangePlanService.getList(lhMoldChangePlan);
        ExcelUtil<LhMoldChangePlan> util = new ExcelUtil<>(LhMoldChangePlan.class);
        String fileName = I18nUtil.getMessage("ui.lh.moldChange.export.sheetName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lhMoldChangePlan.toString(), ApsConstant.PROCEDURE_CODE_LH);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.lh.moldChange.export.sheetName");
        ExcelUtil<LhMoldChangePlanTemp> util = new ExcelUtil<>(LhMoldChangePlanTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("lh:moldChange:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_LH,
                I18nUtil.getMessage("ui.lh.moldChange.export.sheetName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<LhMoldChangePlan> util = new ExcelUtil<>(LhMoldChangePlan.class);
        List<LhMoldChangePlan> list = util.importExcel(in);
        AjaxResult ajaxResult = lhMoldChangePlanService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 发布
     */
    @ApiOperation("变动单发布")
    @RequiresPermissions("lh:moldChange:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(LhMoldChangePlan lhMoldChangePlan) {
        if (lhMoldChangePlan.getScheduleDate() == null) {
            lhMoldChangePlan.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return lhMoldChangePlanService.publish(lhMoldChangePlan);
    }
}
