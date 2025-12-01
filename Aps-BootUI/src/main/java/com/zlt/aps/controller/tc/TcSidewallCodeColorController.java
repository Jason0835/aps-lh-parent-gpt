package com.zlt.aps.controller.tc;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.tc.api.domain.entity.TcSidewallCodeColor;
import com.zlt.aps.tc.api.service.ITcSidewallCodeColorService;
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
 * 胎侧代码前缀颜色设定Controller
 * @author zlt
 * @date 2022-01-14
 */
@Api(tags = "胎侧代码前缀颜色设定")
@Controller
@RequestMapping("/tc/sidewallCodeColor")
public class TcSidewallCodeColorController extends BaseController {

    @Autowired
    private ITcSidewallCodeColorService iTcSidewallCodeColorService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "tc/sidewallCodeColor";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tc:sidewallCodeColor:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/sidewallCodeColor";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tcSidewallCodeColor", new TcSidewallCodeColor());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tcSidewallCodeColor", iTcSidewallCodeColorService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询胎侧代码前缀颜色设定列表
     */
    @ApiOperation("根据条件查询胎侧代码前缀颜色设定列表")
    @RequiresPermissions("tc:sidewallCodeColor:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcSidewallCodeColor entity) {
        return iTcSidewallCodeColorService.list(entity);
    }

    /**
     * 修改或新增胎侧代码前缀颜色设定
     */
    @ApiOperation("修改或新增胎侧代码前缀颜色设定")
    @RequiresPermissions("tc:sidewallCodeColor:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TcSidewallCodeColor tcSidewallCodeColor) {
        AjaxResult ajaxResult = null;
        if(UserConstants.NOT_UNIQUE.equals(iTcSidewallCodeColorService.checkTcSidewallCodeColorUnique(tcSidewallCodeColor))){
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.sidewallCodeColor.checkUnique"));
        }
        if (StringUtils.isBlank(tcSidewallCodeColor.getColorCode())) {
            tcSidewallCodeColor.setColorCode("#000000");
        }
        //设置正则表达式(匹配类型:0-全匹配，1-前缀，2-后缀，3-中间，4-自定义)
        if("0".equals(tcSidewallCodeColor.getMatchType())){
            tcSidewallCodeColor.setRegularExpression("^"+tcSidewallCodeColor.getSidewallCode()+"$");
        }else if("1".equals(tcSidewallCodeColor.getMatchType())){
            tcSidewallCodeColor.setRegularExpression("^"+tcSidewallCodeColor.getSidewallCode()+".*$");
        }else if("2".equals(tcSidewallCodeColor.getMatchType())){
            tcSidewallCodeColor.setRegularExpression("^.*"+tcSidewallCodeColor.getSidewallCode()+"$");
        }else if("3".equals(tcSidewallCodeColor.getMatchType())){
            tcSidewallCodeColor.setRegularExpression("^(?!"+tcSidewallCodeColor.getSidewallCode()+").*"+tcSidewallCodeColor.getSidewallCode()+".*(?<!"+tcSidewallCodeColor.getSidewallCode()+")$");
        }else if("4".equals(tcSidewallCodeColor.getMatchType())){
            tcSidewallCodeColor.setRegularExpression(tcSidewallCodeColor.getSidewallCode());
        }

        if (tcSidewallCodeColor.getId() != null){
            ajaxResult = iTcSidewallCodeColorService.edit(tcSidewallCodeColor);
        } else{
            ajaxResult = iTcSidewallCodeColorService.add(tcSidewallCodeColor);
        }
        return ajaxResult;
    }

    /**
     * 删除胎侧代码前缀颜色设定
     */
    @ApiOperation("删除胎侧代码前缀颜色设定（id不为空）")
    @RequiresPermissions("tc:sidewallCodeColor:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcSidewallCodeColorService.remove(arr);
    }

    /**
     * 校验胎侧代码前缀颜色设定唯一性
     */
    @ApiOperation("校验胎侧代码前缀颜色设定唯一性")
    @PostMapping("/checkTcSidewallCodeColorUnique")
    @ResponseBody
    public String checkTcSidewallCodeColorUnique(TcSidewallCodeColor tcSidewallCodeColor) {
        return iTcSidewallCodeColorService.checkTcSidewallCodeColorUnique(tcSidewallCodeColor);
    }

    /**
     * 导出胎侧代码前缀颜色设定
     */
    @ApiOperation("导出胎侧代码前缀颜色设定")
    @RequiresPermissions("tc:sidewallCodeColor:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,TcSidewallCodeColor tcSidewallCodeColor) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.sidewallCodeColor.modelName");
        List<TcSidewallCodeColor> list = iTcSidewallCodeColorService.getList(tcSidewallCodeColor);
        ExcelUtil<TcSidewallCodeColor> util = new ExcelUtil<>(TcSidewallCodeColor. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, tcSidewallCodeColor.toString(),ApsConstant.PROCEDURE_CODE_TC);
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
        String fileName = I18nUtil.getMessage("ui.data.column.sidewallCodeColor.modelName");
        ExcelUtil<TcSidewallCodeColor> util = new ExcelUtil<>(TcSidewallCodeColor.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file 要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("tc:sidewallCodeColor:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TC,
                I18nUtil.getMessage("ui.data.column.sidewallCodeColor.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<TcSidewallCodeColor> util = new ExcelUtil<>(TcSidewallCodeColor.class);
        InputStream in = new ByteArrayInputStream(data);
        List<TcSidewallCodeColor> list = util.importExcel(in);
        AjaxResult ajaxResult = iTcSidewallCodeColorService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
