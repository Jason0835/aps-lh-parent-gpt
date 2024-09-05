package com.zlt.aps.gsq.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.util.StringUtil;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.domain.SyncDataLogs;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.common.engine.service.SyncDataLogsService;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import com.zlt.aps.gsq.common.handle.GsqSyncDataHandle;
import com.zlt.aps.gsq.engine.service.GsqEngineService;
import com.zlt.aps.gsq.entity.GsqScheduleResult;
import com.zlt.aps.gsq.service.GsqScheduleResultService;
import com.zlt.sync.povo.SyncParamsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钢丝圈排程结果Controller
 *
 * @author chen
 * @date 2021-06-21
 */
@RestController
@RequestMapping("/gsq/scheduleResult")
@Api(tags = "钢丝圈排程结果信息维护接口")
public class GsqScheduleResultController extends BaseController {
    @Autowired
    private GsqScheduleResultService gsqScheduleResultService;
    @Resource
    private GsqEngineService gsqEngineService;
    @Autowired
    private GsqSyncDataHandle gsqSyncDataHandle;
    @Autowired
    private FactoryService factoryService;
	@Resource
	private SyncDataLogsService syncDataLogsService;

    /**
     * 查询钢丝圈排程结果列表
     */
    @ApiOperation("查询钢丝圈排程结果列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GsqScheduleResultDto dto) {
//        startPage("a.STEEL_TYPE");
        GsqScheduleResult scheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        scheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<GsqScheduleResultDto> list = gsqScheduleResultService.selectScheduleResultList(scheduleResult);
        return getDataTable(list);
    }

    /**
     * 获取钢丝圈排程结果详细信息
     */
    @ApiOperation("获取钢丝圈排程结果详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GsqScheduleResultDto getInfo(@PathVariable("id") Long id) {
        return gsqScheduleResultService.selectScheduleResultById(id);
    }


    @PostMapping(value = "/getInfos")
    public List<GsqScheduleResultDto> getInfos(@RequestBody GsqScheduleResultDto scheduleResult) {
        return gsqScheduleResultService.selectByIds(scheduleResult.getIds2());
    }

