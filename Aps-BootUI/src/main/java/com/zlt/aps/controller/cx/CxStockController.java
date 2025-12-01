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
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.api.service.ICxProductConstructionInfoService;
import com.zlt.aps.cx.api.service.ICxStockService;
import com.zlt.aps.template.cx.CxStockTemp;
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
 * 成型库存信息Controller
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/cx/stock")
@Api(tags = {"成型胶库存信息维护接口"})
public class CxStockController extends BaseController {

    private String prefix = "cx/stock";

    @Autowired
    private ICxStockService cxStockService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private ICxProductConstructionInfoService iCxProductConstructionInfoService;

    @RequiresPermissions("cx:stock:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/stock";
    }

    /**
     * 跳转至新增胎侧库存页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxStock", new CxStock());
        mmap.put("editType", "0");
        List<CxProductConstructionInfo> pcList = new ArrayList<CxProductConstructionInfo>();
        mmap.put("embryoVersions", pcList);
        return prefix + "/edit";
    }


    /**
     * 获取胎胚版本列表
     */
    @ApiOperation("获取胎胚版本列表")
    @PostMapping("/getProductEmbryoVersions")
    @ResponseBody
    public AjaxResult getEmbryoVersions(CxProductConstructionInfo cxProductConstructionInfo) {
        List<CxProductConstructionInfo> pcList = iCxProductConstructionInfoService.getList(cxProductConstructionInfo);
        return AjaxResult.success(pcList);
    }

    /**
     * 跳转至修改胎侧库存页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxStock", cxStockService.selectCxStockById(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    /**
     * 跳转至胎侧库存修正页面
     */
    @GetMapping("/modifyStock/{id}")
    public String modifyStock(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxStock", cxStockService.selectCxStockById(id));
        mmap.put("editType", "2");
        List<CxProductConstructionInfo> pcList = new ArrayList<CxProductConstructionInfo>();
        mmap.put("embryoVersions", pcList);
        return prefix + "/edit";
    }

    /**
     * 成型库存信息列表
     */
    @ApiOperation("查询成型胶库存信息列表")
    @RequiresPermissions("cx:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxStock cxStock) {
        return cxStockService.list(cxStock);
    }

    /**
     * 删除成型库存信息
     */
    @ApiOperation("删除成型胶库存信息")
    @RequiresPermissions("cx:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cxStockService.remove(arr);
    }

    /**
     * 新增成型库存信息
     */
    @ApiOperation("新增成型胶库存信息")
    @RequiresPermissions("cx:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(CxStock cxStock) {
        return cxStockService.add(cxStock);
    }

    /**
     * 修改成型库存信息
     */
    @ApiOperation("修改成型胶库存信息")
    @RequiresPermissions("cx:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxStock cxStock) {
        return cxStockService.edit(cxStock);
    }

    @ApiOperation("导出成型胶库存信息")
    @RequiresPermissions("cx:stock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxStock stock) throws IOException {
        List<CxStock> list = cxStockService.exportList(stock);
        ExcelUtil<CxStock> util = new ExcelUtil<>(CxStock.class);
        String fileName = I18nUtil.getMessage("ui.cx.stock.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cx.stock.export.fileName");
        ExcelUtil<CxStockTemp> util = new ExcelUtil<>(CxStockTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("cx:stock:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.cx.stock.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxStock> util = new ExcelUtil<>(CxStock.class);
        List<CxStock> list = util.importExcel(in);
        AjaxResult ajaxResult = cxStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
