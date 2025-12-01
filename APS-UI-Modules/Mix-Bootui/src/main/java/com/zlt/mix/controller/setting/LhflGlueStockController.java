package com.zlt.mix.controller.setting;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.LhflGlueStock;
import com.zlt.mix.setting.api.service.ILhflGlueStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 硫磺辅料终炼库存信息Controller
 *
 * @author Liam
 * @date 2022-04-18
 */
@Api(tags = "硫磺辅料终炼库存信息")
@Controller
@RequestMapping("/setting/lhflGlueStock")
public class LhflGlueStockController extends BaseController {

    @Resource
    private ILhflGlueStockService iLhflGlueStockService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/lhflGlueStock";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:lhflGlueStock:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/lhflGlueStock";
    }

    @ApiOperation("根据条件查询硫磺辅料终炼库存信息列表")
    @RequiresPermissions("setting:lhflGlueStock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listLhflGlueStock(LhflGlueStock entity) {
        return iLhflGlueStockService.listLhflGlueStock(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("lhflGlueStock", new LhflGlueStock());
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhflGlueStock", iLhflGlueStockService.getLhflGlueStockInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增硫磺辅料终炼库存信息")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveLhflGlueStock(LhflGlueStock lhflGlueStock) {
        return iLhflGlueStockService.saveLhflGlueStock(lhflGlueStock);
    }

    @ApiOperation("删除硫磺辅料终炼库存信息（id不为空）")
    @RequiresPermissions("setting:lhflGlueStock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeLhflGlueStock(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhflGlueStockService.deleteLhflGlueStock(arr);
    }

    @ApiOperation("校验硫磺辅料终炼库存信息唯一性")
    @PostMapping("/checkLhflGlueStockUnique")
    @ResponseBody
    public String checkLhflGlueStockUnique(LhflGlueStock lhflGlueStock) {
        return iLhflGlueStockService.checkLhflGlueStockUnique(lhflGlueStock);
    }

    /**
     * 导出硫磺辅料终炼库存信息
     */
    @ApiOperation("导出硫磺辅料终炼库存信息")
    @RequiresPermissions("setting:lhflGlueStock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, LhflGlueStock lhflGlueStock) throws IOException {
        String fileName = I18nUtil.getMessage("setting.lhflGlueStock.modelName");
        List<LhflGlueStock> list = iLhflGlueStockService.exportData(lhflGlueStock);
        ExcelUtil<LhflGlueStock> util = new ExcelUtil<>(LhflGlueStock.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lhflGlueStock.toString(), ZltConstant.PROCEDURE_CODE_FL_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.lhflGlueStock.modelName");
        ExcelUtil<LhflGlueStock> util = new ExcelUtil<>(LhflGlueStock.class);
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
    @RequiresPermissions("setting:lhflGlueStock:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_FL_SETTING,
                I18nUtil.getMessage("setting.lhflGlueStock.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<LhflGlueStock> util = new ExcelUtil<>(LhflGlueStock.class);
        List<LhflGlueStock> list = util.importExcel(in);

        Set<Date> stockDateSet = new HashSet<>();
        Date initDate = DateUtil.parseYYYYMMDDDate("1970-01-01");
        for(LhflGlueStock stock : list) {
            if(StringUtils.isBlank(stock.getBarCode())) {
                //条码为空的情况将胶料名称值作为条码值
                stock.setBarCode(stock.getMaterialName());
            }
            if(stock.getValidTime() == null && (stock.getStockDate() != null && !initDate.equals(stock.getStockDate()))) {
                //有效期为空，将库存日期+2天作为有效期
                stock.setValidTime(DateUtils.addDays(stock.getStockDate(), 2));
            }
            stockDateSet.add(stock.getStockDate());
        }
        if(stockDateSet.size() > 1) {
            return AjaxResult.error(I18nUtil.getMessage("import.stockDate.only"));
        }

        //导入数据
        AjaxResult ajaxResult = iLhflGlueStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
