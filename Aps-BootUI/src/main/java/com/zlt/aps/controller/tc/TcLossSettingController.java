package com.zlt.aps.controller.tc;

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
import com.zlt.aps.tc.api.domain.dto.TcLossSettingDto;
import com.zlt.aps.tc.api.service.ITcLossSettingService;
import com.zlt.aps.template.tc.TcLossSettingTemp;
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
 * 胎侧损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-13
 */
@Api(tags = "胎侧损耗率设定")
@Controller
@RequestMapping("/tc/loss")
public class TcLossSettingController extends BaseController {

    private final String prefix = "tc/loss";
    @Autowired
    private ITcLossSettingService iTcLossSettingService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tc:loss:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/loss";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tcLossSetting", new TcLossSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tcLossSetting", iTcLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询胎侧损耗率设定列表
     */
    @ApiOperation("根据条件查询胎侧损耗率设定列表")
    @RequiresPermissions("tc:loss:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcLossSettingDto entity) {
        return iTcLossSettingService.list(entity);
    }

    /**
     * 修改或新增胎侧损耗率设定
     */
    @ApiOperation("修改或新增胎侧损耗率设定")
    @RequiresPermissions("tc:loss:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TcLossSettingDto tcLossSetting) {
        AjaxResult ajaxResult = null;
        if (tcLossSetting.getId() != null) {
            ajaxResult = iTcLossSettingService.edit(tcLossSetting);
        } else {
            ajaxResult = iTcLossSettingService.add(tcLossSetting);
        }
        return ajaxResult;
    }

    /**
     * 删除胎侧损耗率设定
     */
    @ApiOperation("删除胎侧损耗率设定（id不为空）")
    @RequiresPermissions("tc:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcLossSettingService.remove(arr);
    }


    @ApiOperation("刪除全部")
    @RequiresPermissions("tc:loss:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTcLossSettingService.deleteAll();
    }


    @ApiOperation("校验胎侧损耗率设定唯一性")
    @PostMapping("/checkTcLossSettingUnique")
    @ResponseBody
    public String checkTcLossSettingUnique(TcLossSettingDto tcLossSetting) {
        return iTcLossSettingService.checkTcLossSettingUnique(tcLossSetting);
    }


    /**
     * 导出胎侧损耗率设定
     */
    @ApiOperation("导出胎侧损耗率设定")
    @RequiresPermissions("tc:loss:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TcLossSettingDto tcLossSetting) throws IOException {
        List<TcLossSettingDto> list = iTcLossSettingService.getList(tcLossSetting);
        ExcelUtil<TcLossSettingDto> util = new ExcelUtil<>(TcLossSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.tc.loss.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, tcLossSetting.toString(), ApsConstant.PROCEDURE_CODE_TC);
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
        String fileName = I18nUtil.getMessage("ui.data.column.tc.loss.modelName");
        ExcelUtil<TcLossSettingTemp> util = new ExcelUtil<>(TcLossSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @throws Exception
     */
    @RequiresPermissions("tc:loss:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TC,
                I18nUtil.getMessage("ui.data.column.tc.loss.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //解析文件
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TcLossSettingDto> util = new ExcelUtil<>(TcLossSettingDto.class);
        List<TcLossSettingDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iTcLossSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        //保存导入错误日志信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
