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
import com.zlt.aps.tc.api.domain.dto.TcMouthPlateDto;
import com.zlt.aps.tc.api.service.ITcMouthPlateService;
import com.zlt.aps.template.tc.TcMouthPlateTemp;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
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
 * <p>
 * 胎侧口型板信息维护 前端控制器
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-01
 */
@Controller
@RequestMapping("/tc/mouthPlate")
@Api(tags = {"胎侧口型板信息接口"})
public class TcMouthPlateController extends BaseController {

    private final String prefix = "tc/mouthPlate";

    @Autowired
    private ITcMouthPlateService iTcMouthPlateService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @RequiresPermissions("tc:mouthPlate:view")
    @GetMapping()
    @ApiOperation("跳转到口型板信息首页")
    public String toIndex() {
        return prefix + "/mouthPlate";
    }

    /**
     * 查询胎侧口型板信息维护列表
     */
    @RequiresPermissions("tc:mouthPlate:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询胎侧口型板信息维护列表")
    public TableDataInfo list(TcMouthPlateDto dto) {
        return iTcMouthPlateService.list(dto);
    }

    /**
     * 根据id获取胎侧口型板信息维护详细信息
     */
    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取胎侧口型板信息详细信息,跳转到编辑页面")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("MouthPlate", iTcMouthPlateService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("跳转到胎侧口型板新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("MouthPlate", new TcMouthPlateDto());
        return prefix + "/edit";
    }

    /**
     * 保存胎侧口型板信息维护
     */
    @RequiresPermissions("tc:mouthPlate:edit")
    @PostMapping("/edit")
    @ResponseBody
    @ApiOperation("保存胎侧口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(TcMouthPlateDto dto) {
        return iTcMouthPlateService.edit(dto);
    }

    /**
     * 删除胎侧口型板信息维护
     */
    @RequiresPermissions("tc:mouthPlate:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除胎侧口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcMouthPlateService.remove(arr);
    }



    @ApiOperation("刪除全部")
    @RequiresPermissions("tc:mouthPlate:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTcMouthPlateService.deleteAll();
    }


    /**
     * 导出胎侧口型板信息
     */
    @RequiresPermissions("tc:mouthPlate:export")
    @ApiOperation("导出胎侧口型板信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TcMouthPlateDto dto) throws IOException {
        List<TcMouthPlateDto> list = iTcMouthPlateService.exportData(dto);
        ExcelUtil<TcMouthPlateDto> util = new ExcelUtil<>(TcMouthPlateDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.tc.mouthPlate.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TC);
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
        String fileName = I18nUtil.getMessage("ui.data.column.tc.mouthPlate.modelName");
        ExcelUtil<TcMouthPlateTemp> util = new ExcelUtil<>(TcMouthPlateTemp.class);
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
    @RequiresPermissions("tc:mouthPlate:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TC,
                I18nUtil.getMessage("ui.data.column.tc.mouthPlate.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //解析文件
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TcMouthPlateDto> util = new ExcelUtil<>(TcMouthPlateDto.class);
        List<TcMouthPlateDto> list = util.importExcel(in);
//        Map<String, String> status = iSysDictDataCacheService.getType("STATUS").stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue));
//        list.forEach(a -> a.setStatus(status.get(a.getStatus())));
        AjaxResult ajaxResult = iTcMouthPlateService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
