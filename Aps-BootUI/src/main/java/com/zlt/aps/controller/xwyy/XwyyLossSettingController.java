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
import com.zlt.aps.template.xwyy.XwyyLossSettingTemp;
import com.zlt.aps.xwyy.api.domain.dto.XwyyLossSettingDto;
import com.zlt.aps.xwyy.api.service.IXwyyLossSettingService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 纤维压延损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@Api(tags = "纤维压延损耗率设定")
@Controller
@RequestMapping("/xwyy/loss")
public class XwyyLossSettingController extends BaseController {

    private final String prefix = "xwyy/loss";

    @Autowired
    private IXwyyLossSettingService iXwyyLossSettingService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    /**
     * 跳转至主页面
     */
    @RequiresPermissions("xwyy:loss:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/loss";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("xwyyLossSetting", new XwyyLossSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("xwyyLossSetting", iXwyyLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询纤维压延损耗率设定列表
     */
    @ApiOperation("根据条件查询纤维压延损耗率设定列表")
    @RequiresPermissions("xwyy:loss:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyLossSettingDto dto) {
        return iXwyyLossSettingService.list(dto);
    }

    /**
     * 修改或新增纤维压延损耗率设定
     */
    @ApiOperation("修改或新增纤维压延损耗率设定")
    @RequiresPermissions({"xwyy:loss:edit", "xwyy:loss:add"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(XwyyLossSettingDto dto) {
        AjaxResult ajaxResult = null;
        if (dto.getId() != null) {
            ajaxResult = iXwyyLossSettingService.edit(dto);
        } else {
            ajaxResult = iXwyyLossSettingService.add(dto);
        }
        return ajaxResult;
    }

    /**
     * 删除纤维压延损耗率设定
     */
    @ApiOperation("删除纤维压延损耗率设定（id不为空）")
    @RequiresPermissions("xwyy:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iXwyyLossSettingService.remove(arr);
    }


    @ApiOperation("刪除全部")
    @RequiresPermissions("xwyy:loss:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iXwyyLossSettingService.deleteAll();
    }


    @ApiOperation("校验纤维压延损耗率设定唯一性")
    @PostMapping("/checkXwyyLossSettingUnique")
    @ResponseBody
    public String checkXwyyLossSettingUnique(XwyyLossSettingDto dto) {
        return iXwyyLossSettingService.checkXwyyLossSettingUnique(dto);
    }


    /**
     * 导出纤维压延损耗率设定
     */
    @ApiOperation("导出纤维压延损耗率设定")
    @RequiresPermissions("xwyy:loss:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, XwyyLossSettingDto dto) throws IOException {
        List<XwyyLossSettingDto> list = iXwyyLossSettingService.getList(dto);
        ExcelUtil<XwyyLossSettingDto> util = new ExcelUtil<>(XwyyLossSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.xwyy.loss.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_XWYY);
        iExportLogService.add(exportLog);
    }


    /**
     * 下载模板
     */
    @ApiOperation("下载模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.xwyy.loss.modelName");
        ExcelUtil<XwyyLossSettingTemp> util = new ExcelUtil<>(XwyyLossSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("xwyy:loss:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_XWYY, I18nUtil.getMessage("ui.data.column.xwyy.loss.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<XwyyLossSettingDto> util = new ExcelUtil<>(XwyyLossSettingDto.class);
        List<XwyyLossSettingDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iXwyyLossSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
