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

import com.zlt.aps.cx.api.domain.entity.CxStockLocationMapping;
import com.zlt.aps.cx.api.service.ICxStockLocationMappingService;

/**
 * 库存地点映射Controller
 * @author zlt
 * @date 2021-11-15
 */
@Api(tags = "库存地点映射")
@Controller
@RequestMapping("/cx/stockLocationMapping")
public class CxStockLocationMappingController extends BaseController {

    @Autowired
    private ICxStockLocationMappingService iCxStockLocationMappingService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "cx/stockLocationMapping";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:stockLocationMapping:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/stockLocationMapping";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxStockLocationMapping", new CxStockLocationMapping());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxStockLocationMapping", iCxStockLocationMappingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询库存地点映射列表
     */
    @ApiOperation("根据条件查询库存地点映射列表")
    @RequiresPermissions("cx:stockLocationMapping:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxStockLocationMapping entity) {
        return iCxStockLocationMappingService.list(entity);
    }

    /**
     * 修改或新增库存地点映射
     */
    @ApiOperation("修改或新增库存地点映射")
    @RequiresPermissions("cx:stockLocationMapping:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxStockLocationMapping cxStockLocationMapping) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iCxStockLocationMappingService.checkCxStockLocationMappingUnique(cxStockLocationMapping))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mouthPlate.alreadyExists"));
        }
        if (cxStockLocationMapping.getId() != null){
            ajaxResult = iCxStockLocationMappingService.edit(cxStockLocationMapping);
        } else{
            ajaxResult = iCxStockLocationMappingService.add(cxStockLocationMapping);
        }
        return ajaxResult;
    }

    /**
     * 删除库存地点映射
     */
    @ApiOperation("删除库存地点映射（id不为空）")
    @RequiresPermissions("cx:stockLocationMapping:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxStockLocationMappingService.remove(arr);
    }

    /**
     * 校验库存地点映射唯一性
     */
    @ApiOperation("校验库存地点映射唯一性")
    @PostMapping("/checkCxStockLocationMappingUnique")
    @ResponseBody
    public String checkCxStockLocationMappingUnique(CxStockLocationMapping cxStockLocationMapping) {
        return iCxStockLocationMappingService.checkCxStockLocationMappingUnique(cxStockLocationMapping);
    }

    /**
     * 导出库存地点映射
     */
    @ApiOperation("导出库存地点映射")
    @RequiresPermissions("cx:stockLocationMapping:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,CxStockLocationMapping cxStockLocationMapping) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.stockLocationMapping.modelName");
        List<CxStockLocationMapping> list = iCxStockLocationMappingService.getList(cxStockLocationMapping);
        ExcelUtil<CxStockLocationMapping> util = new ExcelUtil<>(CxStockLocationMapping. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxStockLocationMapping.toString(),ApsConstant.PROCEDURE_CODE_CX);
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
        String fileName = I18nUtil.getMessage("ui.data.column.stockLocationMapping.modelName");
        ExcelUtil<CxStockLocationMapping> util = new ExcelUtil<>(CxStockLocationMapping.class);
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
    @RequiresPermissions("cx:stockLocationMapping:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.stockLocationMapping.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<CxStockLocationMapping> util = new ExcelUtil<>(CxStockLocationMapping.class);
        InputStream in = new ByteArrayInputStream(data);
        List<CxStockLocationMapping> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxStockLocationMappingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
