package com.zlt.aps.controller.cx;

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
import com.zlt.aps.cx.api.domain.entity.CxShareMoldInfo;
import com.zlt.aps.cx.api.service.ICxShareMoldInfoService;
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
import java.util.List;

/**
 * 成型胎胚共用模具信息Controller
 *
 * @author chen
 * @date 2022-03-22
 */
@Api(tags = "成型胎胚共用模具信息")
@Controller
@RequestMapping("/cx/shareMoldInfo")
public class CxShareMoldInfoController extends BaseController {

    @Autowired
    private ICxShareMoldInfoService iCxShareMoldInfoService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "cx/shareMoldInfo";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:shareMoldInfo:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/shareMoldInfo";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxShareMoldInfo", new CxShareMoldInfo());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxShareMoldInfo", iCxShareMoldInfoService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询成型胎胚共用模具信息列表
     */
    @ApiOperation("根据条件查询成型胎胚共用模具信息列表")
    @RequiresPermissions("cx:shareMoldInfo:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxShareMoldInfo entity) {
        return iCxShareMoldInfoService.list(entity);
    }

    /**
     * 修改或新增成型胎胚共用模具信息
     */
    @ApiOperation("修改或新增成型胎胚共用模具信息")
    @RequiresPermissions("cx:shareMoldInfo:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxShareMoldInfo cxShareMoldInfo) {
        AjaxResult ajaxResult = null;
        if (cxShareMoldInfo.getId() != null) {
            ajaxResult = iCxShareMoldInfoService.edit(cxShareMoldInfo);
        } else {
            ajaxResult = iCxShareMoldInfoService.add(cxShareMoldInfo);
        }
        return ajaxResult;
    }

    /**
     * 删除成型胎胚共用模具信息
     */
    @ApiOperation("删除成型胎胚共用模具信息（id不为空）")
    @RequiresPermissions("cx:shareMoldInfo:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxShareMoldInfoService.remove(arr);
    }

    /**
     * 校验成型胎胚共用模具信息唯一性
     */
    @ApiOperation("校验成型胎胚共用模具信息唯一性")
    @PostMapping("/checkCxShareMoldInfoUnique")
    @ResponseBody
    public String checkCxShareMoldInfoUnique(CxShareMoldInfo cxShareMoldInfo) {
        return iCxShareMoldInfoService.checkCxShareMoldInfoUnique(cxShareMoldInfo);
    }

    /**
     * 导出成型胎胚共用模具信息
     */
    @ApiOperation("导出成型胎胚共用模具信息")
    @RequiresPermissions("cx:shareMoldInfo:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxShareMoldInfo cxShareMoldInfo) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.shareMoldInfo.modelName");
        List<CxShareMoldInfo> list = iCxShareMoldInfoService.getList(cxShareMoldInfo);
        ExcelUtil<CxShareMoldInfo> util = new ExcelUtil<>(CxShareMoldInfo.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxShareMoldInfo.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载导入模板
     *
     * @param response 下载的模板文件
     * @throws IOException 异常
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.shareMoldInfo.modelName");
        ExcelUtil<CxShareMoldInfo> util = new ExcelUtil<>(CxShareMoldInfo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file          要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("cx:shareMoldInfo:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.shareMoldInfo.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<CxShareMoldInfo> util = new ExcelUtil<>(CxShareMoldInfo.class);
        InputStream in = new ByteArrayInputStream(data);
        List<CxShareMoldInfo> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxShareMoldInfoService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
