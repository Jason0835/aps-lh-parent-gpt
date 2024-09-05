package com.zlt.aps.controller.gdyy;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.aps.gdyy.api.service.IGdyyStockService;
import com.zlt.aps.template.gdyy.GdyyStockTemp;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * 钢带压延库存信息Controller
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/gdyy/stock")
@Api(tags = {"钢带压延库存信息维护接口"})
public class GdyyStockController extends BaseController {
    private final String prefix = "gdyy/stock";
    @Autowired
    private IGdyyStockService stockService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    /**
     * 跳转至钢带压延库存列表页面
     */
    @RequiresPermissions("gdyy:stock:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/stock";
    }

    /**
     * 跳转至新增钢带压延库存页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("stock", new GdyyStock());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    /**
     * 跳转至修改钢带压延库存页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "1");
        // 按大卷计算
        mmap.put("isRoll", stockService.isRollStock());
        return prefix + "/edit";
    }

    /**
     * 跳转至钢带压延库存修正页面
     */
    @GetMapping("/modifyStock/{id}")
    public String modifyStock(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "2");
        // 按大卷计算
        mmap.put("isRoll", stockService.isRollStock());
        return prefix + "/edit";
    }

    /**
     * 钢带压延库存信息列表
     */
    @ApiOperation("查询钢带压延库存信息列表")
    @RequiresPermissions("gdyy:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GdyyStock stock) {
        return stockService.list(stock);
    }

    /**
     * 删除钢带压延库存信息
     */
    @ApiOperation("删除钢带压延库存信息")
    @RequiresPermissions("gdyy:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return stockService.remove(arr);
    }

    /**
     * 新增钢带压延库存信息
     */
    @ApiOperation("新增钢带压延库存信息")
    @RequiresPermissions("gdyy:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(GdyyStock stock) {
        return stockService.add(stock);
    }

    /**
     * 修改钢带压延库存信息
     */
    @ApiOperation("修改钢带压延库存信息")
    @RequiresPermissions("gdyy:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(GdyyStock stock) {
        return stockService.edit(stock);
    }

    @ApiOperation("导出钢带压延库存信息")
    @RequiresPermissions("gdyy:stock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GdyyStock stock) throws IOException {
        List<GdyyStock> list = stockService.exportList(stock);
        ExcelUtil<GdyyStock> util = new ExcelUtil<>(GdyyStock.class);
        String fileName = I18nUtil.getMessage("ui.gdyy.stock.export.sheetName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(), ApsConstant.PROCEDURE_CODE_GDYY);
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
        String fileName = I18nUtil.getMessage("ui.gdyy.stock.export.fileName");
        ExcelUtil<GdyyStockTemp> util = new ExcelUtil<>(GdyyStockTemp.class);
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
    @RequiresPermissions("gdyy:stock:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GDYY,
                I18nUtil.getMessage("ui.gdyy.stock.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<GdyyStock> util = new ExcelUtil<>(GdyyStock.class);
        List<GdyyStock> list = util.importExcel(in);
        AjaxResult ajaxResult = stockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
