package com.zlt.mix.schedule.controller;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.engine.domain.SyncDataLogs;
import com.zlt.mix.common.engine.service.SyncDataLogsService;
import com.zlt.mix.schedule.api.domain.dto.*;
import com.zlt.mix.schedule.api.domain.entity.*;
import com.zlt.mix.schedule.common.handle.MixSyncDataHandle;
import com.zlt.mix.schedule.common.utils.ScheduleUtils;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.service.GlueDecomposePlanService;
import com.zlt.mix.schedule.service.GlueScheduleResultService;
import com.zlt.mix.schedule.service.GlueScheduleSupplementService;
import com.zlt.mix.schedule.service.ScheduleRedisLockService;
import com.zlt.sync.povo.SyncParamsVO;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 终炼/母炼日计划排程Controller
 *
 * @author chen
 * @date 2022-05-16
 */
@RestController
@RequestMapping("/glueScheduleResult")
public class GlueScheduleResultController extends BaseController {
    @Resource
    private GlueScheduleResultService glueScheduleResultService;
    @Autowired
    private ScheduleRedisLockService scheduleRedisLockService;
    @Autowired
    private GlueDecomposePlanService glueDecomposePlanService;
    @Autowired
    private GlueScheduleSupplementService glueScheduleSupplementService;
    @Autowired
    private PreAuthorizeAspect preAuthorizeAspect;

    @Resource
    private MixSyncDataHandle syncDataHandle;
    @Resource
    private SyncDataLogsService syncDataLogsService;
    @Resource
    private ISysDictDataCacheService iSysDictDataCacheService;
    @Value("${glueClassEditableRole:admin}")
    public String classEditableRole;

    public String syncKey = "MIX_GLUE_SCHE_FBK";

    /**
     * 查询终炼/母炼日计划排程列表
     */
    @ApiOperation("查询终炼/母炼日计划排程列表")
    @PostMapping("/list")
    public TableDataInfo listGlueScheduleResult(@RequestBody GlueScheduleResult glueScheduleResult) throws ParseException {
        startPage(false);
        glueScheduleResult.setOrderStr(orderStr());
        if (glueScheduleResult.getScheduleDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(DateUtils.addDays(new Date(), 1));
            glueScheduleResult.setScheduleDate(sdf.parse(format));
        }
        List<GlueScheduleResult> list = glueScheduleResultService.selectGlueScheduleResultList(glueScheduleResult);
        return getDataTable(list);
    }

    @ApiOperation("获取终炼/母炼日计划排程详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GlueScheduleResult getGlueScheduleResultInfo(@PathVariable("id") Long id){
        return glueScheduleResultService.getById(id);
    }

    @Log(title = "schedule.glueScheduleResult.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存终炼/母炼日计划排程信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveGlueScheduleResult(@RequestBody GlueScheduleResult glueScheduleResult) {
        Long id = glueScheduleResult.getId();
        List<GlueScheduleResult> list = glueScheduleResultService.saveGlueScheduleResult(glueScheduleResult);
        //如果为修改状态，返回发布状态，用于编辑回显
        if (id != null) {
            return AjaxResult.success(I18nUtil.getMessage("common.msg.ajax.operation.success"), list);
        }
        return AjaxResult.success();
    }

    @Log(title = "schedule.glueScheduleResult.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除终炼/母炼日计划排程")
	@PostMapping("/delete/{ids}/{isChangeMasterbatch}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueScheduleResult(@PathVariable Long[] ids, @PathVariable("isChangeMasterbatch") Boolean isChangeMasterbatch){
        int noRelease = glueScheduleResultService.isNoReleaseByIds(ids);
        if (noRelease > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.scheduleResult.release.isNoReleaseByIds"));
        }
        return toAjax(glueScheduleResultService.deleteGlueScheduleResultByIds(ids, isChangeMasterbatch));
    }

    @Log(title = "schedule.glueScheduleResult.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出终炼/母炼日计划排程列表")
    @PostMapping("/exportData")
    public byte[] exportData(@RequestBody GlueScheduleResultExportDictDto dto) throws ParseException {
        startPage(false);
        dto.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        if (dto.getScheduleDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(DateUtils.addDays(new Date(), 1));
            dto.setScheduleDate(sdf.parse(format));
        }
        return glueScheduleResultService.exportData(dto);
    }

    @ApiOperation("校验终炼/母炼日计划排程唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkGlueScheduleResultUnique")
    public String checkGlueScheduleResultUnique(@RequestBody GlueScheduleResult glueScheduleResult){
        return glueScheduleResultService.checkGlueScheduleResultUnique(glueScheduleResult);
    }

