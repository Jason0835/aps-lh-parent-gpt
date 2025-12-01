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
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.schedule.api.domain.dto.*;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleSupplement;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import com.zlt.mix.schedule.api.service.IGlueScheduleResultService;
import com.zlt.mix.schedule.api.service.IGlueSpanReceiveService;
import com.zlt.mix.service.SettingService;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
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
 * 终炼母炼日计划排程Controller
 *
 * @author chen
 * @date 2022-05-16
 */
@Api(tags = "终炼母炼日计划排程")
@Controller
@RequestMapping("/schedule/glueScheduleResult")
public class GlueScheduleResultController extends BaseController {

    @Resource
    private IGlueScheduleResultService iGlueScheduleResultService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;
    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    @Autowired
    private SettingService settingService;

    @Resource
    private IGlueSpanReceiveService iGlueSpanReceiveService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    private final String prefix = "schedule/glueScheduleResult";
	/**
	 * 排产日切换时间
	 */
	private static String DAY_SWITCH_TIME = "16";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("schedule:glueScheduleResult:view")
    @GetMapping()
    public String toIndex(ModelMap modelMap) {
    	Date scheduleDate = this.getScheduleDate(); // 排产日
        modelMap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
        GlueSpanReceive glueSpanReceive = new GlueSpanReceive();
        glueSpanReceive.setScheduleDate(scheduleDate);
        glueSpanReceive.setSource(ZltConstant.SOURCE_GLUE_SCHEDULE_RESULT);
        glueSpanReceive.setEntrustedMixArea(ZltConstant.DEFAULT_MIX_AREA);
        modelMap.put("notReceivedQuantity", iGlueSpanReceiveService.selectUnReceiveCount(glueSpanReceive));
        return prefix + "/glueScheduleResult";
    }

    @ApiOperation("根据条件查询终炼母炼日计划排程列表")
    @RequiresPermissions("schedule:glueScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listGlueScheduleResult(GlueScheduleResult entity) {
        return iGlueScheduleResultService.listGlueScheduleResult(entity);
    }

    /**
     * 跳转至插单页面
     */
    @ApiOperation("跳转至插单页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        GlueScheduleResult glueScheduleResult = new GlueScheduleResult();
        glueScheduleResult.setScheduleDate(this.getScheduleDate()); // 排产日
        mmap.put("glueScheduleResult", glueScheduleResult);
        mmap.put("minDate", DateUtils.getNowDateByyyMMdd());
        return prefix + "/add";
    }

    /**
     * 跳转至编辑页面
     */
    @ApiOperation("跳转至编辑页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
    	GlueScheduleResult schedule = iGlueScheduleResultService.getGlueScheduleResultInfo(id);
        ScheduleClassEditableDto editableStatusDto = new ScheduleClassEditableDto();
        editableStatusDto.setScheduleDate(schedule.getScheduleDate());
        mmap.put("editableStatus", iGlueScheduleResultService.getCLassEditableStatus(editableStatusDto)); // 加载班次可编辑状态
        mmap.put("glueScheduleResult", schedule);
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
        mmap.put("glueScheduleResult", iGlueScheduleResultService.getGlueScheduleResultInfo(id));
        return prefix + "/changeMachine";
    }

    @ApiOperation("跳转至导入页面")
    @GetMapping("/importData")
    public String importData(ModelMap mmp) {
        mmp.put("prefix", prefix);
        mmp.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/importData";
    }

    /**
     * 跳转至重排页面
     */
    @ApiOperation("跳转至重排页面")
    @GetMapping("/toRePlan")
    public String toRePlan(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/rePlan";
    }

    @ApiOperation("更改配方信息页面")
    @GetMapping("/toChangeRecipe")
    public String toChangeRecipe(ModelMap mmap, String machineName, String glue, String id) {
        mmap.put("machineName", machineName);
        mmap.put("glue", glue);
        mmap.put("id", id);
        GlueScheduleResult glueScheduleResultInfo = iGlueScheduleResultService.getGlueScheduleResultInfo(Long.valueOf(id));
        mmap.put("recipeType", glueScheduleResultInfo.getRecipeType());
        mmap.put("recipeTypeName", glueScheduleResultInfo.getRecipeTypeName());
        mmap.put("recipeVersionId", glueScheduleResultInfo.getRecipeVersionId());
        mmap.put("recipeStage", glueScheduleResultInfo.getRecipeStage());
        return prefix + "/changeRecipe";
    }

