package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.utils.StringUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Value;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.zlt.aps.common.utils.ImportUtil;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.io.*;
import java.util.Map;
import java.util.stream.Collectors;

import com.ruoyi.common4ui.utils.file.FileUtils;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import com.zlt.aps.cx.api.domain.entity.ConstructionExportLog;
import com.zlt.aps.cx.api.service.IConstructionExportLogService;

/**
 * 施工信息导出日志Controller
 * @author zlt
 * @date 2021-12-28
 */
@Api(tags = "施工信息导出日志")
@Controller
@RequestMapping("/cx/constructionExportLog")
public class ConstructionExportLogController extends BaseController {

    @Autowired
    private IConstructionExportLogService iConstructionExportLogService;
    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    private final String prefix = "cx/constructionExportLog";

    /**
     * 跳转至主页面
     */
    @GetMapping()
    public String toIndex() {
        return prefix + "/constructionExportLog";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        return prefix + "/importData";
    }


    /**
     * 根据条件查询施工信息导出日志列表
     */
    @ApiOperation("根据条件查询施工信息导出日志列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ConstructionExportLog entity) {
        return iConstructionExportLogService.list(entity);
    }



    /**
     * 删除施工信息导出日志
     */
    @ApiOperation("删除施工信息导出日志（id不为空）")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iConstructionExportLogService.remove(arr);
    }


    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @PostMapping("/importMaterial")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, String fileType) throws Exception {

        //解析
        byte[] data = file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<String> util = new ExcelUtil<>(String.class);
        List<String> list = util.importExcel4Column(in,0,0);

        //生成
        byte[] datas=iConstructionExportLogService.getExcelData(list,fileType);
        String  filePath = ExportUtil.uploadExcelByByte(datas);

        //保存记录
        if(StringUtils.isNotBlank(filePath)){
            ConstructionExportLog constructionExportLog=new ConstructionExportLog();
            constructionExportLog.setBaseVale(null);
            List<SysDictData> dicts = iSysDictDataCacheService.getType("PROCEDURE_CODE");
            Map<String, String> PROCEDURE_CODE_Map = dicts.stream().collect(Collectors.toMap(a->a.getDictValue(),a->a.getDictLabel()));
            String wuliao=I18nUtil.getMessage("ui.data.column.productConstruction.wuliao");
            constructionExportLog.setFileName(PROCEDURE_CODE_Map.get(fileType)+wuliao+DateUtils.dateTimeNow() + ".xlsx");
            constructionExportLog.setFilePath(filePath);
            constructionExportLog.setFileType(fileType);
            iConstructionExportLogService.add(constructionExportLog);
            return AjaxResult.success();
        }else {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
    }
}