    @Log(title = "schedule.glueScheduleResult.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入终炼/母炼日计划排程数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<GlueScheduleResult> list, @RequestParam("scheduleDate") String scheduleDate, @RequestParam("mixArea") String mixArea, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return glueScheduleResultService.importData(list, DateUtils.parseDate(scheduleDate), mixArea, importLogId);
    }

    /**
     * 批量转机台
     */
    @Log(title = "schedule.glueScheduleResult.modelName", newBusinessType = BusinessConstant.CHANGE_MACHINE)
    @ApiOperation("批量转机台")
    @PostMapping("/batchChangeMachine/{machineCode}")
    public AjaxResult batchChangeMachine(@PathVariable("machineCode") String machineCode, @RequestParam("ids") String ids) {
        return glueScheduleResultService.batchChangeMachine(machineCode, Convert.toLongArray(ids));
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody GlueScheduleResult glueScheduleResult) {
        return glueScheduleResultService.changeMachine(glueScheduleResult);
    }

    /**
     * 发布终炼母炼日计划
     */
    @Log(title = "schedule.glueScheduleResult.modelName", newBusinessType = BusinessConstant.PUBLISH)
    @ApiOperation("发布终炼母炼日计划")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody GlueScheduleResult glueScheduleResult) {
        Long[] idArray = null;
    	if (!StringUtils.isEmpty(glueScheduleResult.getIds())) {
            idArray = Convert.toLongArray(glueScheduleResult.getIds());
        	// 数据库操作前针对发布记录加锁，防止出现一个操作发了两次请求
    		if (scheduleRedisLockService.checkLocking("glue:publish:lock", StringUtils.join(idArray))) {
                return AjaxResult.error(I18nUtil.getMessage("ui.scheduleResult.publish.isLock"));
    		}
    	}
    	if (idArray == null || idArray.length == 0){
    	    return AjaxResult.error(I18nUtil.getMessage("ui.frame.alter.mustChooseOneRecord"));
    	}

    	AjaxResult ajaxResult;
        try {
            //获取数据版本号
            String dataVersion = syncDataHandle.getDataVersion(syncKey);
            // 厂别、分公司编号
            String factoryCode = "116";
            String companyCode = "116";
            glueScheduleResult.setDataVersion(dataVersion);
            glueScheduleResult.setFactoryCode(factoryCode);
            glueScheduleResult.setCompanyCode(companyCode);
            ajaxResult = glueScheduleResultService.publish(glueScheduleResult); // 中间库同步
            if (new Integer(HttpStatus.SUCCESS).equals(ajaxResult.get(AjaxResult.CODE_TAG))) { // 中间库同步成功后，推送MQ
                SyncParamsVO syncParamsVO = new SyncParamsVO();
                syncParamsVO.setSyncKey(syncKey);
                syncParamsVO.setDataVersion(dataVersion);
                // 请求参数
                JSONObject params = new JSONObject();
                params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, glueScheduleResult.getScheduleDate()));
                params.put("rowCount", idArray.length);
                syncParamsVO.setParams(params);
                syncParamsVO.setFactoryCode(factoryCode);
                syncParamsVO.setCompanyCode(companyCode);
                syncDataHandle.syncNotice(syncParamsVO);

                // 取回mes的反馈结果
                SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
                String status = logs.getStatus();
                // 更新状态
                glueScheduleResultService.updateRelaseStatus(idArray, status);
                if (ZltConstant.IS_RELEASE.equals(status)) {
                    // 成功
                    ajaxResult = AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
                } else {
                    // 失败，需要返回异常信息
                    ajaxResult = AjaxResult.error(logs.getMsg());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }

        return ajaxResult;
    }

    /**
     * 自动排程
     */
    @Log(title = "schedule.glueScheduleResult.modelName", newBusinessType = BusinessConstant.GENERATE_SCHEDULE)
    @ApiOperation("自动排程")
    @PostMapping("/autoSchedule")
    public AjaxResult autoSchedule(@RequestBody GlueScheduleResult glueScheduleResult) {
    	if (StringUtils.isEmpty(glueScheduleResult.getMixArea())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.scheduleResult.mixArea.isBlank"));
    	}
    	String mixArea = glueScheduleResult.getMixArea();
    	Date scheduleDate = glueScheduleResult.getScheduleDate();

    	// 检查对应的排产日与密炼区是否有终炼母炼日计划记录，没有则直接提示错误
    	GlueDecomposePlan glueDecomposePlan = new GlueDecomposePlan();
    	glueDecomposePlan.setMixArea(mixArea);
    	glueDecomposePlan.setPlanDate(scheduleDate);
    	if (ZltConstant.UNIQUE.equals(glueDecomposePlanService.checkPlanDateAndMixAreaExist(glueDecomposePlan))) {
            return AjaxResult.error(I18nUtil.getMessage("schedule.glueScheduleResult.autoSchedule.fail"));
    	}
    	// 检查对应的胶料需求量记录是否存在机台不唯一的记录
    	if (ZltConstant.NOT_UNIQUE.equals(glueDecomposePlanService.checkMachineError(glueDecomposePlan))) {
            String planDateStr = DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate);
            List<SysDictData> mixAreaList = iSysDictDataCacheService.getType("MIX_AREA");
            Map<String, String> mixAreaMap = mixAreaList.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (v1, v2) -> v1));
            return AjaxResult.error(StringUtils.format(I18nUtil.getMessage("schedule.glueScheduleResult.machine.error"), planDateStr, mixAreaMap.getOrDefault(mixArea, mixArea)));
    	}

    	// 数据库操作前针对密炼区加锁，防止出现一个操作发了两次请求
		if (scheduleRedisLockService.checkLocking("glue:schedule:lock", glueScheduleResult.getMixArea())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.scheduleResult.autoSchedule.isLock"));
		}

		glueScheduleResultService.autoGlueSchedule(glueScheduleResult); // 调用接口
        return AjaxResult.success();
    }

    @ApiOperation("检测对应日期和密炼区的数据是否存在")
    @PostMapping("/checkScheduleDateAndMixAreaExist")
    public String checkScheduleDateAndMixAreaExist(@RequestBody GlueScheduleResult glueScheduleResult) {
        return glueScheduleResultService.checkScheduleDateAndMixAreaExist(glueScheduleResult);
    }

    /**
     * 重排
     */
    @Log(title = "schedule.glueScheduleResult.modelName", newBusinessType = BusinessConstant.RESCHEDULE)
    @ApiOperation("重排")
    @PostMapping("/reschedule")
    public AjaxResult reschedule(@RequestBody GlueScheduleResult glueScheduleResult) {
        // TODO 调用重新排程接口，日志类型更改
        return AjaxResult.success("未调用引擎接口");
    }

    /**
     * 更改配方信息
     */
    @Log(title = "schedule.glueScheduleResult.modelName", newBusinessType = BusinessConstant.CHANGE_RECIPE)
    @ApiOperation("更改配方信息")
    @PostMapping("/changeRecipe")
    public AjaxResult changeRecipe(@RequestBody GlueScheduleResult glueScheduleResult) {
        return glueScheduleResultService.changeRecipe(glueScheduleResult);
    }

    @ApiOperation("获取统计信息")
    @PostMapping("/statistics")
    public TableDataInfo statistics(@RequestBody GlueScheduleResult glueScheduleResult){
        List<GlueScheduleResultStatisticsDto> list=glueScheduleResultService.statistics(glueScheduleResult);
        return getDataTable(list);
    }

    /**
     * 根据条件查询终炼母炼日计划跨区发送列表
     * @param entity 查询条件
     * @return 结果
     */
    @ApiOperation("根据条件查询终炼母炼日计划跨区发送列表")
    @PostMapping("/listGlueSpanSend")
    public TableDataInfo listGlueSpanSend(@RequestBody GlueSpanSend entity) {
        startPage(false);
        entity.setOrderStr(orderStr());
        List<GlueSpanSend> list = glueScheduleResultService.listGlueSpanSend(entity);
        return getDataTable(list);
    }

    /**
     * 发送跨区请求
     * @param dto 跨区请求集合
     * @return 结果
     */
    @ApiOperation("发送跨区请求")
    @PostMapping("/sendGlueSpan")
    public AjaxResult sendGlueSpan(@RequestBody GlueSpanSendDto dto) throws ParseException {
        return glueScheduleResultService.sendGlueSpan(dto);
    }

    /**
     * 根据条件查询终炼母炼日计划跨区接收列表
     * @param entity 查询条件
     * @return 结果
     */
    @ApiOperation("根据条件查询终炼母炼日计划跨区接收列表")
    @PostMapping("/listGlueSpanReceive")
    public TableDataInfo listGlueSpanReceive(@RequestBody GlueSpanReceive entity) {
        startPage(false);
        entity.setOrderStr(orderStr());
        List<GlueSpanReceive> list = glueScheduleResultService.listGlueSpanReceive(entity);
        return getDataTable(list);
    }

    /**
     * 接收跨区请求
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @ApiOperation("接收跨区请求")
    @PostMapping("/receiveGlueSpanReceive")
    public AjaxResult receiveGlueSpanReceive(@RequestBody GlueSpanReceiveDto dto) {
        return glueScheduleResultService.receiveGlueSpanReceive(dto);
    }

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     *
     * @param glueScheduleResult 参数
     * @return 结果
     */
    @ApiOperation("根据排程日期、密炼区、机台，查询机台的各班次总计划量")
    @PostMapping("/getSumQtyByMachineCode")
    public GlueSpanReceiveQtyDto getSumQtyByMachineCode(@RequestBody GlueScheduleResult glueScheduleResult) {
        return glueScheduleResultService.getSumQtyByMachineCode(glueScheduleResult);
    }

    /**
     * 删除跨区发送请求
     * @param ids 要删除的跨区发送请求id
     * @return 结果
     */
    @ApiOperation("删除跨区发送请求")
    @PostMapping("/deleteGlueSpanSend/{ids}")
    public AjaxResult deleteGlueSpanSend(@PathVariable("ids") Long[] ids) {
        return glueScheduleResultService.deleteGlueSpanSend(ids);
    }

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    @ApiOperation("根据选中的ids查询跨区发送时要携带的字段")
    @PostMapping("/selectSpanSendNeedFieldByIds/{ids}")
    public List<GlueScheduleResult> selectSpanSendNeedFieldByIds(@PathVariable("ids") Long[] ids) {
        return glueScheduleResultService.selectSpanSendNeedFieldByIds(ids);
    }


    /**
     * 计算终炼/母炼日计划补量列表
     */
    @ApiOperation("计算终炼/母炼日计划补量列表")
    @PostMapping("/caculateSupplement")
    public TableDataInfo caculateSupplement(@RequestBody GlueScheduleSupplement glueScheduleSupplement) {
        List<GlueScheduleSupplement> list = glueScheduleSupplementService.caculateSuppliment(glueScheduleSupplement);
    	return getDataTable(list);
    }

	@ApiOperation("保存生产补量记录")
    @PostMapping("/saveSupplement")
	public AjaxResult saveSupplement(@RequestBody List<GlueScheduleSupplement> glueScheduleSupplementList) {
        return glueScheduleSupplementService.saveSupplement(glueScheduleSupplementList);
	}

    /**
     * 检查班次是否可编辑
     * @param scheduleDate	排产日期
     * @param classShift	班次编号
     * @return
     */
	@ApiOperation("检查班次是否可编辑")
    @PostMapping("/checkCLassEditable")
    public Boolean checkCLassEditable(@RequestBody ScheduleClassEditableDto dto) {
		Date scheduleDate = dto.getScheduleDate();
		Integer classShift = dto.getClassShift();
    	if(preAuthorizeAspect.hasRole(classEditableRole)) {
    		// 管理员角色不受限制，都可以修改
    		return true;
    	}
    	return ScheduleUtils.checkCLassEditable(scheduleDate, classShift);
    }

    /**
     * 获取各班次可编辑状态
     * @param scheduleDate	排产日期
     * @param classShift	班次编号
     * @return
     */
	@ApiOperation("获取各班次可编辑状态")
    @PostMapping("/getCLassEditableStatus")
	public ScheduleClassEditableDto getCLassEditableStatus(@RequestBody ScheduleClassEditableDto dto) {
		ScheduleClassEditableDto result = new ScheduleClassEditableDto();
		// 分别校验班次
		dto.setClassShift(GlueEngineConstants.SHIFT_CLASS_MID);
		result.setMidEditable(this.checkCLassEditable(dto));
		dto.setClassShift(GlueEngineConstants.SHIFT_CLASS_NIGHT);
		result.setNightEditable(this.checkCLassEditable(dto));
		dto.setClassShift(GlueEngineConstants.SHIFT_CLASS_DAY);
		result.setDayEditable(this.checkCLassEditable(dto));
		return result;
	}

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody GlueScheduleResult scheduleResult) throws ParseException {
        if (scheduleResult.getScheduleDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(DateUtils.addDays(new Date(), 1));
            scheduleResult.setScheduleDate(sdf.parse(format));
        }
        return glueScheduleResultService.getSummaryVo(scheduleResult);
    }
}
