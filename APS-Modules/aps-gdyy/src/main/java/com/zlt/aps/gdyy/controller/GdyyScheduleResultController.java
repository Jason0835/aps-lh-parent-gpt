package com.zlt.aps.gdyy.controller;

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
import com.zlt.aps.common.engine.service.SyncDataLogsService;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto;
import com.zlt.aps.gdyy.api.domain.entity.GdyyDayFinishQty;
import com.zlt.aps.gdyy.common.handle.GdyySyncDataHandle;
import com.zlt.aps.gdyy.engine.service.GdyyEngineService;
import com.zlt.aps.gdyy.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.service.GdyyScheduleResultService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钢带压延排程结果Controller
 *
 * @author chen
 * @date 2021-07-05
 */
@RestController
@RequestMapping("/gdyy/scheduleResult")
@Api(tags = "钢带压延排程结果信息维护接口")
@Slf4j
public class GdyyScheduleResultController extends BaseController {
    @Autowired
    private GdyyScheduleResultService gdyyScheduleResultService;
    @Autowired
    private GdyyEngineService gdyyEngineService;
    @Resource
    private GdyySyncDataHandle gdyySyncDataHandle;
    @Resource
    private SyncDataLogsService syncDataLogsService;

    /**
     * 查询钢带压延排程结果列表
     */
    @ApiOperation("查询钢带压延排程结果列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GdyyScheduleResultDto dto) {
//        startPage("a.CREATE_TIME desc");
        GdyyScheduleResult scheduleResult = new GdyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        scheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<GdyyScheduleResultDto> list = gdyyScheduleResultService.selectScheduleResultList(scheduleResult);
        return getDataTable(list);
    }

    /**
     * 获取钢带压延排程结果详细信息
     */
    @ApiOperation("获取钢带压延排程结果详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GdyyScheduleResultDto getInfo(@PathVariable("id") Long id) {
        return gdyyScheduleResultService.selectScheduleResultById(id);
    }

