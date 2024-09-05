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
import com.zlt.aps.cd90.api.domain.dto.Cd90LossSettingDto;
import com.zlt.aps.cd90.api.service.ICd90LossSettingService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd90.Cd90LossSettingTemp;
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
 * 90度裁断损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@Api(tags = "90度裁断损耗率设定")
@Controller
@RequestMapping("/cd90/loss")
public class Cd90LossSettingController extends BaseController {

    private final String prefix = "cd90/loss";

    @Autowired
    private ICd90LossSettingService iCd90LossSettingService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cd90:loss:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/loss";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cd90LossSetting", new Cd90LossSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cd90LossSetting", iCd90LossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询90度裁断损耗率设定列表
     */
    @ApiOperation("根据条件查询90度裁断损耗率设定列表")
    @RequiresPermissions("cd90:loss:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90LossSettingDto dto) {
        return iCd90LossSettingService.list(dto);
    }

    /**
     * 修改或新增90度裁断损耗率设定
     */
    @ApiOperation("修改或新增90度裁断损耗率设定")
    @RequiresPermissions("cd90:loss:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(Cd90LossSettingDto dto) {
        AjaxResult ajaxResult = null;
        if (dto.getId() != null) {
            ajaxResult = iCd90LossSettingService.edit(dto);
        } else {
            ajaxResult = iCd90LossSettingService.add(dto);
        }
        return ajaxResult;
    }

    /**
     * 删除90度裁断损耗率设定
     */
    @ApiOperation("删除90度裁断损耗率设定（id不为空）")
    @RequiresPermissions("cd90:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCd90LossSettingService.remove(arr);
    }

    @ApiOperation("刪除全部")
    @RequiresPermissions("cd90:loss:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iCd90LossSettingService.deleteAll();
    }

    @ApiOperation("校验90度裁断损耗率设定唯一性")
    @PostMapping("/checkCd90LossSettingUnique")
    @ResponseBody
    public String checkCd90LossSettingUnique(Cd90LossSettingDto dto) {
        return iCd90LossSettingService.checkCd90LossSettingUnique(dto);
    }

    /**
     * 导出90度裁断损耗率设定
     */
    @ApiOperation("导出90度裁断损耗率设定")
    @RequiresPermissions("cd90:loss:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd90LossSettingDto dto) throws IOException {
        List<Cd90LossSettingDto> list = iCd90LossSettingService.getList(dto);
        ExcelUtil<Cd90LossSettingDto> util = new ExcelUtil<>(Cd90LossSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.cd90.loss.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CD90);
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
        String fileName = I18nUtil.getMessage("ui.data.column.cd90.loss.modelName");
        ExcelUtil<Cd90LossSettingTemp> util = new ExcelUtil<>(Cd90LossSettingTemp.class);
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
    @RequiresPermissions("cd90:loss:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD90, I18nUtil.getMessage("ui.data.column.cd90.loss.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<Cd90LossSettingDto> util = new ExcelUtil<>(Cd90LossSettingDto.class);
        List<Cd90LossSettingDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iCd90LossSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }


}
