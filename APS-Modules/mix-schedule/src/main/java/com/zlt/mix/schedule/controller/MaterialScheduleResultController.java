package com.zlt.mix.schedule.controller;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.text.Convert;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.schedule.api.domain.dto.*;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanSend;
import com.zlt.mix.schedule.common.utils.ScheduleUtils;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.service.materialschedule.MaterialEngineService;
import com.zlt.mix.schedule.service.GlueScheduleResultService;
import com.zlt.mix.schedule.service.MaterialScheduleResultService;
import com.zlt.mix.schedule.service.ScheduleRedisLockService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 硫化辅料日计划排程Controller
 *
 * @author chen
 * @date 2022-05-24
 */
@RestController
@RequestMapping("/materialScheduleResult")
public class MaterialScheduleResultController extends BaseController {
    @Resource
    private MaterialScheduleResultService materialScheduleResultService;
    @Resource
    private MaterialEngineService materialEngineService;

    @Autowired
    private GlueScheduleResultService glueScheduleResultService;
    @Autowired
    private ScheduleRedisLockService scheduleRedisLockService;
    @Autowired
    private PreAuthorizeAspect preAuthorizeAspect;
    @Value("${materialClassEditableRole:admin}")
    public String classEditableRole;

    /**
     * 查询硫化辅料日计划排程列表
     */
    @ApiOperation("查询硫化辅料日计划排程列表")
    @PostMapping("/list")
    public TableDataInfo listMaterialScheduleResult(@RequestBody MaterialScheduleResult materialScheduleResult) throws ParseException {
        startPage(false);
        materialScheduleResult.setOrderStr(orderStr());
        if (materialScheduleResult.getScheduleDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(DateUtils.addDays(new Date(), 1));
            materialScheduleResult.setScheduleDate(sdf.parse(format));
        }
        List<MaterialScheduleResult> list = materialScheduleResultService.selectMaterialScheduleResultList(materialScheduleResult);
        return getDataTable(list);
    }

    @ApiOperation("获取硫化辅料日计划排程详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public MaterialScheduleResult getMaterialScheduleResultInfo(@PathVariable("id") Long id) {
        return materialScheduleResultService.getById(id);
    }

    @Log(title = "schedule.materialScheduleResult.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存硫化辅料日计划排程信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveMaterialScheduleResult(@RequestBody MaterialScheduleResult materialScheduleResult) {
        Long id = materialScheduleResult.getId();
        List<MaterialScheduleResult> list = materialScheduleResultService.saveMaterialScheduleResult(materialScheduleResult);
        //如果为修改状态，返回发布状态，用于编辑回显
        if (id != null) {
            return AjaxResult.success(I18nUtil.getMessage("common.msg.ajax.operation.success"), list);
        }
        return AjaxResult.success(list);
    }

    @Log(title = "schedule.materialScheduleResult.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除硫化辅料日计划排程")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteMaterialScheduleResult(@PathVariable Long[] ids) {
        int noRelease = materialScheduleResultService.isNoReleaseByIds(ids);
        if (noRelease > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.scheduleResult.release.isNoReleaseByIds"));
        }
        return toAjax(materialScheduleResultService.deleteMaterialScheduleResultByIds(ids));
    }

    @Log(title = "schedule.materialScheduleResult.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出硫化辅料日计划排程列表")
    @PostMapping("/exportData")
    public byte[] exportData(@RequestBody MaterialScheduleResultExportDictDto dto) throws ParseException {
        startPage(false);
        dto.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        if (dto.getScheduleDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(DateUtils.addDays(new Date(), 1));
            dto.setScheduleDate(sdf.parse(format));
        }
        return materialScheduleResultService.exportData(dto);
    }

    @ApiOperation("校验硫化辅料日计划排程唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkMaterialScheduleResultUnique")
    public String checkMaterialScheduleResultUnique(@RequestBody MaterialScheduleResult materialScheduleResult) {
        return materialScheduleResultService.checkMaterialScheduleResultUnique(materialScheduleResult);
    }