    /**
     * 修改钢带压延排程结果
     */
    @Log(title = "ui.data.column.gdyy.scheduleResult.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/edit")
    @ApiOperation("修改钢带压延排程结果（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody GdyyScheduleResultDto dto) {
        GdyyScheduleResult scheduleResult = new GdyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        if (dto.getId() == null) {
            int exist = gdyyScheduleResultService.checkGdyyCodeExist(scheduleResult);
            if (exist == 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.specNotExist"));
            }
        }
        if (scheduleResult.getId() != null) {
            int releasingOrTimeoutByDate = gdyyScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
            if (releasingOrTimeoutByDate > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
            }
        }
        // 插单校验
        // 根据传入的日期查询是否已有对应排程记录
        Boolean unique = gdyyScheduleResultService.checkUnique(scheduleResult);
        if (!unique) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gdyy.scheduleResult.alreadyExist"));
        }
        gdyyScheduleResultService.saveScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 调量
     */
    @Log(title = "ui.data.column.gdyy.scheduleResult.modelName", businessType = BusinessType.CHANGE_QTY)
    @PostMapping("/changeQty")
    @ApiOperation("调量")
    public AjaxResult changeQty(@RequestBody GdyyScheduleResultDto dto) {
        GdyyScheduleResult scheduleResult = new GdyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int releasingOrTimeoutByDate = gdyyScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        gdyyScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        gdyyScheduleResultService.saveScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 转机台
     */
    @Log(title = "ui.data.column.gdyy.scheduleResult.modelName", businessType = BusinessType.CHANGE_MACHINE)
    @PostMapping("/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody GdyyScheduleResultDto dto) {
        GdyyScheduleResult scheduleResult = new GdyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int releasingOrTimeoutByDate = gdyyScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        // 根据传入的日期查询是否已有对应排程记录
        Boolean unique = gdyyScheduleResultService.checkUnique(scheduleResult);
        if (!unique) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        gdyyScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        gdyyScheduleResultService.saveScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 删除钢带压延排程结果
     */
    @Log(title = "ui.data.column.gdyy.scheduleResult.modelName", businessType = BusinessType.DELETE)
    @PostMapping("/{ids}")
    @ApiOperation("删除钢带压延排程结果信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") long[] ids) {
//        int releasingOrTimeoutByDate = gdyyScheduleResultService.isReleasingOrTimeoutByIds(ids);
//        if (releasingOrTimeoutByDate > 0) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
//        }
        if (gdyyScheduleResultService.isPublishByIds(ids) != ids.length) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isPublishById"));
        }
        gdyyScheduleResultService.deleteScheduleResultByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出钢带压延排程结果列表
     */
    @Log(title = "ui.data.column.gdyy.scheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢带压延排程结果列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody GdyyScheduleResultDto dto) {
//        startPage("a.CREATE_TIME desc");
        GdyyScheduleResult scheduleResult = new GdyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        scheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<GdyyScheduleResultDto> list = gdyyScheduleResultService.selectScheduleResultList(scheduleResult);
        return gdyyScheduleResultService.export(list);
    }

    /**
     * 查询钢带压延排程结果列表
     */
    @ApiOperation("查询钢带压延排程结果列表")
    @PostMapping("/getList")
    public List<GdyyScheduleResultDto> getList(@RequestBody GdyyScheduleResultDto dto) {
        GdyyScheduleResult scheduleResult = new GdyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        return gdyyScheduleResultService.selectScheduleResultList(scheduleResult);
    }

    /**
     * 查询钢带压延排程结果列表
     */
    @Log(title = "ui.data.column.gdyy.ScheduleResult.modalName", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody GdyyScheduleResultDto dto) {
        // 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
        if (syncDataLogsService.checkPublishLocking("gdyy:publish:lock", dto.getIds())) {
            return AjaxResult.success(); // 如果已经被锁定了，则直接返回
        }
        GdyyScheduleResult scheduleResult = new GdyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int releasingOrTimeoutByDate = gdyyScheduleResultService.isReleasingOrTimeoutByIds(ArrayUtils.toPrimitive(scheduleResult.getIds()));
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        scheduleResult.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        // 过滤未发布及发布失败的数据
        List<GdyyScheduleResultDto> list = gdyyScheduleResultService.selectScheduleResultList(scheduleResult)
                 .stream().filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }
        // 获取机台id为空和多机台的记录
        List<GdyyScheduleResultDto> collect = list.stream().filter(item -> StringUtil.isEmpty(item.getMachineCode()) || item.getMachineCode().contains(",")).collect(Collectors.toList());
        if (collect.size() > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
        }
        long[] arr = list.stream().mapToLong(GdyyScheduleResultDto::getId).toArray();
        // 获取下发接口版本号
        String dataVersion = gdyySyncDataHandle.getDataVersion(ApsConstant.GDYY_DEPLOY_SYNC_KEY);
        AjaxResult ajaxResult = null;
        try {
            // 发布排程记录
            gdyyScheduleResultService.publish(scheduleResult, arr, dataVersion);

            gdyyScheduleResultService.publishNoticeMes(dto.getScheduleDate(), dataVersion, arr.length);

            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
//            SyncDataLogs logs = new SyncDataLogs();
//            logs.setStatus("2");
//            logs.setMsg("测试");
            String status = logs.getStatus();
            // 更新状态
            gdyyScheduleResultService.updateRelaseStatus(dataVersion, arr, status);
            if (ApsConstant.IS_RELEASE.equals(status)) {
                // 成功
                ajaxResult = AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
            } else {
                // 失败，需要返回异常信息
                ajaxResult = AjaxResult.error(logs.getMsg());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }

        return ajaxResult;
    }

    /**
     * 自动排程
     */
    @Log(title = "ui.data.column.gdyy.scheduleResult.modelName", businessType = BusinessType.AUTOPLAN)
    @ApiOperation("自动排程")
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody GdyyScheduleResultDto dto) {
        // 调用引擎自动排程接口
    	gdyyEngineService.autoGdyySchedule(dto.getScheduleDate());
        return AjaxResult.success("自动排程成功");
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param dto 排程日期
     * @return 是否已经发布
     */
    @ApiOperation("查询排程日期是否已发布")
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody GdyyScheduleResultDto dto) {
        return gdyyScheduleResultService.isPublish(dto.getScheduleDate());
    }

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     *
     * @param dto 要校验记录
     * @return 查询到的记录数
     */
    @ApiOperation("根据排程日期、物料编号、机台id校验唯一性")
    @PostMapping("/checkUnique")
    public Boolean checkUnique(@RequestBody GdyyScheduleResultDto dto) {
        GdyyScheduleResult scheduleResult = new GdyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        return gdyyScheduleResultService.checkUnique(scheduleResult);
    }

    @Log(title = "ui.data.column.gdyy.scheduleResult.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢带压延排程结果信息")
    public AjaxResult importData(@RequestBody List<GdyyScheduleResultDto> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate") String scheduleDate) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gdyyScheduleResultService.importData(list, importLogId, DateUtils.parseDate(scheduleDate));
    }


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param dto 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody GdyyScheduleResultDto dto){
        GdyyScheduleResult scheduleResult = new GdyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        return gdyyScheduleResultService.isReleasingOrTimeoutByDate(scheduleResult.getScheduleDate());
    }

    /**
     * 更改发布状态
     * @param dto 排程日期
     * @return 结果
     */
    @Log(title = "ui.data.column.gdyyScheduleResult.modalName", businessType = BusinessType.BALANCE)
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody GdyyScheduleResultDto dto){
        GdyyScheduleResult scheduleResult = new GdyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        gdyyScheduleResultService.changeReleaseStatus(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 导入钢带压延完成量
     * @param list 完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/importFinishQty")
    @ApiOperation("导入钢带压延完成量")
    public AjaxResult importFinishQty(@RequestBody List<GdyyDayFinishQty> list, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gdyyScheduleResultService.importFinishQty(list, importLogId);
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody GdyyScheduleResultDto scheduleResult) {
        return gdyyScheduleResultService.getSummaryVo(scheduleResult);
    }
}