    @ApiOperation("插单选配方页面")
    @GetMapping("/toChooseRecipeType")
    public String toChooseRecipeType(ModelMap mmap, String machineName, String glue) {
        mmap.put("machineName", machineName);
        mmap.put("glue", glue);
        return prefix + "/chooseRecipeType";
    }

    @ApiOperation("统计字段页面")
    @GetMapping("/toStatistics")
    public String toStatistics(ModelMap mmap, Date scheduleDate,String mixArea, String machineCode) {
        GlueScheduleResult glueScheduleResult = new GlueScheduleResult();
        glueScheduleResult.setScheduleDate(scheduleDate);
        glueScheduleResult.setMixArea(mixArea);
        glueScheduleResult.setMachineCode(machineCode);
        mmap.put("glueScheduleResult",glueScheduleResult);
        //获取对应密炼区的机台信息
        List<MixMachine> machineInfo = settingService.getMachineInfo(mixArea);
        mmap.put("machineType", machineInfo);
        return prefix + "/statistics";
    }

    @ApiOperation("跳转至跨区发送页面")
    @GetMapping("/toSendCrossRegional")
    public String toSendCrossRegional(String mixArea, String ids, ModelMap mmap) {
        mmap.put("mixArea", mixArea);
        mmap.put("sendPerson", AuthorizationUtils.getSysUser().getUserName());
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        // 查询选中的记录回写发送页
        List<GlueScheduleResult> selectList = new ArrayList<>();
        if (StringUtils.isNotBlank(ids)) {
            Long[] scheduleIds = Convert.toLongArray(ids);
            selectList = iGlueScheduleResultService.selectSpanSendNeedFieldByIds(scheduleIds);
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
    public String toChooseMachine(GlueSpanReceive param, ModelMap mmap) {
        GlueSpanReceive receiveInfo = iGlueSpanReceiveService.getGlueSpanReceiveInfo(param);
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
        mmap.put("glueSpanReceive", receiveInfo);
        return prefix + "/receiveChooseMachine";
    }

	@ApiOperation("跳转至生产补量页面")
	@GetMapping("/supplement/{mixArea}/{scheduleDate}")
    @RequiresPermissions("schedule:glueScheduleResult:supplement")
	public String supplement(@PathVariable("mixArea") String mixArea, @PathVariable("scheduleDate") Date scheduleDate,
			ModelMap mmap) {
		mmap.put("scheduleDate", DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
		mmap.put("entrustedMixArea", mixArea);
		return prefix + "/supplement";
	}

    @ApiOperation("获取对应的字段统计信息")
    @PostMapping("/statistics")
    @ResponseBody
    public TableDataInfo statistics(GlueScheduleResult glueScheduleResult) {
        TableDataInfo statistics = iGlueScheduleResultService.statistics(glueScheduleResult);
        return statistics;
    }

    @ApiOperation("修改或新增终炼母炼日计划排程")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueScheduleResult(GlueScheduleResult glueScheduleResult) {
    	this.setUserIp(glueScheduleResult);
        return iGlueScheduleResultService.saveGlueScheduleResult(glueScheduleResult);
    }

    @ApiOperation("删除终炼母炼日计划排程（id不为空）")
    @RequiresPermissions("schedule:glueScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeGlueScheduleResult(String ids, Boolean isChangeMasterbatch) {
        Long[] arr = Convert.toLongArray(ids);
        if (isChangeMasterbatch == null) {
            isChangeMasterbatch = false;
        }
        return iGlueScheduleResultService.deleteGlueScheduleResult(arr, isChangeMasterbatch);
    }

    @ApiOperation("校验终炼母炼日计划排程唯一性")
    @PostMapping("/checkGlueScheduleResultUnique")
    @ResponseBody
    public String checkGlueScheduleResultUnique(GlueScheduleResult glueScheduleResult) {
        return iGlueScheduleResultService.checkGlueScheduleResultUnique(glueScheduleResult);
    }

    /**
     * 导出终炼母炼日计划排程
     */
    @ApiOperation("导出终炼母炼日计划排程")
    @RequiresPermissions("schedule:glueScheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GlueScheduleResult glueScheduleResult) throws IOException {
        String fileName = I18nUtil.getMessage("schedule.glueScheduleResult.modelName");
        GlueScheduleResultExportDictDto dictDto = new GlueScheduleResultExportDictDto();
        HashMap<String, String> mixAreaDictMap = iSysDictDataCacheService.getType("MIX_AREA").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        HashMap<String, String> recipeStageDictMap = iSysDictDataCacheService.getType("PRODUCT_STAGE").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        HashMap<String, String> releaseStatusDictMap = iSysDictDataCacheService.getType("MIX_RELEASE_STATUS").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        dictDto.setMixAreaDictMap(mixAreaDictMap);
        dictDto.setRecipeStageDictMap(recipeStageDictMap);
        dictDto.setReleaseStatusDictMap(releaseStatusDictMap);
        BeanUtils.copyProperties(glueScheduleResult, dictDto);
        byte[] data = iGlueScheduleResultService.exportData(dictDto);
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
        String fileName = I18nUtil.getMessage("schedule.glueScheduleResult.modelName");
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
    @RequiresPermissions("schedule:glueScheduleResult:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date scheduleDate, String mixArea) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_MIX,
                I18nUtil.getMessage("schedule.glueScheduleResult.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        List<GlueScheduleResult> list = parseObject(in, scheduleDate, mixArea);
        //导入数据
        AjaxResult ajaxResult = iGlueScheduleResultService.importData(list, DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate), mixArea, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 转机台
     */
    @RequiresPermissions("schedule:glueScheduleResult:changeMachine")
    @ApiOperation("转机台")
    @PostMapping("/batchChangeMachine/{machineCode}")
    @ResponseBody
    public AjaxResult batchChangeMachine(@PathVariable("machineCode") String machineCode, String ids) {
        return iGlueScheduleResultService.batchChangeMachine(machineCode, ids);
    }

    /**
     * 转机台
     */
    @RequiresPermissions("schedule:glueScheduleResult:changeMachine")
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(GlueScheduleResult glueScheduleResult) {
    	this.setUserIp(glueScheduleResult);
        return iGlueScheduleResultService.changeMachine(glueScheduleResult);
    }

    /**
     * 自动排程校验
     */
    @ApiOperation("自动排程校验")
    @PostMapping("/validateAutoPlan")
    @ResponseBody
    public AjaxResult validateAutoPlan(GlueScheduleResult glueScheduleResult) {
        if (glueScheduleResult.getScheduleDate() == null) {
            glueScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        // TODO 调用校验接口
        return AjaxResult.success("2");
    }

    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @RequiresPermissions("schedule:glueScheduleResult:autoSchedule")
    @PostMapping("/autoSchedule")
    @ResponseBody
    public AjaxResult autoSchedule(GlueScheduleResult glueScheduleResult) {
        if (glueScheduleResult.getScheduleDate() == null) {
            glueScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
    	this.setUserIp(glueScheduleResult);
        return iGlueScheduleResultService.autoSchedule(glueScheduleResult);
    }

    /**
     * 排程发布校验
     */
    @ApiOperation("排程发布前校验")
    @PostMapping("/publishValidate")
    @ResponseBody
    public AjaxResult publishValidate(GlueScheduleResult glueScheduleResult) {
        // TODO 调用校验接口
        return AjaxResult.success();
    }

    /**
     * 排程发布
     */
    @ApiOperation("排程发布")
    @RequiresPermissions("schedule:glueScheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(GlueScheduleResult glueScheduleResult) {
        if (glueScheduleResult.getScheduleDate() == null) {
            glueScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
    	this.setUserIp(glueScheduleResult);
        return iGlueScheduleResultService.publish(glueScheduleResult);
    }

    @ApiOperation("检测对应日期和密炼区的数据是否存在")
    @PostMapping("/checkScheduleDateAndMixAreaExist")
    @ResponseBody
    public AjaxResult checkScheduleDateAndMixAreaExist(GlueScheduleResult glueScheduleResult) {
        String unique = iGlueScheduleResultService.checkScheduleDateAndMixAreaExist(glueScheduleResult);

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
     * 重排校验
     */
    @ApiOperation("重排校验")
    @PostMapping("/validateRePlan")
    @ResponseBody
    public AjaxResult validateRePlan(GlueScheduleResult glueScheduleResult) {
        if (glueScheduleResult.getScheduleDate() == null) {
            glueScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        // TODO 调用重排校验接口（如果返回结果有多种可能，需要同步修改前端rePlan.html）
        return AjaxResult.success();
    }

    /**
     * 重排
     */
    @ApiOperation("重排")
    @RequiresPermissions("schedule:glueScheduleResult:reschedule")
    @PostMapping("/reschedule")
    @ResponseBody
    public AjaxResult reschedule(GlueScheduleResult glueScheduleResult) {
        if (glueScheduleResult.getScheduleDate() == null) {
            glueScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iGlueScheduleResultService.reschedule(glueScheduleResult);
    }

    /**
     * 更改配方信息
     */
    @ApiOperation("更改配方信息")
    @RequiresPermissions("schedule:glueScheduleResult:edit")
    @PostMapping("/changeRecipe")
    @ResponseBody
    public AjaxResult changeRecipe(GlueScheduleResult glueScheduleResult) {
    	this.setUserIp(glueScheduleResult);
        return iGlueScheduleResultService.changeRecipe(glueScheduleResult);
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
    	return AjaxResult.success(iGlueScheduleResultService.checkCLassEditable(dto));
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
    	return AjaxResult.success(iGlueScheduleResultService.getCLassEditableStatus(dto));
    }

    /**
     * 根据excel的io流解析成对应集合对象
     *
     * @param in io
     * @return 解析后集合对象
     * @throws Exception 异常
     */
    private List<GlueScheduleResult> parseObject(InputStream in, Date scheduleDate, String mixArea) throws Exception {
        List<GlueScheduleResult> list = new ArrayList<>();
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

            for (int i = 2; i < rows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                GlueScheduleResult scheduleResult = getRowData(row, productStageMap, isReleaseMap);
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

    private GlueScheduleResult getRowData(Row row, Map<String, String> productStageMap, Map<String, String> isReleaseMap) throws ParseException, IllegalAccessException {
        int cellNum = 0;

        String machineName = getCellValue(row.getCell(cellNum++));    //0   密炼机台名称
        String releaseStatus = getCellValue(row.getCell(cellNum++));    //1   发布状态
        String glue = getCellValue(row.getCell(cellNum++));    //2   胶料名称
        String recipeTypeName = getCellValue(row.getCell(cellNum++));    //3   配方类型名称
        String recipeVersionId = getCellValue(row.getCell(cellNum++));    //4   配方版本号
        String recipeStage = getCellValue(row.getCell(cellNum++));    //5   配方阶段
        String stockQty = getCellValue(row.getCell(cellNum++));   //6   库存
        String safeStockQty = getCellValue(row.getCell(cellNum++));   //7   安全库存
        String formulaWeight = getCellValue(row.getCell(cellNum++));   //8   配方重量
        String formulaTime = getCellValue(row.getCell(cellNum++));   //9   配方时间
        String totalPlanQty = getCellValue(row.getCell(cellNum++));  //10   总计划
        String totalSurplus = getCellValue(row.getCell(cellNum++));  //11   总剩余
        String totalFinish = getCellValue(row.getCell(cellNum++));  //12   总完成
        String midProduceOrder = getCellValue(row.getCell(cellNum++));  //13   中班顺序
        String midPlanQty = getCellValue(row.getCell(cellNum++));  //14   中班计划
        cellNum++; //15 中班完成，不需要解析，跳过
        cellNum++; //16 中班完成率，不需要解析，跳过
        cellNum++; //17 中班预计开始，不需要解析，跳过
        cellNum++; //18 中班预计完成时间，不需要解析，跳过
        String midRemark = getCellValue(row.getCell(cellNum++));  //19   中班备注
        String nightProduceOrder = getCellValue(row.getCell(cellNum++));  //20   夜班顺序
        String nightPlanQty = getCellValue(row.getCell(cellNum++));  //21   夜班计划
        cellNum++; //22 夜班完成，不需要解析，跳过
        cellNum++; //23 夜班完成率，不需要解析，跳过
        cellNum++; //24 夜班预计开始，不需要解析，跳过
        cellNum++; //25 夜班预计完成时间，不需要解析，跳过
        String nightRemark = getCellValue(row.getCell(cellNum++));  //26   夜班备注
        // String dayProduceOrder = getCellValue(row.getCell(cellNum++));  //27   白班顺序
        // String dayPlanQty = getCellValue(row.getCell(cellNum++));  //28   白班计划
        // cellNum++; //29 白班完成，不需要解析，跳过
        // cellNum++; //30 白班完成率，不需要解析，跳过
        // cellNum++; //31 白班预计开始，不需要解析，跳过
        // cellNum++; //32 白班预计完成时间，不需要解析，跳过
        // String dayRemark = getCellValue(row.getCell(cellNum));  //33   白班备注

        GlueScheduleResult entity = new GlueScheduleResult();
        // 是否发布字典转换
        if (StringUtils.isNotEmpty(releaseStatus) && StringUtils.isNotEmpty(isReleaseMap) && isReleaseMap.containsKey(releaseStatus)) {
            entity.setReleaseStatus(isReleaseMap.get(releaseStatus));
        }
        entity.setMachineName(machineName);
        entity.setGlue(glue);
        entity.setRecipeTypeName(recipeTypeName);
        entity.setRecipeVersionId(recipeVersionId);
        // 配方阶段字典转换
        if (StringUtils.isNotEmpty(recipeStage) && StringUtils.isNotEmpty(productStageMap) && productStageMap.containsKey(recipeStage)) {
            entity.setRecipeStage(productStageMap.get(recipeStage));
        }
        entity.setStockQty(getDoubleValue(stockQty));
        entity.setSafeStockQty(getDoubleValue(safeStockQty));
        entity.setFormulaWeight(getDoubleValue(formulaWeight));
        entity.setFormulaTime(getDoubleValue(formulaTime));
        entity.setTotalPlanQty(getDoubleValue(totalPlanQty));
        entity.setTotalSurplus(getDoubleValue(totalSurplus));
        entity.setTotalFinish(getDoubleValue(totalFinish));
        entity.setMidProduceOrder(getIntegerValue(midProduceOrder));
        entity.setMidPlanQty(getDoubleValue(midPlanQty));
        entity.setMidRemark(midRemark);
        entity.setNightProduceOrder(getIntegerValue(nightProduceOrder));
        entity.setNightPlanQty(getDoubleValue(nightPlanQty));
        entity.setNightRemark(nightRemark);
        // entity.setDayProduceOrder(getIntegerValue(dayProduceOrder));
        // entity.setDayPlanQty(getDoubleValue(dayPlanQty));
        // entity.setDayRemark(dayRemark);
        return checkObjFieldIsNull(entity) ? null : entity;
    }

    @ApiOperation("根据条件查询分解胶料需求量跨区发送列表")
    @RequiresPermissions("schedule:glueScheduleResult:glueSpanSend")
    @PostMapping("/listGlueSpanSend")
    @ResponseBody
    public TableDataInfo listGlueSpanSend(GlueSpanSend entity) {
        return iGlueScheduleResultService.listGlueSpanSend(entity);
    }

    @ApiOperation("发送跨区请求")
    @RequiresPermissions("schedule:glueScheduleResult:glueSpanSend")
    @PostMapping("/sendGlueSpan")
    @ResponseBody
    public AjaxResult sendGlueSpan(GlueSpanSendDto dto) {
        return iGlueScheduleResultService.sendGlueSpan(dto);
    }

    @ApiOperation("根据条件查询分解胶料需求量跨区接收列表")
    @RequiresPermissions("schedule:glueScheduleResult:glueSpanReceive")
    @PostMapping("/listGlueSpanReceive")
    @ResponseBody
    public TableDataInfo listGlueSpanReceive(GlueSpanReceive entity) {
        return iGlueScheduleResultService.listGlueSpanReceive(entity);
    }

    @ApiOperation("接收跨区请求")
    @RequiresPermissions("schedule:glueScheduleResult:glueSpanReceive")
    @PostMapping("/receiveGlueSpanReceive")
    @ResponseBody
    public AjaxResult receiveGlueSpanReceive(GlueSpanReceiveDto dto) {
        return iGlueScheduleResultService.receiveGlueSpanReceive(dto);
    }

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     *
     * @param glueScheduleResult 参数
     * @return 结果
     */
    @ApiOperation("根据排程日期、密炼区、机台，查询机台的各班次总计划量")
    @PostMapping("/getSumQtyByMachineCode")
    @ResponseBody
    public AjaxResult getSumQtyByMachineCode(GlueScheduleResult glueScheduleResult) {
        GlueSpanReceiveQtyDto sumQtyByMachineCode = iGlueScheduleResultService.getSumQtyByMachineCode(glueScheduleResult);
        return AjaxResult.success(sumQtyByMachineCode == null ? new GlueSpanReceiveQtyDto() : sumQtyByMachineCode);
    }

    /**
     * 删除跨区发送请求
     * @param ids 要删除的跨区发送请求id
     * @return 结果
     */
    @ApiOperation("删除跨区发送请求")
    @RequiresPermissions("glueScheduleResult:glueSpanSend:remove")
    @PostMapping("/deleteGlueSpanSend")
    @ResponseBody
    public AjaxResult deleteGlueSpanSend(Long[] ids) {
        return iGlueScheduleResultService.deleteGlueSpanSend(ids);
    }
    
    @ApiOperation("获取补量列表")
    @RequiresPermissions("schedule:glueScheduleResult:supplement")
    @PostMapping("/caculateSupplement")
    @ResponseBody
    public TableDataInfo caculateSuppliment(GlueScheduleSupplement entity) {
        return iGlueScheduleResultService.caculateSupplement(entity);
    }

	@ApiOperation("保存生产补量记录")
    @RequiresPermissions("schedule:glueScheduleResult:supplement")
    @PostMapping("/saveSupplement")
    @ResponseBody
	public AjaxResult saveSupplement(@RequestBody List<GlueScheduleSupplement> glueScheduleSupplementList) {
//		GlueScheduleSupplementDto supplement = new GlueScheduleSupplementDto();
        return iGlueScheduleResultService.saveSupplement(glueScheduleSupplementList);
	}

    @ApiOperation("选择配方信息页面")
    @GetMapping("/toSelectRecipe")
    public String toSelectRecipe(ModelMap mmap, String machineName, String glue, String id, String recipeType, String recipeVersionId) {
        mmap.put("machineName", machineName);
        mmap.put("id", id);
        mmap.put("glue", glue);
        mmap.put("recipeType", recipeType);
        mmap.put("recipeVersionId", recipeVersionId);
        return prefix + "/selectRecipe";
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
	private void setUserIp(GlueScheduleResult glueScheduleResult) {
		glueScheduleResult.setOperIp(IpUtils.getIpAddr(ServletUtils.getRequest()));// 获取客户端IP
	}

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    @ResponseBody
    public AjaxResult getSummaryVo(GlueScheduleResult scheduleResult) {
        return iGlueScheduleResultService.getSummaryVo(scheduleResult);
    }
}
