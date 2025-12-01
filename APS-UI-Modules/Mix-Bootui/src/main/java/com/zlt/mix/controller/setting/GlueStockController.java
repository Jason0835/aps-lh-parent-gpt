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
import com.zlt.mix.setting.api.domain.dto.GlueStockDto;
import com.zlt.mix.setting.api.domain.entity.GlueStock;
import com.zlt.mix.setting.api.service.IGlueStockService;
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
 * 终炼胶库存信息Controller
 *
 * @author Gim
 * @date 2022-03-18
 */
@Api(tags = "库存信息")
@Controller
@RequestMapping("/setting/stock")
public class GlueStockController extends BaseController {

    @Resource
    private IGlueStockService iGlueStockService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/stock";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:stock:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/stock";
    }

    @ApiOperation("根据条件查询库存信息列表")
    @RequiresPermissions("setting:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listGlueStock(GlueStock entity) {
        return iGlueStockService.listGlueStock(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("tGlueStockDto", new GlueStockDto());
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tGlueStockDto", iGlueStockService.getGlueStockInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增库存信息")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueStock(GlueStockDto glueStockDto) {
        return iGlueStockService.saveGlueStock(glueStockDto);
    }

    @ApiOperation("删除库存信息（id不为空）")
    @RequiresPermissions("setting:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeGlueStock(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGlueStockService.deleteGlueStock(arr);
    }

    @ApiOperation("校验库存信息唯一性")
    @PostMapping("/checkGlueStockUnique")
    @ResponseBody
    public String checkGlueStockUnique(GlueStock glueStock) {
        return iGlueStockService.checkGlueStockUnique(glueStock);
    }

    /**
     * 导出库存信息
     */
    @ApiOperation("导出库存信息")
    @RequiresPermissions("setting:stock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GlueStock glueStock) throws IOException {
        String fileName = I18nUtil.getMessage("setting.stock.modelName");
        List<GlueStockDto> list = iGlueStockService.exportData(glueStock);
        ExcelUtil<GlueStockDto> util = new ExcelUtil<>(GlueStockDto.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, glueStock.toString(), ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.stock.modelName");
        ExcelUtil<GlueStock> util = new ExcelUtil<>(GlueStock.class);
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
    @RequiresPermissions("setting:stock:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.stock.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<GlueStock> util = new ExcelUtil<>(GlueStock.class);
        List<GlueStock> list = util.importExcel(in);

        Set<Date> stockDateSet = new HashSet<>();
        Date initDate = DateUtil.parseYYYYMMDDDate("1970-01-01");
        for(GlueStock stock : list) {
            if(StringUtils.isBlank(stock.getBarCode())) {
                //条码为空的情况将胶料名称值作为条码值
                stock.setBarCode(stock.getGlue());
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
        AjaxResult ajaxResult = iGlueStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
