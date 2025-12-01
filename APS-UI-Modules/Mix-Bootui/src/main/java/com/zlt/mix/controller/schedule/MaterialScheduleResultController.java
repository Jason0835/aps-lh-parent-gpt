package com.zlt.mix.controller.schedule;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.ip.IpUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.utils.file.FileUtils4UI;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.core.utils.MixCommonUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.schedule.api.domain.dto.*;
import com.zlt.mix.schedule.api.domain.entity.*;
import com.zlt.mix.schedule.api.service.IMaterialScheduleResultService;
import com.zlt.mix.schedule.api.service.IMaterialSpanReceiveService;
import com.zlt.mix.service.SettingService;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ExcelUtils.*;

/**
 * 硫化辅料日计划排程Controller
 *
 * @author chen
 * @date 2022-05-24
 */
@Api(tags = "硫化辅料日计划排程")
@Controller
@RequestMapping("/schedule/materialScheduleResult")
public class MaterialScheduleResultController extends BaseController {

    @Resource
    private IMaterialScheduleResultService iMaterialScheduleResultService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;
    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;
    @Resource
    private IMaterialSpanReceiveService iMaterialSpanReceiveService;
    @Autowired
    private SettingService settingService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    private final String prefix = "schedule/materialScheduleResult";
	/**
	 * 排产日切换时间（硫磺辅料有多种班制，需求暂定只考虑16点切换的情况）
	 */
	private static String DAY_SWITCH_TIME = "16";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("schedule:materialScheduleResult:view")
    @GetMapping()
    public String toIndex(ModelMap modelMap) {
    	Date scheduleDate = this.getScheduleDate(); // 排产日
        modelMap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
        return prefix + "/materialScheduleResult";
    }

    @ApiOperation("根据条件查询硫化辅料日计划排程列表")
    @RequiresPermissions("schedule:materialScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listMaterialScheduleResult(MaterialScheduleResult entity) {
        return iMaterialScheduleResultService.listMaterialScheduleResult(entity);
    }

    /**
     * 跳转至插单页面
     */
    @ApiOperation("跳转至插单页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        MaterialScheduleResult scheduleResult = new MaterialScheduleResult();
        scheduleResult.setScheduleDate(this.getScheduleDate()); // 排产日
        mmap.put("materialScheduleResult", scheduleResult);
        mmap.put("minDate", DateUtils.getNowDateByyyMMdd());
        return prefix + "/add";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
    	MaterialScheduleResult schedule = iMaterialScheduleResultService.getMaterialScheduleResultInfo(id);
        ScheduleClassEditableDto editableStatusDto = new ScheduleClassEditableDto();
        editableStatusDto.setScheduleDate(schedule.getScheduleDate());
        mmap.put("editableStatus", iMaterialScheduleResultService.getCLassEditableStatus(editableStatusDto)); // 加载班次可编辑状态
        mmap.put("materialScheduleResult", schedule);
        return prefix + "/edit";
    }

    /**
     * 跳转至生成排程页面
     */
    @ApiOperation("跳转至生成排程页面")
    @GetMapping("/toAutoPlan")
    public String toAutoPlan(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/autoPlan";
    }

    /**
     * 跳转至转机台页面
     */
    @ApiOperation("跳转至转机台页面")
    @GetMapping("/toChangeMachine/{id}")
    public String toChangeMachine(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("materialScheduleResult", iMaterialScheduleResultService.getMaterialScheduleResultInfo(id));
        return prefix + "/changeMachine";
    }

