package com.zlt.aps.controller.cd15;

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
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.api.service.ICd15StockService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd15.Cd15StockTemp;
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
 * 15°裁断库存信息Controller
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/cd15/stock")
@Api(tags = {"15°裁断库存信息维护接口"})
public class Cd15StockController extends BaseController {
    private String prefix = "cd15/stock";

    @Autowired
    private ICd15StockService stockService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至15°裁断库存列表页面
     */
    @RequiresPermissions("cd15:stock:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/stock";
    }

    /**
     * 跳转至新增15°裁断库存页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("stock", new Cd15Stock());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    /**
     * 跳转至修改15°裁断库存页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    /**
     * 跳转至15°裁断库存修正页面
     */
    @GetMapping("/modifyStock/{id}")
    public String modifyStock(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "2");
        return prefix + "/edit";
    }

    /**
     * 15°裁断库存信息列表
     */
    @ApiOperation("查询15°裁断库存信息列表")
    @RequiresPermissions("cd15:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15Stock stock) {
        return stockService.list(stock);
    }

    /**
     * 删除15°裁断库存信息
     */
    @ApiOperation("删除15°裁断库存信息")
    @RequiresPermissions("cd15:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return stockService.remove(arr);
    }

    /**
     * 新增15°裁断库存信息
     */
    @ApiOperation("新增15°裁断库存信息")
    @RequiresPermissions("cd15:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(Cd15Stock stock) {
        return stockService.add(stock);
    }

    /**
     * 修改15°裁断库存信息
     */
    @ApiOperation("修改15°裁断库存信息")
    @RequiresPermissions("cd15:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(Cd15Stock stock) {
        AjaxResult ajaxResult = null;
        //id为空则是新增操作，否则是编辑
        if (stock.getId() != null) {
            ajaxResult = stockService.edit(stock);
        } else {
            ajaxResult = stockService.add(stock);
        }
        return ajaxResult;
    }

    @ApiOperation("导出15°裁断库存信息")
    @RequiresPermissions("cd15:stock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd15Stock stock) throws IOException {
        List<Cd15Stock> list = stockService.exportList(stock);
        ExcelUtil<Cd15Stock> util = new ExcelUtil(Cd15Stock.class);
        String fileName = I18nUtil.getMessage("ui.cd15.stock.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(), ApsConstant.PROCEDURE_CODE_CD15);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cd15.stock.export.fileName");
        ExcelUtil<Cd15StockTemp> util = new ExcelUtil<>(Cd15StockTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("cd15:stock:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<Cd15Stock> util = new ExcelUtil<>(Cd15Stock.class);
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD15,
                I18nUtil.getMessage("ui.cd15.stock.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        List<Cd15Stock> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = stockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
