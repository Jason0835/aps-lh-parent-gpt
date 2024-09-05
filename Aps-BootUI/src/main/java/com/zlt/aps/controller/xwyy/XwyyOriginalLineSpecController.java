package com.zlt.aps.controller.xwyy;


import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyOriginalLineSpec;
import com.zlt.aps.xwyy.api.service.IXwyyOriginalLineSpecService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
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
import java.io.InputStream;
import java.util.List;

@Api(tags = {"纤维压延原线规格管理接口"})
@Controller
@RequestMapping("/xwyy/originalLineSpec")
public class XwyyOriginalLineSpecController extends BaseController {

    private final String prefix = "xwyy/originalLineSpec";

    @Resource
    private IXwyyOriginalLineSpecService iXwyyOriginalLineSpecService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @RequiresPermissions("xwyy:originalLineSpec:view")
    @GetMapping()
    public String originalLineSpec() {
        return prefix + "/originalLineSpec";
    }

    @ApiOperation("根据条件查询原线规格管理列表")
    @RequiresPermissions("xwyy:originalLineSpec:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyOriginalLineSpec dto) {
        return iXwyyOriginalLineSpecService.listOriginalLineSpec(dto);
    }

    @ApiOperation("跳转到原线规格管理新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("originalLineSpec", new XwyyOriginalLineSpec());
        return prefix + "/edit";
    }

    @ApiOperation("获取原线规格管理信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("originalLineSpec", iXwyyOriginalLineSpecService.getOriginalLineSpec(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改原线规格管理(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions("xwyy:originalLineSpec:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveOriginalLineSpec(XwyyOriginalLineSpec dto) {
        return iXwyyOriginalLineSpecService.saveOriginalLineSpec(dto);
    }

    @ApiOperation("根据code判断是否已经存在")
    @PostMapping("/checkOriginalLineSpecCodeUnique")
    @ResponseBody
    public String checkOriginalLineSpecCodeUnique(XwyyOriginalLineSpec dto) {
        return iXwyyOriginalLineSpecService.checkOriginalLineSpecCodeUnique(dto);
    }

    @ApiOperation("刪除原线规格管理")
    @RequiresPermissions("xwyy:originalLineSpec:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iXwyyOriginalLineSpecService.deleteOriginalLineSpec(arr);
    }

    @ApiOperation("导出原线规格管理")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, XwyyOriginalLineSpec dto) throws IOException {
        List<XwyyOriginalLineSpec> list = iXwyyOriginalLineSpecService.exportData(dto);
        ExcelUtil<XwyyOriginalLineSpec> util = new ExcelUtil<>(XwyyOriginalLineSpec.class);
        String fileName = I18nUtil.getMessage("ui.xwyy.originalLineSpec.column.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_XWYY);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.xwyy.originalLineSpec.column.modalName");
        ExcelUtil<XwyyOriginalLineSpec> util = new ExcelUtil<>(XwyyOriginalLineSpec.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("xwyy:originalLineSpec:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<XwyyOriginalLineSpec> util = new ExcelUtil<>(XwyyOriginalLineSpec.class);

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_XWYY,
                I18nUtil.getMessage("ui.xwyy.originalLineSpec.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        List<XwyyOriginalLineSpec> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = iXwyyOriginalLineSpecService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
