package com.zlt.aps.controller.gsq;

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
import com.zlt.aps.gsq.api.domain.dto.GsqLossSettingDto;
import com.zlt.aps.gsq.api.service.IGsqLossSettingService;
import com.zlt.aps.template.gsq.GsqLossSettingTemp;
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
 * 钢丝圈损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-13
 */
@Api(tags = "钢丝圈损耗率设定")
@Controller
@RequestMapping("/gsq/loss")
public class GsqLossSettingController extends BaseController {

    private final String prefix = "gsq/loss";

    @Autowired
    private IGsqLossSettingService iGsqLossSettingService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("gsq:loss:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/loss";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("gsqLossSetting", new GsqLossSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("gsqLossSetting", iGsqLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询钢丝圈损耗率设定列表
     */
    @ApiOperation("根据条件查询钢丝圈损耗率设定列表")
    @RequiresPermissions("gsq:loss:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqLossSettingDto dto) {
        return iGsqLossSettingService.list(dto);
    }

    /**
     * 修改或新增钢丝圈损耗率设定
     */
    @ApiOperation("修改或新增钢丝圈损耗率设定")
    @RequiresPermissions("gsq:loss:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(GsqLossSettingDto gsqLossSetting) {
        AjaxResult ajaxResult = null;
        if (gsqLossSetting.getId() != null) {
            ajaxResult = iGsqLossSettingService.edit(gsqLossSetting);
        } else {
            ajaxResult = iGsqLossSettingService.add(gsqLossSetting);
        }
        return ajaxResult;
    }

    /**
     * 删除钢丝圈损耗率设定
     */
    @ApiOperation("删除钢丝圈损耗率设定（id不为空）")
    @RequiresPermissions("gsq:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGsqLossSettingService.remove(arr);
    }


    @ApiOperation("刪除全部")
    @RequiresPermissions("gsq:loss:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iGsqLossSettingService.deleteAll();
    }


    @ApiOperation("校验钢丝圈损耗率设定唯一性")
    @PostMapping("/checkGsqLossSettingUnique")
    @ResponseBody
    public String checkGsqLossSettingUnique(GsqLossSettingDto dto) {
        return iGsqLossSettingService.checkGsqLossSettingUnique(dto);
    }


    /**
     * 导出钢丝圈损耗率设定
     */
    @ApiOperation("导出钢丝圈损耗率设定")
    @RequiresPermissions("gsq:loss:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GsqLossSettingDto gsqLossSetting) throws IOException {
        List<GsqLossSettingDto> list = iGsqLossSettingService.getList(gsqLossSetting);
        ExcelUtil<GsqLossSettingDto> util = new ExcelUtil<>(GsqLossSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.gsq.loss.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, gsqLossSetting.toString(), ApsConstant.PROCEDURE_CODE_GSQ);
        iExportLogService.add(exportLog);
    }

    @ApiOperation("下载模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.gsq.loss.modelName");
        ExcelUtil<GsqLossSettingTemp> util = new ExcelUtil<>(GsqLossSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("gsq:loss:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GSQ,
                I18nUtil.getMessage("ui.data.column.gsq.loss.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<GsqLossSettingDto> util = new ExcelUtil<>(GsqLossSettingDto.class);
        List<GsqLossSettingDto> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = iGsqLossSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
