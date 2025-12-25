package com.zlt.aps.tq.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.api.domain.entity.TqDayFinishQty;
import com.zlt.aps.tq.engine.service.TqEngineService;
import com.zlt.aps.tq.entity.TqScheduleResult;
import com.zlt.aps.tq.service.TqScheduleResultService;
import com.zlt.sync.api.service.ISyncDataLogsApiService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * 胎圈排程结果Controller
 *
 * @author chen
 * @date 2021-06-21
 */
@RestController
@RequestMapping("/tq/scheduleResult")
@Api(tags = "胎圈排程结果信息维护接口")
public class TqScheduleResultController extends BaseController {
    @Autowired
    private TqScheduleResultService tqScheduleResultService;
    @Resource
    private TqEngineService tqEngineService;
    @Autowired
    private FactoryService factoryService;
	@Resource
	private ISyncDataLogsApiService syncDataLogsService;

    /**
     * 查询胎圈排程结果列表
     */
    @ApiOperation("查询胎圈排程结果列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TqScheduleResultDto dto) {
//        startPage("a.CREATE_TIME");
        TqScheduleResult scheduleResult = new TqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        scheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<TqScheduleResultDto> list = tqScheduleResultService.selectScheduleResultList(scheduleResult);
        return getDataTable(list);
    }

    /**
     * 获取胎圈排程结果详细信息
     */
    @ApiOperation("获取胎圈排程结果详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TqScheduleResultDto getInfo(@PathVariable("id") Long id) {
        return tqScheduleResultService.selectScheduleResultById(id);
    }

    @PostMapping(value = "/getInfos")
    public List<TqScheduleResultDto> getInfos(@RequestBody TqScheduleResultDto scheduleResult) {
        return tqScheduleResultService.selectByIds(scheduleResult.getIds2());
    }

