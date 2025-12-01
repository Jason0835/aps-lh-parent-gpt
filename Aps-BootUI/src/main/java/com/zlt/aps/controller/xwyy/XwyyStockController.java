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
import com.zlt.aps.template.xwyy.XwyyStockTemp;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
import com.zlt.aps.xwyy.api.service.IXwyyStockService;
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
 * 纤维压延库存信息Controller
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/xwyy/stock")
@Api(tags = {"纤维压延库存信息维护接口"})
public class XwyyStockController extends BaseController {
    private final String prefix = "xwyy/stock";

    @Autowired
    private IXwyyStockService stockService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至纤维压延库存列表页面
     */
    @RequiresPermissions("xwyy:stock:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/stock";
    }

    /**
     * 跳转至新增纤维压延库存页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("stock", new XwyyStock());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    /**
     * 跳转至修改纤维压延库存页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    /**
     * 跳转至纤维压延库存修正页面
     */
    @GetMapping("/modifyStock/{id}")
    public String stockRevise(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "2");
        return prefix + "/edit";
    }

    /**
     * 纤维压延库存信息列表
     */
    @ApiOperation("查询纤维压延库存信息列表")
    @RequiresPermissions("xwyy:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyStock stock) {
        return stockService.list(stock);
    }

    /**
     * 删除纤维压延库存信息
     */
    @ApiOperation("删除纤维压延库存信息")
    @RequiresPermissions("xwyy:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return stockService.remove(arr);
    }

    /**
     * 新增纤维压延库存信息
     */
    @ApiOperation("新增纤维压延库存信息")
    @RequiresPermissions("xwyy:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(XwyyStock stock) {
        return stockService.add(stock);
    }

    /**
     * 修改纤维压延库存信息
     */
    @ApiOperation("修改纤维压延库存信息")
    @RequiresPermissions("xwyy:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(XwyyStock stock) {
        AjaxResult ajaxResult = null;
        //id为空则是新增操作，否则是编辑
        if (stock.getId() != null) {
            ajaxResult = stockService.edit(stock);
        } else {
            ajaxResult = stockService.add(stock);
        }
        return ajaxResult;
    }

    @ApiOperation("导出纤维压延库存信息")
    @RequiresPermissions("xwyy:stock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, XwyyStock stock) throws IOException {
        List<XwyyStock> list = stockService.exportList(stock);
        ExcelUtil<XwyyStock> util = new ExcelUtil<>(XwyyStock.class);
        String fileName = I18nUtil.getMessage("ui.xwyy.stock.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(), ApsConstant.PROCEDURE_CODE_XWYY);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.xwyy.stock.export.fileName");
        ExcelUtil<XwyyStockTemp> util = new ExcelUtil<>(XwyyStockTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("xwyy:stock:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_XWYY, I18nUtil.getMessage("ui.xwyy.stock.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<XwyyStock> util = new ExcelUtil<>(XwyyStock.class);
        List<XwyyStock> list = util.importExcel(in);
        AjaxResult ajaxResult = stockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }


}
