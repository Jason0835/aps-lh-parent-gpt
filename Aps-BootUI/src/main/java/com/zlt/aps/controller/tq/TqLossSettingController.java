package com.zlt.aps.controller.tq;

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
import com.zlt.aps.template.tq.TqLossSettingTemp;
import com.zlt.aps.tq.api.domain.dto.TqLossSettingDto;
import com.zlt.aps.tq.api.service.ITqLossSettingService;
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
 * 胎圈损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-13
 */
@Api(tags = "胎圈损耗率设定")
@Controller
@RequestMapping("/tq/loss")
public class TqLossSettingController extends BaseController {

    private final String prefix = "tq/loss";
    @Autowired
    private ITqLossSettingService iTqLossSettingService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tq:loss:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/loss";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tqLossSetting", new TqLossSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tqLossSetting", iTqLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询胎圈损耗率设定列表
     */
    @ApiOperation("根据条件查询胎圈损耗率设定列表")
    @RequiresPermissions("tq:loss:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqLossSettingDto dto) {
        return iTqLossSettingService.list(dto);
    }

    /**
     * 修改或新增胎圈损耗率设定
     */
    @ApiOperation("修改或新增胎圈损耗率设定")
    @RequiresPermissions({"tq:loss:edit", "tq:loss:add"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TqLossSettingDto dto) {
        AjaxResult ajaxResult = null;
        if (dto.getId() != null) {
            ajaxResult = iTqLossSettingService.edit(dto);
        } else {
            ajaxResult = iTqLossSettingService.add(dto);
        }
        return ajaxResult;
    }

    /**
     * 删除胎圈损耗率设定
     */
    @ApiOperation("删除胎圈损耗率设定（id不为空）")
    @RequiresPermissions("tq:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqLossSettingService.remove(arr);
    }


    @ApiOperation("刪除全部")
    @RequiresPermissions("tq:loss:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTqLossSettingService.deleteAll();
    }


    @ApiOperation("校验胎圈损耗率设定唯一性")
    @PostMapping("/checkTqLossSettingUnique")
    @ResponseBody
    public String checkTqLossSettingUnique(TqLossSettingDto dto) {
        return iTqLossSettingService.checkTqLossSettingUnique(dto);
    }

    /**
     * 导出胎圈损耗率设定
     */
    @ApiOperation("导出胎圈损耗率设定")
    @RequiresPermissions("tq:loss:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TqLossSettingDto dto) throws IOException {
        List<TqLossSettingDto> list = iTqLossSettingService.getList(dto);
        ExcelUtil<TqLossSettingDto> util = new ExcelUtil<>(TqLossSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.tq.loss.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TQ);
        iExportLogService.add(exportLog);
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tq.loss.modelName");
        ExcelUtil<TqLossSettingTemp> util = new ExcelUtil<>(TqLossSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("tq:loss:import")
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TQ,
                I18nUtil.getMessage("ui.data.column.tq.loss.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TqLossSettingDto> util = new ExcelUtil<>(TqLossSettingDto.class);
        List<TqLossSettingDto> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = iTqLossSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
