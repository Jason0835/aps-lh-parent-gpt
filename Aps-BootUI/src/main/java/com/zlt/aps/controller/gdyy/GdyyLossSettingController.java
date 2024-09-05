package com.zlt.aps.controller.gdyy;

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
import com.zlt.aps.gdyy.api.domain.dto.GdyyLossSettingDto;
import com.zlt.aps.gdyy.api.service.IGdyyLossSettingService;
import com.zlt.aps.template.gdyy.GdyyLossSettingTemp;
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
 * 钢带压延损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@Api(tags = "钢带压延损耗率设定")
@Controller
@RequestMapping("/gdyy/loss")
public class GdyyLossSettingController extends BaseController {

    private final String prefix = "gdyy/loss";
    @Autowired
    private IGdyyLossSettingService iGdyyLossSettingService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("gdyy:loss:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/loss";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("gdyyLossSetting", new GdyyLossSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("gdyyLossSetting", iGdyyLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询钢带压延损耗率设定列表
     */
    @ApiOperation("根据条件查询钢带压延损耗率设定列表")
    @RequiresPermissions("gdyy:loss:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GdyyLossSettingDto dto) {
        return iGdyyLossSettingService.list(dto);
    }

    /**
     * 修改或新增钢带压延损耗率设定
     */
    @ApiOperation("修改或新增钢带压延损耗率设定")
    @RequiresPermissions({"gdyy:loss:edit", "gdyy:loss:add"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(GdyyLossSettingDto dto) {
        AjaxResult ajaxResult = null;
        if (dto.getId() != null) {
            ajaxResult = iGdyyLossSettingService.edit(dto);
        } else {
            ajaxResult = iGdyyLossSettingService.add(dto);
        }
        return ajaxResult;
    }

    /**
     * 删除钢带压延损耗率设定
     */
    @ApiOperation("删除钢带压延损耗率设定（id不为空）")
    @RequiresPermissions("gdyy:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGdyyLossSettingService.remove(arr);
    }

    @ApiOperation("刪除全部")
    @RequiresPermissions("gdyy:loss:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iGdyyLossSettingService.deleteAll();
    }



    @ApiOperation("校验钢带压延损耗率设定唯一性")
    @PostMapping("/checkGdyyLossSettingUnique")
    @ResponseBody
    public String checkGdyyLossSettingUnique(GdyyLossSettingDto dto) {
        return iGdyyLossSettingService.checkGdyyLossSettingUnique(dto);
    }

    /**
     * 导出钢带压延损耗率设定
     */
    @ApiOperation("导出钢带压延损耗率设定")
    @RequiresPermissions("gdyy:loss:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GdyyLossSettingDto dto) throws IOException {
        List<GdyyLossSettingDto> list = iGdyyLossSettingService.getList(dto);
        ExcelUtil<GdyyLossSettingDto> util = new ExcelUtil<>(GdyyLossSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.gdyy.loss.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_GDYY);
        iExportLogService.add(exportLog);
    }


    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.gdyy.loss.modelName");
        ExcelUtil<GdyyLossSettingTemp> util = new ExcelUtil<>(GdyyLossSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("gdyy:loss:import")
    @ApiOperation("导入excel数据")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<GdyyLossSettingDto> util = new ExcelUtil<>(GdyyLossSettingDto.class);
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GDYY,
                I18nUtil.getMessage("ui.data.column.gdyy.loss.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        List<GdyyLossSettingDto> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = iGdyyLossSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