    /**
     * 修改胎圈排程结果
     */
    @Log(title = "ui.data.column.tq.scheduleResult.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/edit")
    @ApiOperation("修改胎圈排程结果（id为空则插单，id不为空则修改）")
    public AjaxResult edit(@RequestBody TqScheduleResultDto dto) {
        if (dto.getId() == null) {
            int exist = tqScheduleResultService.checkTqCodeExist(dto);
            if (exist == 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.specNotExist"));
            }
        }
        TqScheduleResult scheduleResult = new TqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        if (scheduleResult.getId() != null) {
            int releasingOrTimeoutByIds = tqScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
            if (releasingOrTimeoutByIds > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
            }
        }
        // 根据传入的日期查询是否已有对应排程记录
        Boolean unique = tqScheduleResultService.checkUnique(scheduleResult);
        if (!unique) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        tqScheduleResultService.saveScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 转机台
     */
    @Log(title = "ui.data.column.tq.scheduleResult.modelName", businessType = BusinessType.CHANGE_MACHINE)
    @PostMapping("/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody TqScheduleResultDto dto) {
        TqScheduleResult scheduleResult = new TqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int releasingOrTimeoutByIds = tqScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        // 根据传入的日期查询是否已有对应排程记录
        Boolean unique = tqScheduleResultService.checkUnique(scheduleResult);
        if (!unique) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        tqScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        tqScheduleResultService.saveScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 调量
     */
    @Log(title = "ui.data.column.tq.scheduleResult.modelName", businessType = BusinessType.CHANGE_QTY)
    @PostMapping("/changeQty")
    @ApiOperation("调量")
    public AjaxResult changeQty(@RequestBody TqScheduleResultDto dto) {
        TqScheduleResult scheduleResult = new TqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int releasingOrTimeoutByIds = tqScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setMidPlanQty(scheduleResult.getMidPlanQty() == null ? 0D : scheduleResult.getMidPlanQty());
        scheduleResult.setNightPlanQty(scheduleResult.getNightPlanQty() == null ? 0D : scheduleResult.getNightPlanQty());
        scheduleResult.setDayPlanQty(scheduleResult.getDayPlanQty() == null ? 0D : scheduleResult.getDayPlanQty());
        scheduleResult.setNextMidPlanQty(scheduleResult.getNextMidPlanQty() == null ? 0D : scheduleResult.getNextMidPlanQty());
        tqScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        tqScheduleResultService.saveScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 删除胎圈排程结果
     */
    @Log(title = "ui.data.column.tq.scheduleResult.modelName", businessType = BusinessType.DELETE)
    @PostMapping("/{ids}")
    @ApiOperation("删除胎圈排程结果信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") long[] ids) {
//        int releasingOrTimeoutByIds = tqScheduleResultService.isReleasingOrTimeoutByIds(ids);
//        if (releasingOrTimeoutByIds > 0) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
//        }
        if (tqScheduleResultService.isPublishByIds(ids) != ids.length) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isPublishById"));
        }
        tqScheduleResultService.deleteScheduleResultByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出胎圈排程结果列表
     */
    @Log(title = "ui.data.column.tq.scheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎圈排程结果列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody TqScheduleResultDto dto) {
//        startPage("a.CREATE_TIME");
        TqScheduleResult scheduleResult = new TqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        scheduleResult.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        scheduleResult.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        scheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<TqScheduleResultDto> list = tqScheduleResultService.selectScheduleResultList(scheduleResult);
        return tqScheduleResultService.export(list);
    }

    /**
     * 查询胎圈排程结果列表
     */
    @ApiOperation("查询胎圈排程结果列表")
    @PostMapping("/getList")
    public List<TqScheduleResultDto> getList(@RequestBody TqScheduleResultDto dto) {
        TqScheduleResult scheduleResult = new TqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        return tqScheduleResultService.selectScheduleResultList(scheduleResult);
    }

    /**
     * 查询胎圈排程结果列表
     */
    @Log(title = "ui.data.column.tq.scheduleResult.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody TqScheduleResultDto dto) {
    	// 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
    	if (syncDataLogsService.checkPublishLocking("tq:publish:lock", dto.getIds())) {
    		return AjaxResult.success(); // 如果已经被锁定了，则直接返回
    	}
        TqScheduleResult scheduleResult = new TqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int releasingOrTimeoutByIds = tqScheduleResultService.isReleasingOrTimeoutByIds(Arrays.stream(scheduleResult.getIds()).mapToLong(Long::longValue).toArray());
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        scheduleResult.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));

        // 过滤未发布及发布失败的数据
        List<TqScheduleResultDto> list = tqScheduleResultService.selectScheduleResultList(scheduleResult).stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }
        // 获取机台id为空和多机台的记录
        List<TqScheduleResultDto> collect = list.stream().filter(item -> StringUtil.isEmpty(item.getMachineId()) || item.getMachineId().contains(",")).collect(Collectors.toList());
        if (collect.size() > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
        }
        long[] arr = list.stream().mapToLong(TqScheduleResultDto::getId).toArray();
        // 获取下发接口版本号
        String dataVersion = syncDataLogsService.getDataVersion(ApsConstant.TQ_DEPLOY_SYNC_KEY);
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();
        AjaxResult ajaxResult = null;
        try {
            //ids为空或数组大小为0，发布今日所有未发布的排程记录
            tqScheduleResultService.publish(scheduleResult, arr, dataVersion, factoryCode, companyCode);
            // TODO 调整成itf接口
            //数据同步到中间库后，往 mq中发送消息通知 MES去取数据
//            SyncParamsVO syncParamsVO = new SyncParamsVO();
//            syncParamsVO.setSyncKey(ApsConstant.TQ_DEPLOY_SYNC_KEY);
//            syncParamsVO.setDataVersion(dataVersion);
//            // 请求参数
//            JSONObject params = new JSONObject();
//            params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleResult.getScheduleDate()));
//			params.put("rowCount", arr.length);
//            syncParamsVO.setParams(params);
//            syncParamsVO.setFactoryCode(factoryCode);
//            syncParamsVO.setCompanyCode(companyCode);
//            //往消息队列发送消息
//            tqSyncDataHandle.syncNotice(syncParamsVO);

			// 取回mes的反馈结果
			SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
			String status = logs.getStatus();
			// 更新状态
			tqScheduleResultService.updateRelaseStatus(dataVersion, arr, status);
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
    @Log(title = "ui.data.column.tq.scheduleResult.modelName", businessType = BusinessType.AUTOPLAN)
    @ApiOperation("自动排程")
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody TqScheduleResultDto dto) {
        Date scheduleDate = dto.getScheduleDate();
        tqEngineService.autoTqSchedule(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
        return AjaxResult.success();
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param dto 排程日期
     * @return 是否已经发布
     */
    @ApiOperation("查询排程日期是否已发布")
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody TqScheduleResultDto dto) {
        return tqScheduleResultService.isPublish(dto.getScheduleDate());
    }

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     *
     * @param dto 要校验记录
     * @return 查询到的记录数
     */
    @ApiOperation("根据排程日期、物料编号、机台id校验唯一性")
    @PostMapping("/checkUnique")
    public Boolean checkUnique(@RequestBody TqScheduleResultDto dto) {
        TqScheduleResult scheduleResult = new TqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        return tqScheduleResultService.checkUnique(scheduleResult);
    }

    @Log(title = "ui.data.column.tq.scheduleResult.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎圈排程结果信息")
    public AjaxResult importData(@RequestBody List<TqScheduleResultDto> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate") String scheduleDate) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tqScheduleResultService.importData(list, importLogId, DateUtils.parseDate(scheduleDate));
    }

    /**
     * 选机台
     */
    @Log(title = "ui.data.column.tq.scheduleResult.modelName", businessType = BusinessType.CHOOSE_MACHINE)
    @PostMapping("/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody TqScheduleResultDto schesduleResult) {
        TqScheduleResultDto scheduleResult0 = tqScheduleResultService.selectScheduleResultById(schesduleResult.getId());
        if (compare(schesduleResult.getMachineId(), scheduleResult0.getMachineId())) {
            return AjaxResult.success();
        }
        scheduleResult0.setMachineId(schesduleResult.getMachineId());
        return tqScheduleResultService.chooseMachine(scheduleResult0);
    }

    /**
     * 对比
     */
    public boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleResult 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody TqScheduleResultDto scheduleResult){
        return tqScheduleResultService.isReleasingOrTimeoutByDate(scheduleResult.getScheduleDate());
    }

    /**
     * 更改发布状态
     * @param entity 排程日期
     * @return 结果
     */
    @Log(title = "ui.data.column.tq.scheduleResult.modelName")
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody TqScheduleResultDto entity){
        TqScheduleResult tqScheduleResult = new TqScheduleResult();
        BeanUtils.copyProperties(entity, tqScheduleResult);
        tqScheduleResultService.changeReleaseStatus(tqScheduleResult);
        return AjaxResult.success();
    }

    /**
     * 导入完成量
     * @param list 完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/importFinishQty")
    @ApiOperation("导入完成量")
    public AjaxResult importFinishQty(@RequestBody List<TqDayFinishQty> list, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tqScheduleResultService.importFinishQty(list, importLogId);
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody TqScheduleResultDto scheduleResult) {
        TqScheduleResult result = new TqScheduleResult();
        BeanUtils.copyProperties(scheduleResult, result);
        return tqScheduleResultService.getSummaryVo(result);
    }
}
