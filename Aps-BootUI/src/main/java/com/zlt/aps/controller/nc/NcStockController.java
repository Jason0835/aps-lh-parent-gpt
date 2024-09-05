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
import com.zlt.aps.nc.api.domain.entity.NcStock;
import com.zlt.aps.nc.api.service.INcStockService;
import com.zlt.aps.template.nc.NcStockTemp;
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
 * 内衬库存信息Controller
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/nc/stock")
@Api(tags = {"内衬胶库存信息维护接口"})
public class NcStockController extends BaseController {
    private String prefix = "nc/stock";

    @Autowired
    private INcStockService stockService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;

    /**
     * 跳转至内衬库存列表页面
     */
    @RequiresPermissions("nc:stock:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/stock";
    }

    /**
     * 跳转至新增内衬库存页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("stock", new NcStock());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    /**
     * 跳转至修改内衬库存页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    /**
     * 跳转至内衬库存修正页面
     */
    @GetMapping("/modifyStock/{id}")
    public String modifyStock(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "2");
        return prefix + "/edit";
    }

    /**
     * 内衬库存信息列表
     */
    @ApiOperation("查询内衬胶库存信息列表")
    @RequiresPermissions("nc:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(NcStock stock) {
        return stockService.list(stock);
    }

    /**
     * 删除内衬库存信息
     */
    @ApiOperation("删除内衬胶库存信息")
    @RequiresPermissions("nc:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return stockService.remove(arr);
    }

    /**
     * 新增内衬库存信息
     */
    @ApiOperation("新增内衬胶库存信息")
    @RequiresPermissions("nc:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(NcStock stock) {
        return stockService.add(stock);
    }

    /**
     * 修改内衬库存信息
     */
    @ApiOperation("修改内衬胶库存信息")
    @RequiresPermissions("nc:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(NcStock stock) {
        return stockService.edit(stock);
    }

    @ApiOperation("导出内衬胶库存信息")
    @RequiresPermissions("nc:stock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, NcStock stock) throws IOException {
        List<NcStock> list = stockService.exportList(stock);
        ExcelUtil<NcStock> util = new ExcelUtil(NcStock.class);
        String fileName = I18nUtil.getMessage("ui.nc.stock.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(), ApsConstant.PROCEDURE_CODE_NC);
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
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.nc.stock.export.fileName");
        ExcelUtil<NcStockTemp> util = new ExcelUtil<>(NcStockTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("nc:stock:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_NC,
                I18nUtil.getMessage("ui.nc.stock.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //解析文件
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<NcStock> util = new ExcelUtil<>(NcStock.class);
        List<NcStock> list = util.importExcel(in);
        AjaxResult ajaxResult = stockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
