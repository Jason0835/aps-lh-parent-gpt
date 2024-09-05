package com.zlt.aps.controller.nc;


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
import com.zlt.aps.nc.api.domain.dto.NcGlueOrderDto;
import com.zlt.aps.nc.api.service.INcGlueOrderService;
import com.zlt.aps.template.nc.NcGlueOrderTemp;
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

@Api(tags = {"内衬胶料顺序维护接口"})
@Controller
@RequestMapping("/nc/glueOrder")
public class NcGlueOrderController extends BaseController {

    private String prefix = "nc/glueOrder";

    @Resource
    private INcGlueOrderService iNcGlueOrderService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @RequiresPermissions("nc:glueOrder:view")
    @GetMapping()
    public String glueOrder() {
        return prefix + "/glueOrder";
    }

    @ApiOperation("根据条件查询胶料顺序列表")
//    @RequiresPermissions("nc:glueOrder:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(NcGlueOrderDto dto) {
        return iNcGlueOrderService.listGlueOrder(dto);
    }

    @ApiOperation("跳转到胶料顺序新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("glueOrder", new NcGlueOrderDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取胶料顺序信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("glueOrder", iNcGlueOrderService.getGlueOrder(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改胶料顺序(id为空则进行新增，id不为空则进行修改)")
//    @RequiresPermissions("nc:glueOrder:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueOrder(NcGlueOrderDto dto) {
        return iNcGlueOrderService.saveGlueOrder(dto);
    }

    @ApiOperation("根据code判断胶料组号是否已经存在")
    @PostMapping("/checkGlueCodeUnique")
    @ResponseBody
    public String checkGlueCodeUnique(NcGlueOrderDto dto) {
        return iNcGlueOrderService.checkGlueCodeUnique(dto);
    }

    @ApiOperation("刪除胶料顺序")
//    @RequiresPermissions("nc:glueOrder:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iNcGlueOrderService.deleteGlueOrder(arr);
    }

    @ApiOperation("导出胶料顺序")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, NcGlueOrderDto dto) throws IOException {
        List<NcGlueOrderDto> list = iNcGlueOrderService.exportData(dto);
        ExcelUtil<NcGlueOrderDto> util = new ExcelUtil(NcGlueOrderDto.class);
        String fileName = I18nUtil.getMessage("ui.nc.glueOrder.column.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_NC);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.nc.glueOrder.column.modalName");
        ExcelUtil<NcGlueOrderTemp> util = new ExcelUtil<>(NcGlueOrderTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("nc:glueOrder:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_NC,
                I18nUtil.getMessage("ui.nc.glueOrder.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<NcGlueOrderDto> util = new ExcelUtil<>(NcGlueOrderDto.class);
        List<NcGlueOrderDto> list = util.importExcel(in);
        // 导入数据
        AjaxResult ajaxResult = iNcGlueOrderService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