    @ApiOperation("跳转至导入页面")
    @GetMapping("/importData")
    public String importDate(ModelMap mmp) {
        mmp.put("prefix", prefix);
        mmp.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));
        return prefix + "/importData";
    }

    @ApiOperation("更改配方信息页面")
    @GetMapping("/toChangeRecipe")
    public String toChangeRecipe(ModelMap mmap, String machineName, String materialName, String id) {
        mmap.put("machineName", machineName);
        mmap.put("materialName", materialName);
        mmap.put("id", id);
        MaterialScheduleResult scheduleResult = iMaterialScheduleResultService.getMaterialScheduleResultInfo(Long.valueOf(id));
        mmap.put("recipeType", scheduleResult.getRecipeType());
        mmap.put("recipeTypeName", scheduleResult.getRecipeTypeName());
        mmap.put("recipeVersionId", scheduleResult.getRecipeVersionId());
        mmap.put("recipeStage", scheduleResult.getRecipeStage());
        return prefix + "/changeRecipe";
    }

    @ApiOperation("插单选配方页面")
    @GetMapping("/toChooseRecipeType")
    public String toChooseRecipeType(ModelMap mmap, String machineName, String materialName) {
        mmap.put("machineName", machineName);
        mmap.put("materialName", materialName);
        return prefix + "/chooseRecipeType";
    }

    @ApiOperation("跳转至跨区发送页面")
    @GetMapping("/toSendCrossRegional")
    public String toSendCrossRegional(String mixArea, String ids, ModelMap mmap) {
        mmap.put("mixArea", mixArea);
        mmap.put("sendPerson", AuthorizationUtils.getSysUser().getUserName());
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        // 查询选中的记录回写发送页
        List<MaterialScheduleResult> selectList = new ArrayList<>();
        if (StringUtils.isNotBlank(ids)) {
            Long[] scheduleIds = Convert.toLongArray(ids);
            selectList = iMaterialScheduleResultService.selectSpanSendNeedFieldByIds(scheduleIds);
        }
        mmap.put("selectList", selectList);
        return prefix + "/sendCrossRegional";
    }

    @ApiOperation("跳转至跨区接收页面")
    @GetMapping("/toReceiveCrossRegional/{mixArea}")
    public String toReceiveCrossRegional(@PathVariable("mixArea") String mixArea, ModelMap mmap) {
        mmap.put("receivePerson", AuthorizationUtils.getSysUser().getUserName());
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        mmap.put("entrustedMixArea", mixArea);
        return prefix + "/receiveCrossRegional";
    }

    @ApiOperation("跳转至跨区接收页面")
    @GetMapping("/toChooseMachine")
    public String toChooseMachine(MaterialSpanReceive param, ModelMap mmap) {
        MaterialSpanReceive receiveInfo = iMaterialSpanReceiveService.getMaterialSpanReceiveInfo(param);
        String machineCode = param.getMachineCode();
        String machineName = param.getMachineName();
        String recipeTypeName = param.getRecipeTypeName();
        String recipeType = param.getRecipeType();
        String recipeVersionId = param.getRecipeVersionId();
        String recipeStage = param.getRecipeStage();
        if (StringUtils.isNotBlank(machineCode)) {
            receiveInfo.setMachineCode(machineCode);
        }
        if (StringUtils.isNotBlank(machineName)) {
            receiveInfo.setMachineName(machineName);
        }
        if (StringUtils.isNotBlank(recipeTypeName)) {
            receiveInfo.setRecipeTypeName(recipeTypeName);
        }
        if (StringUtils.isNotBlank(recipeType)) {
            receiveInfo.setRecipeType(recipeType);
        }
        if (StringUtils.isNotBlank(recipeVersionId)) {
            receiveInfo.setRecipeVersionId(recipeVersionId);
        }
        if (StringUtils.isNotBlank(recipeStage)) {
            receiveInfo.setRecipeStage(recipeStage);
        }
        mmap.put("materialSpanReceive", receiveInfo);
        return prefix + "/receiveChooseMachine";
    }

    @ApiOperation("修改或新增硫化辅料日计划排程")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveMaterialScheduleResult(MaterialScheduleResult materialScheduleResult) {
    	this.setUserIp(materialScheduleResult);
        return iMaterialScheduleResultService.saveMaterialScheduleResult(materialScheduleResult);
    }

    @ApiOperation("删除硫化辅料日计划排程（id不为空）")
    @RequiresPermissions("schedule:materialScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeMaterialScheduleResult(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMaterialScheduleResultService.deleteMaterialScheduleResult(arr);
    }

    @ApiOperation("校验硫化辅料日计划排程唯一性")
    @PostMapping("/checkMaterialScheduleResultUnique")
    @ResponseBody
    public String checkMaterialScheduleResultUnique(MaterialScheduleResult materialScheduleResult) {
        return iMaterialScheduleResultService.checkMaterialScheduleResultUnique(materialScheduleResult);
    }

    /**
     * 导出硫化辅料日计划排程
     */
    @ApiOperation("导出硫化辅料日计划排程")
    @RequiresPermissions("schedule:materialScheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, MaterialScheduleResult materialScheduleResult) throws IOException {
        String fileName = I18nUtil.getMessage("schedule.materialScheduleResult.modelName");
        MaterialScheduleResultExportDictDto dictDto = new MaterialScheduleResultExportDictDto();
        HashMap<String, String> mixAreaDictMap = iSysDictDataCacheService.getType("MIX_AREA").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        HashMap<String, String> recipeStageDictMap = iSysDictDataCacheService.getType("PRODUCT_STAGE").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        HashMap<String, String> releaseStatusDictMap = iSysDictDataCacheService.getType("MIX_RELEASE_STATUS").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        HashMap<String, String> isOrNotDictMap = iSysDictDataCacheService.getType("IS_HAVE").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        HashMap<String, String> classShiftDictMap = iSysDictDataCacheService.getType("LH_CLASS_SHIFT").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        dictDto.setMixAreaDictMap(mixAreaDictMap);
        dictDto.setRecipeStageDictMap(recipeStageDictMap);
        dictDto.setReleaseStatusDictMap(releaseStatusDictMap);
        dictDto.setIsOrNotDictMap(isOrNotDictMap);
        dictDto.setClassShiftDictMap(classShiftDictMap);
        BeanUtils.copyProperties(materialScheduleResult, dictDto);
        byte[] data = iMaterialScheduleResultService.exportData(dictDto);
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, dictDto.toString(), ZltConstant.PROCEDURE_CODE_MIX);
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
        String fileName = I18nUtil.getMessage("schedule.materialScheduleResult.modelName");
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "schedule/" + fileName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        ExcelUtil.setResponseHeader(response, fileName);
        FileUtils4UI.writeInputStream(in, response.getOutputStream());
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file         要导入的文件
     * @param scheduleDate 排程日期
     * @param mixArea      密炼区
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("schedule:materialScheduleResult:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date scheduleDate, String mixArea) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_MIX,
                I18nUtil.getMessage("schedule.materialScheduleResult.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<MaterialScheduleResult> util = new ExcelUtil<>(MaterialScheduleResult.class);
        List<MaterialScheduleResult> list = parseObject(in, scheduleDate, mixArea);
        //导入数据
        AjaxResult ajaxResult = iMaterialScheduleResultService.importData(list, DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate), mixArea, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 转机台
     */
    @RequiresPermissions("schedule:materialScheduleResult:changeMachine")
    @ApiOperation("转机台")
    @PostMapping("/batchChangeMachine/{machineCode}")
    @ResponseBody
    public AjaxResult batchChangeMachine(@PathVariable("machineCode") String machineCode, String ids) {
        return iMaterialScheduleResultService.batchChangeMachine(machineCode, ids);
    }

    /**
     * 转机台
     */
    @RequiresPermissions("schedule:materialScheduleResult:changeMachine")
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(MaterialScheduleResult materialScheduleResult) {
    	this.setUserIp(materialScheduleResult);
        return iMaterialScheduleResultService.changeMachine(materialScheduleResult);
    }

    /**
     * 自动排程校验
     */
    @ApiOperation("自动排程校验")
    @PostMapping("/validateAutoPlan")
    @ResponseBody
    public AjaxResult validateAutoPlan(MaterialScheduleResult materialScheduleResult) {
        if (materialScheduleResult.getScheduleDate() == null) {
            materialScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        // TODO 调用校验接口
        return AjaxResult.success("2");
    }

    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @RequiresPermissions("schedule:materialScheduleResult:autoSchedule")
    @PostMapping("/autoSchedule")
    @ResponseBody
    public AjaxResult autoSchedule(MaterialScheduleResult materialScheduleResult) {
        if (materialScheduleResult.getScheduleDate() == null) {
            materialScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        this.setUserIp(materialScheduleResult);
        return iMaterialScheduleResultService.autoSchedule(materialScheduleResult);
    }

    /**
     * 排程发布校验
     */
    @ApiOperation("排程发布前校验")
    @PostMapping("/publishValidate")
    @ResponseBody
    public AjaxResult publishValidate(MaterialScheduleResult materialScheduleResult) {
        // TODO 调用校验接口
        return AjaxResult.success();
    }

    /**
     * 排程发布
     */
    @ApiOperation("排程发布")
    @RequiresPermissions("schedule:materialScheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(MaterialScheduleResult materialScheduleResult) {
        if (materialScheduleResult.getScheduleDate() == null) {
            materialScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        this.setUserIp(materialScheduleResult);
        return iMaterialScheduleResultService.publish(materialScheduleResult);
    }

    @ApiOperation("检测对应日期和密炼区的数据是否存在")
    @PostMapping("/checkScheduleDateAndMixAreaExist")
    @ResponseBody
    public AjaxResult checkScheduleDateAndMixAreaExist(MaterialScheduleResult materialScheduleResult) {
        String unique = iMaterialScheduleResultService.checkScheduleDateAndMixAreaExist(materialScheduleResult);

        //避免ZltConstant是否唯一的常量值修改，在此处定义0为唯一，1为不唯一
        if (ZltConstant.UNIQUE.equals(unique)) {
            return AjaxResult.success("0");
        }
        if (ZltConstant.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.success("1");
        }
        return AjaxResult.error();
    }

    /**
     * 更改配方信息
     */
    @ApiOperation("更改配方信息")
    @RequiresPermissions("schedule:materialScheduleResult:edit")
    @PostMapping("/changeRecipe")
    @ResponseBody
    public AjaxResult changeRecipe(MaterialScheduleResult materialScheduleResult) {
    	this.setUserIp(materialScheduleResult);
        return iMaterialScheduleResultService.changeRecipe(materialScheduleResult);
    }
    
    /**
     * 检查班次是否可编辑
     * @param classShift
     * @return
     */
    @ApiOperation("检查班次是否可编辑")
    @PostMapping("/checkCLassEditable")
    @ResponseBody
    public AjaxResult checkCLassEditable(ScheduleClassEditableDto dto) {
    	return AjaxResult.success(iMaterialScheduleResultService.checkCLassEditable(dto));
    }
    
    /**
     * 获取各班次可编辑状态
     * @param scheduleDate 排产日期
     * @return
     */
    @ApiOperation("检查班次是否可编辑")
    @PostMapping("/getCLassEditableStatus")
    @ResponseBody
    public AjaxResult getCLassEditableStatus(ScheduleClassEditableDto dto) {
    	return AjaxResult.success(iMaterialScheduleResultService.getCLassEditableStatus(dto));
    }

    /**
     * 根据excel的io流解析成对应集合对象
     *
     * @param in io
     * @return 解析后集合对象
     * @throws Exception 异常
     */
    private List<MaterialScheduleResult> parseObject(InputStream in, Date scheduleDate, String mixArea) throws Exception {
        List<MaterialScheduleResult> list = new ArrayList<>();
        if (in == null) {
            return list;
        }
        Workbook workbook = WorkbookFactory.create(in);
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        }
        int rows = sheet.getPhysicalNumberOfRows();

        if (rows > 2) {
            List<SysDictData> productStageList = iSysDictDataCacheService.getType("PRODUCT_STAGE");
            Map<String, String> productStageMap = productStageList.stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue));
            List<SysDictData> isReleaseList = iSysDictDataCacheService.getType("MIX_RELEASE_STATUS");
            Map<String, String> isReleaseMap = isReleaseList.stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue));
            List<SysDictData> isHaveList = iSysDictDataCacheService.getType("IS_HAVE");
            Map<String, String> isHaveMap = isHaveList.stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue));
            List<SysDictData> lhClassShiftList = iSysDictDataCacheService.getType("LH_CLASS_SHIFT");
            Map<String, String> lhClassShiftMap = lhClassShiftList.stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue));

            for (int i = 2; i < rows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                MaterialScheduleResult scheduleResult = getRowData(row, productStageMap, isReleaseMap, isHaveMap, lhClassShiftMap);
                // 所有数据都为空的情况，视为该行数据为无效行，不做导入
                if (scheduleResult == null) {
                    continue;
                }
                scheduleResult.setScheduleDate(scheduleDate);
                scheduleResult.setMixArea(mixArea);
                list.add(scheduleResult);
            }
        }
        return list;
    }

    private MaterialScheduleResult getRowData(Row row, Map<String, String> productStageMap, Map<String, String> isReleaseMap, Map<String, String> isHaveMap, Map<String, String> lhClassShiftMap) throws ParseException, IllegalAccessException {
        int cellNum = 0;

        String machineName = getCellValue(row.getCell(cellNum++));    //0   小料机台名称
        String classShift = getCellValue(row.getCell(cellNum++));    //1   班制
        String releaseStatus = getCellValue(row.getCell(cellNum++));    //2   发布状态
        String materialName = getCellValue(row.getCell(cellNum++));    //3   胶料名称
        String recipeTypeName = getCellValue(row.getCell(cellNum++));    //4   配方类型名称
        String recipeVersionId = MixCommonUtil.stripTrailingZeros(getCellValue(row.getCell(cellNum++)));    //5   配方版本号
        String recipeStage = getCellValue(row.getCell(cellNum++));    //6   配方阶段
        String stockQty = getCellValue(row.getCell(cellNum++));   //7   库存
        String safeStockQty = getCellValue(row.getCell(cellNum++));    //8   安全库存
        String demandQty = getCellValue(row.getCell(cellNum++));   //9   需求量
        String demandPlanning = getCellValue(row.getCell(cellNum++));   //10   需求计划
        String totalPlanQty = getCellValue(row.getCell(cellNum++));  //11   总计划
//        String totalSurplus = getCellValue(row.getCell(cellNum++));  //12   总剩余（无用字段）
        String midProduceOrder = getCellValue(row.getCell(cellNum++));  //13   中班顺序
        String midPlanQty = getCellValue(row.getCell(cellNum++));  //14   中班计划
        String midFinishQty = getCellValue(row.getCell(cellNum++));  //15   中班完成
        row.getCell(cellNum++);  //   中班完成率
        row.getCell(cellNum++);  //   中班预计开始时间
        row.getCell(cellNum++);  //16   中班预计完成时间
        String midRemark = getCellValue(row.getCell(cellNum++));  //17   中班备注
        String nightProduceOrder = getCellValue(row.getCell(cellNum++));  //18   夜班顺序
        String nightPlanQty = getCellValue(row.getCell(cellNum++));  //19   夜班计划
        String nightFinishQty = getCellValue(row.getCell(cellNum++));  //20   夜班完成
        row.getCell(cellNum++);  //   夜班完成率
        row.getCell(cellNum++);  //   夜班预计开始时间
        row.getCell(cellNum++);  //21   夜班预计完成时间
        String nightRemark = getCellValue(row.getCell(cellNum++));  //22   夜班备注
        String dayProduceOrder = getCellValue(row.getCell(cellNum++));  //23   白班顺序
        String dayPlanQty = getCellValue(row.getCell(cellNum++));  //24   白班计划
        String dayFinishQty = getCellValue(row.getCell(cellNum++));  //25   白班完成
        row.getCell(cellNum++);  //   白班完成率
        row.getCell(cellNum++);  //   白班预计开始时间
        row.getCell(cellNum++);  //26   白班预计完成时间
        String dayRemark = getCellValue(row.getCell(cellNum));  //27   白班备注

        MaterialScheduleResult entity = new MaterialScheduleResult();
        entity.setMachineName(machineName);
        // 班制字典转换
        if (StringUtils.isNotEmpty(lhClassShiftMap) && StringUtils.isNotEmpty(lhClassShiftMap) && lhClassShiftMap.containsKey(classShift)) {
            entity.setClassShift(Integer.valueOf(lhClassShiftMap.get(classShift)));
        }
        // 是否发布字典转换
        if (StringUtils.isNotEmpty(releaseStatus) && StringUtils.isNotEmpty(isReleaseMap) && isReleaseMap.containsKey(releaseStatus)) {
            entity.setReleaseStatus(isReleaseMap.getOrDefault(releaseStatus, ""));
        }
        entity.setMaterialName(materialName);
        entity.setRecipeTypeName(recipeTypeName);
        entity.setRecipeVersionId(recipeVersionId);
        // 配方阶段字典转换
        if (StringUtils.isNotEmpty(recipeStage) && StringUtils.isNotEmpty(productStageMap) && productStageMap.containsKey(recipeStage)) {
            entity.setRecipeStage(productStageMap.get(recipeStage));
        }
        entity.setStockQty(getDoubleValue(stockQty));
        entity.setSafeStockQty(getDoubleValue(safeStockQty));
        entity.setDemandQty(getDoubleValue(demandQty));
//        entity.setDemandPlanning(demandPlanning);
        entity.setTotalPlanQty(getDoubleValue(totalPlanQty));
//        entity.setTotalSurplus(getDoubleValue(totalSurplus));//无用字段
        entity.setMidProduceOrder(getIntegerValue(midProduceOrder));
        entity.setMidPlanQty(getDoubleValue(midPlanQty));
        entity.setMidFinishQty(getDoubleValue(midFinishQty));
//        entity.setMidExpectFinishTime(getDateValue(midExpectFinishTime, "yyyy-MM-dd"));
        entity.setMidRemark(midRemark);
        entity.setNightProduceOrder(getIntegerValue(nightProduceOrder));
        entity.setNightPlanQty(getDoubleValue(nightPlanQty));
        entity.setNightFinishQty(getDoubleValue(nightFinishQty));
//        entity.setNightExpectFinishTime(getDateValue(nightExpectFinishTime, "yyyy-MM-dd"));
        entity.setNightRemark(nightRemark);
        entity.setDayProduceOrder(getIntegerValue(dayProduceOrder));
        entity.setDayPlanQty(getDoubleValue(dayPlanQty));
        entity.setDayFinishQty(getDoubleValue(dayFinishQty));
//        entity.setDayExpectFinishTime(getDateValue(dayExpectFinishTime, "yyyy-MM-dd"));
        entity.setDayRemark(dayRemark);
        return checkObjFieldIsNull(entity) ? null : entity;
    }

    @ApiOperation("统计字段页面")
    @GetMapping("/toStatistics")
    public String toStatistics(ModelMap mmap, Date scheduleDate,String mixArea, String machineCode) {
        MaterialScheduleResult materialScheduleResult = new MaterialScheduleResult();
        materialScheduleResult.setScheduleDate(scheduleDate);
        materialScheduleResult.setMixArea(mixArea);
        materialScheduleResult.setMachineCode(machineCode);
        mmap.put("materialScheduleResult",materialScheduleResult);
        //获取对应密炼区的机台信息
        List<LhflMachine> machineInfo = settingService.getLhflMachineInfo(mixArea);
        mmap.put("machineType", machineInfo);
        return prefix + "/statistics";
    }

    @ApiOperation("获取对应的字段统计信息")
    @PostMapping("/statistics")
    @ResponseBody
    public TableDataInfo statistics(MaterialScheduleResult materialScheduleResult) {
        return iMaterialScheduleResultService.statistics(materialScheduleResult);
    }

    @ApiOperation("超期预警页面")
    @GetMapping("/toExpireWarning")
    public String toExpireWarning(ModelMap mmap, String mixArea) {
        mmap.put("mixArea",mixArea);
        return prefix + "/expireWarning";
    }

    @ApiOperation("获取超期预警统计信息")
    @PostMapping("/expireWarning")
    @ResponseBody
    public TableDataInfo expireWarning(MaterialScheduleResult materialScheduleResult) {
        return iMaterialScheduleResultService.expireWarning(materialScheduleResult);
    }

    @ApiOperation("根据条件查询硫磺辅料日计划跨区发送列表")
    @RequiresPermissions("schedule:materialScheduleResult:materialSpanSend")
    @PostMapping("/listMaterialSpanSend")
    @ResponseBody
    public TableDataInfo listMaterialSpanSend(MaterialSpanSend entity) {
        return iMaterialScheduleResultService.listMaterialSpanSend(entity);
    }

    @ApiOperation("发送跨区请求")
    @RequiresPermissions("schedule:materialScheduleResult:materialSpanSend")
    @PostMapping("/sendMaterialSpan")
    @ResponseBody
    public AjaxResult sendMaterialSpan(MaterialSpanSendDto dto) {
        return iMaterialScheduleResultService.sendMaterialSpan(dto);
    }

    @ApiOperation("根据条件查询硫磺辅料日计划跨区接收列表")
    @RequiresPermissions("schedule:materialScheduleResult:materialSpanReceive")
    @PostMapping("/listMaterialSpanReceive")
    @ResponseBody
    public TableDataInfo listMaterialSpanReceive(MaterialSpanReceive entity) {
        return iMaterialScheduleResultService.listMaterialSpanReceive(entity);
    }

    @ApiOperation("接收跨区请求")
    @RequiresPermissions("schedule:materialScheduleResult:materialSpanReceive")
    @PostMapping("/receiveMaterialSpanReceive")
    @ResponseBody
    public AjaxResult receiveMaterialSpanReceive(MaterialSpanReceiveDto dto) {
        return iMaterialScheduleResultService.receiveMaterialSpanReceive(dto);
    }

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     *
     * @param scheduleResult 参数
     * @return 结果
     */
    @ApiOperation("根据排程日期、密炼区、机台，查询机台的各班次总计划量")
    @PostMapping("/getSumQtyByMachineCode")
    @ResponseBody
    public AjaxResult getSumQtyByMachineCode(MaterialScheduleResult scheduleResult) {
        MaterialSpanReceiveQtyDto sumQtyByMachineCode = iMaterialScheduleResultService.getSumQtyByMachineCode(scheduleResult);
        return AjaxResult.success(sumQtyByMachineCode == null ? new MaterialSpanReceiveQtyDto() : sumQtyByMachineCode);
    }

    /**
     * 删除跨区发送请求
     * @param ids 要删除的跨区发送请求id
     * @return 结果
     */
    @ApiOperation("删除跨区发送请求")
    @RequiresPermissions("materialScheduleResult:materialSpanSend:remove")
    @PostMapping("/deleteMaterialSpanSend")
    @ResponseBody
    public AjaxResult deleteMaterialSpanSend(Long[] ids) {
        return iMaterialScheduleResultService.deleteMaterialSpanSend(ids);
    }

    /**
     * 获取排产日时间，要根据当前时间确定：16点之前显示当天；16点之后显示下一天
     * @return
     */
	private Date getScheduleDate() {
    	Date initDate = new Date();
    	String nowHour = DateUtils.parseDateToStr("HH", initDate); // 当前整点数
    	if (nowHour.compareTo(DAY_SWITCH_TIME) >= 0) { // 超过换班时间的，日期需要+1
    		initDate = DateUtils.addDays(initDate, 1);
    	}
		return initDate;
	}

	/**
	 * 设置IP
	 * @param glueScheduleResult
	 */
	private void setUserIp(MaterialScheduleResult materialScheduleResult) {
		materialScheduleResult.setOperIp(IpUtils.getIpAddr(ServletUtils.getRequest()));// 获取客户端IP
	}
}