    /**
     * 修改钢丝圈排程结果
     */
    @Log(title = "ui.data.column.gsq.scheduleResult.modelName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ApiOperation("修改钢丝圈排程结果")
    public AjaxResult edit(@RequestBody GsqScheduleResultDto dto) {
        GsqScheduleResult scheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        if (scheduleResult.getId() != null) {
            int releasingOrTimeoutByIds = gsqScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
            if (releasingOrTimeoutByIds > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
            }
        }
        gsqScheduleResultService.editScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 插单
     */
    @Log(title = "ui.data.column.gsq.scheduleResult.modelName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ApiOperation("插单")
    public AjaxResult add(@RequestBody GsqScheduleResultDto dto) {
        GsqScheduleResult scheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int exist = gsqScheduleResultService.checkGsqCodeExist(scheduleResult);
        if (exist == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.specNotExist"));
        }
        // 插单校验
        dto.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        dto.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        // 根据传入的日期查询是否已有对应排程记录
        Boolean unique = gsqScheduleResultService.checkUnique(scheduleResult);
        if (!unique) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        List<GsqScheduleResult> scheduleResults = gsqScheduleResultService.selectByScheduleDateAndCode(scheduleResult);
        gsqScheduleResultService.addScheduleResult(scheduleResult);
        gsqScheduleResultService.insetDispatcherLogInsertOrder(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, scheduleResults, scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 调量
     */
    @Log(title = "ui.data.column.gsq.scheduleResult.modelName", businessType = BusinessType.CHANGE_QTY)
    @PostMapping("/changeQty")
    @ApiOperation("调量")
    public AjaxResult changeQty(@RequestBody GsqScheduleResultDto dto) {
        GsqScheduleResult scheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int releasingOrTimeoutByIds = gsqScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setMidPlanQty(scheduleResult.getMidPlanQty() == null ? 0D : scheduleResult.getMidPlanQty());
        scheduleResult.setNightPlanQty(scheduleResult.getNightPlanQty() == null ? 0D : scheduleResult.getNightPlanQty());
        scheduleResult.setDayPlanQty(scheduleResult.getDayPlanQty() == null ? 0D : scheduleResult.getDayPlanQty());
        gsqScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        gsqScheduleResultService.editScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 转机台
     */
    @Log(title = "ui.data.column.gsq.scheduleResult.modelName", businessType = BusinessType.CHANGE_MACHINE)
    @PostMapping("/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody GsqScheduleResultDto dto) {
        GsqScheduleResult scheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int releasingOrTimeoutByIds = gsqScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        // 唯一校验
        dto.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        dto.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        // 根据传入的日期查询是否已有对应排程记录
        Boolean unique = gsqScheduleResultService.checkUnique(scheduleResult);
        if (!unique) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        gsqScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        gsqScheduleResultService.editScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 选机台
     * @param dto 更改后机台信息
     * @return 结果
     */
    @PostMapping("/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody GsqScheduleResultDto dto){
        int releasingOrTimeoutByIds = gsqScheduleResultService.isReleasingOrTimeoutByIds(new long[]{dto.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        // 校验机台字段是否修改，未修改则返回成功
        GsqScheduleResultDto result = gsqScheduleResultService.selectScheduleResultById(dto.getId());
        if (ObjectUtils.compare(result.getMachineId(), dto.getMachineId()) == 0) {
            return AjaxResult.success();
        }
        result.setMachineId(dto.getMachineId());
        GsqScheduleResult gsqScheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(result, gsqScheduleResult);
        if (!gsqScheduleResultService.checkUnique(gsqScheduleResult)){
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        //确认机台后，重新计算耗损率
        this.gsqEngineService.confirmGsqMachine(result);
        gsqScheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(result, gsqScheduleResult);

        gsqScheduleResultService.chooseMachine(gsqScheduleResult);
        return AjaxResult.success();
    }

    /**
     * 删除钢丝圈排程结果
     */
    @Log(title = "ui.data.column.gsq.scheduleResult.modelName", businessType = BusinessType.DELETE)
    @PostMapping("/{ids}")
    @ApiOperation("删除钢丝圈排程结果信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") long[] ids) {
//        int releasingOrTimeoutByIds = gsqScheduleResultService.isReleasingOrTimeoutByIds(ids);
//        if (releasingOrTimeoutByIds > 0) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
//        }
        if (gsqScheduleResultService.isPublishByIds(ids) != ids.length) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isPublishById"));
        }
        gsqScheduleResultService.deleteScheduleResultByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出钢丝圈排程结果列表
     */
    @Log(title = "ui.data.column.gsq.scheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢丝圈排程结果列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody GsqScheduleResultDto dto) {
//        startPage("a.CREATE_TIME");
        GsqScheduleResult scheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        scheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<GsqScheduleResultDto> list = gsqScheduleResultService.selectScheduleResultList(scheduleResult);
        return gsqScheduleResultService.export(list);
    }

    /**
     * 查询钢丝圈排程结果列表
     */
    @ApiOperation("查询钢丝圈排程结果列表")
    @PostMapping("/getList")
    public List<GsqScheduleResultDto> getList(@RequestBody GsqScheduleResultDto dto) {
        GsqScheduleResult scheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        return gsqScheduleResultService.selectScheduleResultList(scheduleResult);
    }

    /**
     * 发布当天未发布的排程结果
     */
    @Log(title = "ui.data.column.gsq.scheduleResult.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody GsqScheduleResultDto dto) {
    	// 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
    	if (syncDataLogsService.checkPublishLocking("gsq:publish:lock", dto.getIds())) {
    		return AjaxResult.success(); // 如果已经被锁定了，则直接返回
    	}
        GsqScheduleResult scheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        Date scheduleDate = scheduleResult.getScheduleDate();
        int releasingOrTimeoutByIds = gsqScheduleResultService.isReleasingOrTimeoutByIds(Arrays.stream(scheduleResult.getIds()).mapToLong(Long::longValue).toArray());
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setYear(DateFormatUtils.format(scheduleDate, "yyyy"));
        scheduleResult.setMonth(DateFormatUtils.format(scheduleDate, "MM"));
        // 查询今日所有排程结果
        // 过滤未发布及发布失败的数据
        List<GsqScheduleResultDto> list = gsqScheduleResultService.selectScheduleResultList(scheduleResult).stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }
        // 获取机台id为空和多机台的记录
        List<GsqScheduleResultDto> collect = list.stream().filter(item -> StringUtil.isEmpty(item.getMachineId()) || item.getMachineId().contains(",")).collect(Collectors.toList());
        if (collect.size() > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
        }
        long[] arr = list.stream().mapToLong(GsqScheduleResultDto::getId).toArray();
        // 获取下发接口版本号
        String dataVersion = gsqSyncDataHandle.getDataVersion(ApsConstant.GSQ_DEPLOY_SYNC_KEY);
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();
        AjaxResult ajaxResult = null;
        try {
            //ids为空或数组大小为0，发布今日所有未发布的排程记录
            gsqScheduleResultService.publish(scheduleResult, arr, dataVersion, factoryCode, companyCode);
            //数据同步到中间库后，往 mq中发送消息通知 MES去取数据
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ApsConstant.GSQ_DEPLOY_SYNC_KEY);
            syncParamsVO.setDataVersion(dataVersion);
            // 请求参数
            JSONObject params = new JSONObject();
            params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate));
			params.put("rowCount", arr.length);
            syncParamsVO.setParams(params);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);
            //往消息队列发送消息
            gsqSyncDataHandle.syncNotice(syncParamsVO);

			// 取回mes的反馈结果
			SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
			String status = logs.getStatus();
			// 更新状态
			gsqScheduleResultService.updateRelaseStatus(dataVersion, arr, status);
			if (ApsConstant.IS_RELEASE.equals(status)) {
				// 成功
				ajaxResult = AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
			} else {
				// 失败，需要返回异常信息
				ajaxResult = AjaxResult.error(logs.getMsg());
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
    @Log(title = "ui.data.column.gsq.scheduleResult.modelName", businessType = BusinessType.AUTOPLAN)
    @ApiOperation("自动排程")
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody GsqScheduleResultDto dto) {
        Date scheduleDate = dto.getScheduleDate();
        gsqEngineService.autoGsqSchedule(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
        return AjaxResult.success();
    }

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @ApiOperation("查询排程日期是否已发布")
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody GsqScheduleResultDto dto){
        return gsqScheduleResultService.isPublish(dto.getScheduleDate());
    }

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     *
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    @ApiOperation("根据排程日期、物料编号、机台id校验唯一性")
    @PostMapping("/checkUnique")
    public Boolean checkUnique(@RequestBody GsqScheduleResultDto dto) {
        GsqScheduleResult scheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        return gsqScheduleResultService.checkUnique(scheduleResult);
    }

    @Log(title = "ui.data.column.gsq.scheduleResult.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢丝圈排程结果信息")
    public AjaxResult importData(@RequestBody List<GsqScheduleResultDto> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate") String scheduleDate) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gsqScheduleResultService.importData(list, importLogId, DateUtils.parseDate(scheduleDate));
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody GsqScheduleResultDto scheduleResult){
        return gsqScheduleResultService.isReleasingOrTimeoutByDate(scheduleResult.getScheduleDate());
    }

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName")
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody GsqScheduleResultDto entity){
        GsqScheduleResult gsqScheduleResult = new GsqScheduleResult();
        BeanUtils.copyProperties(entity, gsqScheduleResult);
        gsqScheduleResultService.changeReleaseStatus(gsqScheduleResult);
        return AjaxResult.success();
    }
}
