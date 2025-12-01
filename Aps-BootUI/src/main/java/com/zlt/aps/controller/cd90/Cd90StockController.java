package com.zlt.aps.controller.cd90;

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
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.api.service.ICd90StockService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd90.Cd90StockTemp;
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
 * 90°裁断库存信息Controller
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/cd90/stock")
@Api(tags = {"90°裁断库存信息维护接口"})
public class Cd90StockController extends BaseController {
    private String prefix = "cd90/stock";

    @Autowired
    private ICd90StockService stockService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至90°裁断库存列表页面
     */
    @RequiresPermissions("cd90:stock:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/stock";
    }

    /**
     * 跳转至新增90°裁断库存页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("stock", new Cd90Stock());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    /**
     * 跳转至修改90°裁断库存页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    /**
     * 跳转至90°裁断库存修正页面
     */
    @GetMapping("/modifyStock/{id}")
    public String modifyStock(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "2");
        return prefix + "/edit";
    }

    /**
     * 90°裁断库存信息列表
     */
    @ApiOperation("查询90°裁断库存信息列表")
    @RequiresPermissions("cd90:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90Stock stock) {
        return stockService.list(stock);
    }

    /**
     * 删除90°裁断库存信息
     */
    @ApiOperation("删除90°裁断库存信息")
    @RequiresPermissions("cd90:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return stockService.remove(arr);
    }

    /**
     * 新增90°裁断库存信息
     */
    @ApiOperation("新增90°裁断库存信息")
    @RequiresPermissions("cd90:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(Cd90Stock stock) {
        return stockService.add(stock);
    }

    /**
     * 修改90°裁断库存信息
     */
    @ApiOperation("修改90°裁断库存信息")
    @RequiresPermissions("cd90:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(Cd90Stock stock) {
        AjaxResult ajaxResult = null;
        //id为空则是新增操作，否则是编辑
        if (stock.getId() != null) {
            ajaxResult = stockService.edit(stock);
        } else {
            ajaxResult = stockService.add(stock);
        }
        return ajaxResult;
    }

    @ApiOperation("导出90°裁断库存信息")
    @RequiresPermissions("cd90:stock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd90Stock stock) throws IOException {
        List<Cd90Stock> list = stockService.exportList(stock);
        ExcelUtil<Cd90Stock> util = new ExcelUtil(Cd90Stock.class);
        String fileName = I18nUtil.getMessage("ui.cd90.stock.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(), ApsConstant.PROCEDURE_CODE_CD90);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     *
     * @param response
     * @throws IOException
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cd90.stock.export.fileName");
        ExcelUtil<Cd90StockTemp> util = new ExcelUtil<>(Cd90StockTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("cd90:stock:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD90, I18nUtil.getMessage("ui.cd90.stock.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<Cd90Stock> util = new ExcelUtil<>(Cd90Stock.class);
        List<Cd90Stock> list = util.importExcel(in);
        AjaxResult ajaxResult = stockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }


}
