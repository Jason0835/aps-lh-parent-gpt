package com.zlt.aps.controller.tq;


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
import com.zlt.aps.tq.api.domain.entity.TqAssistSpec;
import com.zlt.aps.tq.api.service.ITqAssistSpecService;
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

@Api(tags = {"胎圈外协规格管理接口"})
@Controller
@RequestMapping("/tq/assistSpec")
public class TqAssistSpecController extends BaseController {

    private String prefix = "tq/assistSpec";

    @Resource
    private ITqAssistSpecService iTqAssistSpecService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @RequiresPermissions("tq:assistSpec:view")
    @GetMapping()
    public String assistSpec() {
        return prefix + "/assistSpec";
    }

    @ApiOperation("根据条件查询外协规格管理列表")
//    @RequiresPermissions("tq:assistSpec:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqAssistSpec dto) {
        return iTqAssistSpecService.listAssistSpec(dto);
    }

    @ApiOperation("跳转到外协规格管理新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("assistSpec", new TqAssistSpec());
        return prefix + "/edit";
    }

    @ApiOperation("获取外协规格管理信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("assistSpec", iTqAssistSpecService.getAssistSpec(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改外协规格管理(id为空则进行新增，id不为空则进行修改)")
//    @RequiresPermissions("tq:assistSpec:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveAssistSpec(TqAssistSpec dto) {
        return iTqAssistSpecService.saveAssistSpec(dto);
    }

    @ApiOperation("根据code判断是否已经存在")
    @PostMapping("/checkAssistSpecCodeUnique")
    @ResponseBody
    public String checkAssistSpecCodeUnique(TqAssistSpec dto) {
        return iTqAssistSpecService.checkAssistSpecCodeUnique(dto);
    }

    @ApiOperation("刪除外协规格管理")
//    @RequiresPermissions("tq:assistSpec:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqAssistSpecService.deleteAssistSpec(arr);
    }

    @ApiOperation("导出外协规格管理")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TqAssistSpec dto) throws IOException {
        List<TqAssistSpec> list = iTqAssistSpecService.exportData(dto);
        ExcelUtil<TqAssistSpec> util = new ExcelUtil<>(TqAssistSpec.class);
        String fileName = I18nUtil.getMessage("ui.tq.assistSpec.column.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TQ);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.tq.assistSpec.column.modalName");
        ExcelUtil<TqAssistSpec> util = new ExcelUtil<>(TqAssistSpec.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("tq:assistSpec:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TqAssistSpec> util = new ExcelUtil<>(TqAssistSpec.class);

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TQ,
                I18nUtil.getMessage("ui.tq.assistSpec.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        List<TqAssistSpec> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = iTqAssistSpecService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
