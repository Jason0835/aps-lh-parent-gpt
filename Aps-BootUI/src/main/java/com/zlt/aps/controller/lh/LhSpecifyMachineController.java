package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;
import com.zlt.aps.lh.api.service.ILhSpecifyMachineService;
import com.zlt.aps.template.lh.LhSpecifyMachineTemp;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 硫化定点机台信息Controller
 *
 * @author zlt
 * @date 2021-07-21
 */
@Api(tags = "硫化定点机台信息")
@Controller
@RequestMapping("/lh/lhSpecifyMachine")
public class LhSpecifyMachineController extends BaseController {

    private final String prefix = "lh/lhSpecifyMachine";

    @Autowired
    private ILhSpecifyMachineService iLhSpecifyMachineService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    /**
     * 跳转至主页面
     */
    @RequiresPermissions("lh:lhSpecifyMachine:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/lhSpecifyMachine";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("lhSpecifyMachine", new LhSpecifyMachine());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhSpecifyMachine", iLhSpecifyMachineService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询硫化定点机台信息列表
     */
    @ApiOperation("根据条件查询硫化定点机台信息列表")
    @RequiresPermissions("lh:lhSpecifyMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhSpecifyMachine entity) {
        return iLhSpecifyMachineService.list(entity);
    }

    /**
     * 修改或新增硫化定点机台信息
     */
    @ApiOperation("修改或新增硫化定点机台信息")
    @RequiresPermissions({"lh:lhSpecifyMachine:edit", "lh:lhSpecifyMachine:add"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(LhSpecifyMachine lhSpecifyMachine) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iLhSpecifyMachineService.checkLhSpecifyMachineUnique(lhSpecifyMachine))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.lh.lhSpecifyMachine.UniqueCheck"));
        }
        if (lhSpecifyMachine.getId() != null) {
            ajaxResult = iLhSpecifyMachineService.edit(lhSpecifyMachine);
        } else {
            ajaxResult = iLhSpecifyMachineService.add(lhSpecifyMachine);
        }
        return ajaxResult;
    }

    /**
     * 删除硫化定点机台信息
     */
    @ApiOperation("删除硫化定点机台信息（id不为空）")
    @RequiresPermissions("lh:lhSpecifyMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhSpecifyMachineService.remove(arr);
    }

    /**
     * 导出硫化定点机台信息
     */
    @ApiOperation("导出硫化定点机台信息")
    @RequiresPermissions("lh:lhSpecifyMachine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, LhSpecifyMachine lhSpecifyMachine) throws IOException {
        List<LhSpecifyMachine> list = iLhSpecifyMachineService.getList(lhSpecifyMachine);
        ExcelUtil<LhSpecifyMachine> util = new ExcelUtil<>(LhSpecifyMachine.class);
        String fileName = I18nUtil.getMessage("ui.lh.lhSpecifyMachine.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lhSpecifyMachine.toString(), ApsConstant.PROCEDURE_CODE_LH);
        iExportLogService.add(exportLog);
    }


    /**
     * 下载模板
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.lh.lhSpecifyMachine.export.fileName");
        ExcelUtil<LhSpecifyMachineTemp> util = new ExcelUtil<>(LhSpecifyMachineTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("lh:lhSpecifyMachine:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_LH,
                I18nUtil.getMessage("ui.lh.lhSpecifyMachine.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<LhSpecifyMachine> util = new ExcelUtil<>(LhSpecifyMachine.class);
        List<LhSpecifyMachine> list = new ArrayList<>();
        try {
            list = util.importExcel(in);
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        AjaxResult ajaxResult = iLhSpecifyMachineService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
