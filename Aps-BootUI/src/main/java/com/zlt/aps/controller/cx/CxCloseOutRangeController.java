package com.zlt.aps.controller.cx;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Value;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.zlt.aps.common.utils.ImportUtil;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.io.*;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import com.zlt.aps.cx.api.domain.entity.CxCloseOutRange;
import com.zlt.aps.cx.api.service.ICxCloseOutRangeService;

/**
 * 成型收尾范围系数Controller
 * @author zlt
 * @date 2021-12-28
 */
@Api(tags = "成型收尾范围系数")
@Controller
@RequestMapping("/cx/closeOutRange")
public class CxCloseOutRangeController extends BaseController {

    @Autowired
    private ICxCloseOutRangeService iCxCloseOutRangeService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "cx/closeOutRange";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:closeOutRange:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/closeOutRange";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxCloseOutRange", new CxCloseOutRange());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxCloseOutRange", iCxCloseOutRangeService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询成型收尾范围系数列表
     */
    @ApiOperation("根据条件查询成型收尾范围系数列表")
    @RequiresPermissions("cx:closeOutRange:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxCloseOutRange entity) {
        return iCxCloseOutRangeService.list(entity);
    }

    /**
     * 修改或新增成型收尾范围系数
     */
    @ApiOperation("修改或新增成型收尾范围系数")
    @RequiresPermissions("cx:closeOutRange:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxCloseOutRange cxCloseOutRange) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iCxCloseOutRangeService.checkCxCloseOutRangeUnique(cxCloseOutRange))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mouthPlate.RangeAlreadyExists"));
        }
        if (cxCloseOutRange.getId() != null){
            ajaxResult = iCxCloseOutRangeService.edit(cxCloseOutRange);
        } else{
            ajaxResult = iCxCloseOutRangeService.add(cxCloseOutRange);
        }
        return ajaxResult;
    }

    /**
     * 删除成型收尾范围系数
     */
    @ApiOperation("删除成型收尾范围系数（id不为空）")
    @RequiresPermissions("cx:closeOutRange:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxCloseOutRangeService.remove(arr);
    }

    /**
     * 校验成型收尾范围系数唯一性
     */
    @ApiOperation("校验成型收尾范围系数唯一性")
    @PostMapping("/checkCxCloseOutRangeUnique")
    @ResponseBody
    public String checkCxCloseOutRangeUnique(CxCloseOutRange cxCloseOutRange) {
        return iCxCloseOutRangeService.checkCxCloseOutRangeUnique(cxCloseOutRange);
    }

    /**
     * 导出成型收尾范围系数
     */
    @ApiOperation("导出成型收尾范围系数")
    @RequiresPermissions("cx:closeOutRange:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,CxCloseOutRange cxCloseOutRange) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.closeOutRange.modelName");
        List<CxCloseOutRange> list = iCxCloseOutRangeService.getList(cxCloseOutRange);
        ExcelUtil<CxCloseOutRange> util = new ExcelUtil<>(CxCloseOutRange. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxCloseOutRange.toString(),"ApsConstant.PROCEDURE_CODE_XXX");
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
        String fileName = I18nUtil.getMessage("ui.data.column.closeOutRange.modelName");
        ExcelUtil<CxCloseOutRange> util = new ExcelUtil<>(CxCloseOutRange.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file 要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("cx:closeOutRange:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, "ApsConstant.PROCEDURE_CODE_XXX",
                I18nUtil.getMessage("ui.data.column.closeOutRange.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<CxCloseOutRange> util = new ExcelUtil<>(CxCloseOutRange.class);
        InputStream in = new ByteArrayInputStream(data);
        List<CxCloseOutRange> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxCloseOutRangeService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