    @Log(title = "schedule.materialScheduleResult.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入硫化辅料日计划排程数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MaterialScheduleResult> list, @RequestParam("scheduleDate") String scheduleDate, @RequestParam("mixArea") String mixArea, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return materialScheduleResultService.importData(list, DateUtils.parseDate(scheduleDate), mixArea, importLogId);
    }

    /**
     * 批量转机台
     */
    @Log(title = "schedule.materialScheduleResult.modelName", newBusinessType = BusinessConstant.CHANGE_MACHINE)
    @ApiOperation("批量转机台")
    @PostMapping("/batchChangeMachine/{machineCode}")
    public AjaxResult batchChangeMachine(@PathVariable("machineCode") String machineCode, @RequestParam("ids") String ids) {
        return materialScheduleResultService.batchChangeMachine(machineCode, Convert.toLongArray(ids));
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody MaterialScheduleResult scheduleResult) {
        materialScheduleResultService.changeMachine(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 发布硫磺辅料日计划
     */
    @Log(title = "schedule.materialScheduleResult.modelName", newBusinessType = BusinessConstant.PUBLISH)
    @ApiOperation("发布硫磺辅料日计划")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody MaterialScheduleResult scheduleResult) {
    	if (!StringUtils.isEmpty(scheduleResult.getIds())) {
            Long[] idArray = Convert.toLongArray(scheduleResult.getIds());
        	// 数据库操作前针对发布记录加锁，防止出现一个操作发了两次请求
    		if (scheduleRedisLockService.checkLocking("material:publish:lock", StringUtil.join(idArray))) {
                return AjaxResult.error(I18nUtil.getMessage("ui.scheduleResult.publish.isLock"));
    		}
    	}

        return materialScheduleResultService.publish(scheduleResult);
    }

    /**
     * 自动排程
     */
    @Log(title = "schedule.materialScheduleResult.modelName", newBusinessType = BusinessConstant.GENERATE_SCHEDULE)
    @ApiOperation("自动排程")
    @PostMapping("/autoSchedule")
    public AjaxResult autoSchedule(@RequestBody MaterialScheduleResult scheduleResult) {
    	// 数据库操作前针对密炼区加锁，防止出现一个操作发了两次请求
		if (scheduleRedisLockService.checkLocking("material:schedule:lock", scheduleResult.getMixArea())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.scheduleResult.autoSchedule.isLock"));
		}
        //检查对应日期和密炼区的硫磺辅料日计划的是否有数据，如果没有直接返回错误提示
        GlueScheduleResult glueScheduleResult = new GlueScheduleResult();
        glueScheduleResult.setScheduleDate(scheduleResult.getScheduleDate());
        glueScheduleResult.setMixArea(scheduleResult.getMixArea());
        if(ZltConstant.UNIQUE.equals(glueScheduleResultService.checkScheduleDateAndMixAreaExist(glueScheduleResult))){
            throw new RuntimeException(I18nUtil.getMessage("schedule.materialScheduleResult.autoSchedule.fail"));
        }
        materialEngineService.autoSchedule(scheduleResult.getScheduleDate(), scheduleResult.getMixArea());  //调用硫磺辅料自动排程方法
        materialScheduleResultService.autoCreateMaterialSpanRecord(scheduleResult.getMixArea(), scheduleResult.getScheduleDate());  //自动排程后，根据跨区设置表，自动生产相应的跨区发送和接收记录
        materialScheduleResultService.saveAutoScheduleLog(scheduleResult); // 记录日志
        return AjaxResult.success();
    }

    @ApiOperation("检测对应日期和密炼区的数据是否存在")
    @PostMapping("/checkScheduleDateAndMixAreaExist")
    public String checkScheduleDateAndMixAreaExist(@RequestBody MaterialScheduleResult scheduleResult) {
        return materialScheduleResultService.checkScheduleDateAndMixAreaExist(scheduleResult);
    }

    /**
     * 更改配方信息
     */
    @Log(title = "schedule.materialScheduleResult.modelName", newBusinessType = BusinessConstant.CHANGE_RECIPE)
    @ApiOperation("更改配方信息")
    @PostMapping("/changeRecipe")
    public AjaxResult changeRecipe(@RequestBody MaterialScheduleResult materialScheduleResult) {
        return materialScheduleResultService.changeRecipe(materialScheduleResult);
    }

    @ApiOperation("获取统计信息")
    @PostMapping("/statistics")
    public TableDataInfo statistics(@RequestBody MaterialScheduleResult materialScheduleResult){
        List<MaterialScheduleResultStatisticsDto> list=materialScheduleResultService.statistics(materialScheduleResult);
        return getDataTable(list);
    }

    @ApiOperation("获取超期预警信息")
    @PostMapping("/expireWarning")
    public TableDataInfo expireWarning(@RequestBody MaterialScheduleResult materialScheduleResult){
        List<MaterialExpireWarningDto> list=materialScheduleResultService.expireWarning(materialScheduleResult);
        return getDataTable(list);
    }

    /**
     * 根据条件查询硫磺辅料日计划跨区发送列表
     * @param entity 查询条件
     * @return 结果
     */
    @ApiOperation("根据条件查询硫磺辅料日计划跨区发送列表")
    @PostMapping("/listMaterialSpanSend")
    public TableDataInfo listMaterialSpanSend(@RequestBody MaterialSpanSend entity) {
        startPage(false);
        entity.setOrderStr(orderStr());
        List<MaterialSpanSend> list = materialScheduleResultService.listMaterialSpanSend(entity);
        return getDataTable(list);
    }

    /**
     * 发送跨区请求
     * @param dto 跨区请求集合
     * @return 结果
     */
    @ApiOperation("发送跨区请求")
    @PostMapping("/sendMaterialSpan")
    public AjaxResult sendMaterialSpan(@RequestBody MaterialSpanSendDto dto) throws ParseException {
        return materialScheduleResultService.sendMaterialSpan(dto);
    }

    /**
     * 根据条件查询硫磺辅料日计划跨区接收列表
     * @param entity 查询条件
     * @return 结果
     */
    @ApiOperation("根据条件查询硫磺辅料日计划跨区接收列表")
    @PostMapping("/listMaterialSpanReceive")
    public TableDataInfo listMaterialSpanReceive(@RequestBody MaterialSpanReceive entity) {
        startPage(false);
        entity.setOrderStr(orderStr());
        List<MaterialSpanReceive> list = materialScheduleResultService.listMaterialSpanReceive(entity);
        return getDataTable(list);
    }

    /**
     * 接收跨区请求
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @ApiOperation("接收跨区请求")
    @PostMapping("/receiveMaterialSpanReceive")
    public AjaxResult receiveMaterialSpanReceive(@RequestBody MaterialSpanReceiveDto dto) {
        return materialScheduleResultService.receiveMaterialSpanReceive(dto);
    }

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     *
     * @param scheduleResult 参数
     * @return 结果
     */
    @ApiOperation("根据排程日期、密炼区、机台，查询机台的各班次总计划量")
    @PostMapping("/getSumQtyByMachineCode")
    public MaterialSpanReceiveQtyDto getSumQtyByMachineCode(@RequestBody MaterialScheduleResult scheduleResult) {
        return materialScheduleResultService.getSumQtyByMachineCode(scheduleResult);
    }

    /**
     * 删除跨区发送请求
     * @param ids 要删除的跨区发送请求id
     * @return 结果
     */
    @ApiOperation("删除跨区发送请求")
    @PostMapping("/deleteMaterialSpanSend/{ids}")
    public AjaxResult deleteMaterialSpanSend(@PathVariable("ids") Long[] ids) {
        return materialScheduleResultService.deleteMaterialSpanSend(ids);
    }

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    @ApiOperation("根据选中的ids查询跨区发送时要携带的字段")
    @PostMapping("/selectSpanSendNeedFieldByIds/{ids}")
    public List<MaterialScheduleResult> selectSpanSendNeedFieldByIds(@PathVariable("ids") Long[] ids) {
        return materialScheduleResultService.selectSpanSendNeedFieldByIds(ids);
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
}
