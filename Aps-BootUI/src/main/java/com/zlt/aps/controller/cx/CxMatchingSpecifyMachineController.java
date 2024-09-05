package com.zlt.aps.controller.cx;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.domain.CxSelect;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachine;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachineList;
import com.zlt.aps.cx.api.service.ICxMatchingSpecifyMachineService;
import com.zlt.aps.template.cx.CxMatchingSpecifyMachineListTemp;
import com.zlt.aps.template.cx.CxMatchingSpecifyMachineTemp;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.*;

/**
 * 定点机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "定点机台信息维护接口")
@Controller
@RequestMapping("/cx/specifyMachine")
public class CxMatchingSpecifyMachineController extends BaseController {
    @Autowired
    private ICxMatchingSpecifyMachineService machineInfoService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    private String prefix = "cx/specifyMachine";

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    /**
     * 列表跳转至machine页面
     *
     * @return
     */
    @RequiresPermissions("cx:specifyMachine:view")
    @GetMapping()
    public String operlog() {

        return prefix + "/specifyMachine";
    }

    /**
     * 新增跳转至add页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        CxMatchingSpecifyMachine machineInfo = new CxMatchingSpecifyMachine();
        mmap.put("tSpecifyMachine", machineInfo);
        return prefix + "/edit";
    }

    /**
     * 查询定点机台信息列表
     */
    @ApiOperation("根据条件查询定点机台信息")
    @RequiresPermissions("cx:specifyMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxMatchingSpecifyMachine machineInfo) {
        return machineInfoService.list(machineInfo);
    }

    /**
     * 跳转至修改定点机台页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        CxMatchingSpecifyMachine machineInfo = machineInfoService.getInfo(id);
        if (machineInfo == null) {
            machineInfo = new CxMatchingSpecifyMachine();
        }
        mmap.put("tSpecifyMachine", machineInfo);

        return prefix + "/edit";
    }


    @ApiOperation("修改定点机台信息（id不为空）")
    @RequiresPermissions("cx:specifyMachine:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxMatchingSpecifyMachine machineInfo) {
        AjaxResult ajaxResult = null;
        //id为空则是新增操作，否则是编辑
        if (machineInfo.getId() != null) {
            ajaxResult = machineInfoService.edit(machineInfo);
        } else {
            ajaxResult = machineInfoService.add(machineInfo);
        }
        return ajaxResult;
    }

    /**
     * 删除定点机台信息
     */
    @ApiOperation("删除定点机台信息（id不为空）")
    @RequiresPermissions("cx:specifyMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return machineInfoService.remove(arr);
    }


    @ApiOperation("导出定点机台信息")
    @RequiresPermissions("cx:specifyMachine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxMatchingSpecifyMachine machineInfo) throws IOException {
        List<CxMatchingSpecifyMachine> list = machineInfoService.exportList(machineInfo);
        ExcelUtil<CxMatchingSpecifyMachine> util = new ExcelUtil(CxMatchingSpecifyMachine.class);
        String fileName = I18nUtil.getMessage("ui.cx.specifyMachine.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, machineInfo.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }


    /**
     * 下载模板
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cx.specifyMachine.export.fileName");
        ExcelUtil<CxMatchingSpecifyMachineTemp> util = new ExcelUtil<>(CxMatchingSpecifyMachineTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("cx:specifyMachine:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.cx.specifyMachine.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxMatchingSpecifyMachine> util = new ExcelUtil<>(CxMatchingSpecifyMachine.class);
        List<CxMatchingSpecifyMachine> list = util.importExcel(in);
        AjaxResult ajaxResult = machineInfoService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }


    /**
     * 根据ID查询定点机台信息列表
     */
    @ApiOperation("根据id查询定点机台配置列表")
    @RequiresPermissions("cx:specifyMachine:getDetailById")
    @PostMapping("/getDetailById")
    @ResponseBody
    public TableDataInfo getDetailById(CxMatchingSpecifyMachine machineInfo) {
        return machineInfoService.getDetailById(machineInfo);
    }

    //============================================================================

    /**
     * 配置列表页面
     */
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("specifyMachineId", id);
        List<CxSelect> cxList = getSelect();
        mmap.put("data", JSON.toJSON(cxList));
        return prefix + "/detail/specifyMachinelist";
    }

    /**
     * 配置列表列表
     */
    @ApiOperation("获取配置详情列表")
    @PostMapping("/detail/list")
    @ResponseBody
    public TableDataInfo detailList(CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) {
        return machineInfoService.detailList(cxMatchingSpecifyMachineList);
    }

    /**
     * 配置列表列表编辑页面
     */
    @GetMapping("/detail/edit/{id}")
    public String detailEdit(@PathVariable("id") Long id, ModelMap mmap) {
        if (id == null) {
            CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList = new CxMatchingSpecifyMachineList();
            mmap.put("cxSpecifyMachineList", cxMatchingSpecifyMachineList);
            return prefix + "/detail/edit";
        }
        CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList = machineInfoService.getDetailInfo(id);
        if (cxMatchingSpecifyMachineList == null) {
            cxMatchingSpecifyMachineList = new CxMatchingSpecifyMachineList();
        }
        mmap.put("pid", cxMatchingSpecifyMachineList.getSpecifyMachineId());
        mmap.put("tSpecifyMachineList", cxMatchingSpecifyMachineList);

        List<CxSelect> cxList = getSelect();
        mmap.put("data", JSON.toJSON(cxList));
        return prefix + "/detail/edit";
    }

    /**
     * 配置列表列表新增页面
     */
    @GetMapping("/detail/add/{id}")
    public String detailAdd(@PathVariable("id") Long pid, ModelMap mmap) {
        CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList = new CxMatchingSpecifyMachineList();
        mmap.put("tSpecifyMachineList", cxMatchingSpecifyMachineList);
        mmap.put("pid", pid);
        List<CxSelect> cxList = getSelect();
        mmap.put("data", JSON.toJSON(cxList));

        return prefix + "/detail/edit";
    }

    /**
     * 配置列表列表编辑
     */
    @ApiOperation("编辑配置详情")
    @PostMapping("/detail/detailEditSave")
    @ResponseBody
    public AjaxResult detailEditSave(CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) {
        AjaxResult ajaxResult = null;
        //设置MachineCode
        CxMatchingSpecifyMachineList cx = new CxMatchingSpecifyMachineList();
        cx.setProcedureCode(cxMatchingSpecifyMachineList.getProcedureCode());
        cx.setMachineId(cxMatchingSpecifyMachineList.getMachineId());
        List<CxMatchingSpecifyMachineList> list = machineInfoService.viewList(cx);
        if (CollectionUtils.isNotEmpty(list)) {
            CxMatchingSpecifyMachineList cxSpecifyMachinet = list.get(0);
            cxMatchingSpecifyMachineList.setMachineCode(cxSpecifyMachinet.getMachineCode());
        }
        //id为空则是新增操作，否则是编辑
        if (cxMatchingSpecifyMachineList.getId() != null) {
            ajaxResult = machineInfoService.detailEdit(cxMatchingSpecifyMachineList);
        } else {
            ajaxResult = machineInfoService.detailAdd(cxMatchingSpecifyMachineList);
        }
        return ajaxResult;
    }

    /**
     * 配置列表列表删除
     */
    @ApiOperation("删除配置详情")
    @PostMapping("/detail/remove")
    @ResponseBody
    public AjaxResult detailRemove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return machineInfoService.detailRemove(arr);
    }

    /**
     * 配置列表列表导出
     */
    @RequiresPermissions("cx:specifyMachinelist:export")
    @GetMapping("/detail/export")
    @ResponseBody
    public void detailExport(HttpServletResponse response, CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) throws IOException {
        List<CxMatchingSpecifyMachineList> list = machineInfoService.detailExport(cxMatchingSpecifyMachineList);
        CxMatchingSpecifyMachine machineInfo = machineInfoService.getInfo(cxMatchingSpecifyMachineList.getSpecifyMachineId());
        if (machineInfo != null) {
            for (CxMatchingSpecifyMachineList cx : list) {
                cx.setEmbryoCode(machineInfo.getEmbryoCode());
                cx.setSap(machineInfo.getSap());
                cx.setSpecDesc(machineInfo.getSpecDesc());
            }
        }
        ExcelUtil<CxMatchingSpecifyMachineList> util = new ExcelUtil(CxMatchingSpecifyMachineList.class);
        String fileName = I18nUtil.getMessage("ui.cx.specifyMachine.detailExport.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxMatchingSpecifyMachineList.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }

    /**
     * 获取下拉选项
     */
    public List<CxSelect> getSelect() {
        List<SysDictData> dicts = iSysDictDataCacheService.getType("PROCEDURE_CODE");
        List<CxSelect> returnList = new ArrayList<CxSelect>();
        for (SysDictData sysDictData : dicts) {
            CxSelect cx1 = new CxSelect();
            cx1.setN(sysDictData.getDictLabel());
            cx1.setV(sysDictData.getDictValue());
            CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList = new CxMatchingSpecifyMachineList();
            cxMatchingSpecifyMachineList.setProcedureCode(sysDictData.getDictValue());
            List<CxMatchingSpecifyMachineList> list = machineInfoService.viewList(cxMatchingSpecifyMachineList);
            List<CxSelect> tmList = new ArrayList<CxSelect>();
            for (CxMatchingSpecifyMachineList cx : list) {
                CxSelect cx2 = new CxSelect();
                cx2.setN(cx.getMachineName());
                cx2.setV(cx.getMachineId());
                tmList.add(cx2);
            }
            cx1.setS(tmList);
            returnList.add(cx1);
        }
        return returnList;
    }


    /**
     * 下载模板
     */
    @GetMapping("/detail/importTemplate")
    @ResponseBody
    public AjaxResult detailImportTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cx.specifyMachine.detailExport.fileName");
        ExcelUtil<CxMatchingSpecifyMachineListTemp> util = new ExcelUtil<>(CxMatchingSpecifyMachineListTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("cx:specifyMachinelist:import")
    @PostMapping("/detail/importData")
    @ResponseBody
    public AjaxResult detailImportData(MultipartFile file, boolean updateSupport, Long specifyMachineId) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.cx.specifyMachine.detailExport.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxMatchingSpecifyMachineList> util = new ExcelUtil<>(CxMatchingSpecifyMachineList.class);
        Map<String, Map<String, String>> procedureMachineMap = getprocedureMachineMap();
        List<CxMatchingSpecifyMachineList> list = util.importExcel(in);
        list.forEach(a -> {
            a.setSpecifyMachineId(specifyMachineId);
            Map<String, String> map = procedureMachineMap.get(a.getProcedureCode());
            if (map != null) {
               String machineIdAndCode =map.get(a.getMachineName());
                a.setMachineId(machineIdAndCode.split("&")[0]);
                a.setMachineCode(machineIdAndCode.split("&")[1]);
            }
        });
        AjaxResult ajaxResult = machineInfoService.detailImportData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 组装工序与机台map
     */
    public Map<String, Map<String, String>> getprocedureMachineMap() {
        Map<String, Map<String, String>> procedureMachineMap = new HashMap<>();
        List<SysDictData> dicts = iSysDictDataCacheService.getType("PROCEDURE_CODE");
        for (SysDictData sysDictData : dicts) {
            CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList = new CxMatchingSpecifyMachineList();
            cxMatchingSpecifyMachineList.setProcedureCode(sysDictData.getDictValue());
            List<CxMatchingSpecifyMachineList> list = machineInfoService.viewList(cxMatchingSpecifyMachineList);

            //根据机台名称去重
            TreeSet<CxMatchingSpecifyMachineList> treeSet = new TreeSet<CxMatchingSpecifyMachineList>(new Comparator<CxMatchingSpecifyMachineList>() {
                @Override
                public int compare(CxMatchingSpecifyMachineList o1, CxMatchingSpecifyMachineList o2) {
                    return o1.getMachineName().compareTo(o2.getMachineName());
                }
            });
            treeSet.addAll(list);
            list =new ArrayList<>(treeSet);

            Map<String, String> map = new HashMap<>();
            for (CxMatchingSpecifyMachineList cx : list) {
                map.put(cx.getMachineName(), cx.getMachineId()+"&"+cx.getMachineCode());
            }
            procedureMachineMap.put(sysDictData.getDictValue(), map);
        }
        return procedureMachineMap;
    }

}
