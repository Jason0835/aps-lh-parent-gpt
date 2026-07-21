package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.dto.TmRollingRecalcRequestDTO;
import com.zlt.aps.tm.api.domain.dto.TmScheduleResultImportDTO;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmInsertTaskRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmScheduleShiftDateVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面排程结果表 Feign接口
 */
@FeignClient(contextId = "ITmScheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmScheduleResultRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmScheduleResult/list")
    TableDataInfo list(@RequestBody TmScheduleResult queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmScheduleResult/save")
    AjaxResult save(TmScheduleResult tmScheduleResult);

    @ApiOperation("删除")
    @DeleteMapping("/tmScheduleResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmScheduleResult/{id}")
    TmScheduleResult getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmScheduleResult/checkUnique")
    String checkUnique(@RequestBody TmScheduleResult tmScheduleResultVO);

    /**
     * 校验胎面自动排程请求。
     *
     * @param request 自动排程请求
     * @return 校验结果
     */
    @ApiOperation("校验胎面自动排程")
    @PostMapping("/tmScheduleResult/validateAutoPlan")
    AjaxResult validateAutoPlan(@RequestBody TmAutoScheduleRequestVo request);

    /**
     * 执行胎面自动排程。
     *
     * @param request 自动排程请求
     * @return 自动排程结果
     */
    @ApiOperation("执行胎面自动排程")
    @PostMapping("/tmScheduleResult/autoPlan")
    AjaxResult autoPlan(@RequestBody TmAutoScheduleRequestVo request);


    /**
     * 查询胎面自动排程任务状态。
     *
     * @param taskId 自动排程任务 ID
     * @return 任务状态和异常明细
     */
    @ApiOperation("查询胎面自动排程任务状态")
    @GetMapping("/tmScheduleResult/autoPlan/task/{taskId}")
    AjaxResult getAutoPlanTask(@PathVariable("taskId") String taskId);

    /**
     * 查询最近胎面自动排程任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务状态
     */
    @ApiOperation("查询最近胎面自动排程任务")
    @GetMapping("/tmScheduleResult/autoPlan/task/latest")
    AjaxResult getLatestAutoPlanTask(@RequestParam("factoryCode") String factoryCode,
                                     @RequestParam("scheduleDate") String scheduleDate);

    /**
     * 查询胎面排程看板。
     *
     * @param queryVO 看板查询条件
     * @return 看板数据
     */
    @ApiOperation("查询胎面排程看板")
    @PostMapping("/tmScheduleResult/board")
    AjaxResult board(@RequestBody TmScheduleResult queryVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmScheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody TmScheduleResult queryVO, @PathVariable("fileName") String fileName);

    /**
     * 按专用模板导出胎面排程结果。
     *
     * @param queryVO 查询条件
     * @param fileName 文件名称
     * @return Excel 文件字节
     */
    @ApiOperation("按专用模板导出胎面排程结果")
    @PostMapping("/tmScheduleResult/exportDataScheduleResult/{fileName}")
    byte[] exportDataScheduleResult(@RequestBody TmScheduleResult queryVO,
                                    @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmScheduleResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 按专用模板导入胎面排程结果。
     *
     * @param importDTO 导入文件和业务条件
     * @param updateSupport 已存在记录是否更新
     * @return 导入结果
     */
    @ApiOperation("按专用模板导入胎面排程结果")
    @PostMapping("/tmScheduleResult/importDataScheduleResult")
    AjaxResult importDataScheduleResult(@RequestBody TmScheduleResultImportDTO importDTO,
                                        @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("转机台")
    @PostMapping("/tmScheduleResult/changeMachine")
    AjaxResult changeMachine(@RequestBody TmScheduleResult scheduleResult);

    /**
     * 在 aps-tm 单个事务中批量转机台。
     *
     * @param machineCode 目标机台编码
     * @param scheduleResultList 待转机的排程结果
     * @return 批量转机结果
     */
    @ApiOperation("批量转机台")
    @PostMapping("/tmScheduleResult/batchChangeMachine/{machineCode}")
    AjaxResult batchChangeMachine(@PathVariable("machineCode") String machineCode,
                                  @RequestBody List<TmScheduleResult> scheduleResultList);

    /**
     * 手动触发胎面自动滚动重算。
     *
     * @param request 工厂、日期和目标班次
     * @return 滚动重算统计
     */
    @ApiOperation("胎面自动滚动重算")
    @PostMapping("/tmScheduleResult/rollingRecalc")
    AjaxResult rollingRecalc(@RequestBody TmRollingRecalcRequestDTO request);

    /**
     * 人工插入排程任务。
     *
     * @param requestVo 插单内容
     * @return 插单结果
     */
    @ApiOperation("人工插单")
    @PostMapping("/tmScheduleResult/insertTask")
    AjaxResult insertTask(@RequestBody TmInsertTaskRequestVo requestVo);

    /**
     * 调整排程计划量。
     *
     * @param scheduleResult 调量内容
     * @return 调量结果
     */
    @ApiOperation("调整计划量")
    @PostMapping("/tmScheduleResult/changeQty")
    AjaxResult changeQty(@RequestBody TmScheduleResult scheduleResult);

    /**
     * 校验页面发布请求。
     *
     * @param ids 排程结果 ID 集合
     * @return 校验结果
     */
    @ApiOperation("校验胎面发布")
    @PostMapping("/tmScheduleResult/publishValidate")
    AjaxResult publishValidate(@RequestBody List<Long> ids);

    /**
     * 将排程结果置为待发布。
     *
     * @param ids 排程结果 ID 集合
     * @return 发布状态变更结果
     */
    @ApiOperation("发布胎面排程")
    @PostMapping("/tmScheduleResult/publish")
    AjaxResult publish(@RequestBody List<Long> ids);

    /**
     * 获取胎面排程班次日期列表
     *
     * @param scheduleResult 排程日期
     * @return 班次日期列表
     */
    @ApiOperation("获取胎面排程班次日期列表")
    @PostMapping("/tmScheduleResult/listScheduleShiftDates")
    List<TmScheduleShiftDateVO> listScheduleShiftDates(@RequestBody TmScheduleResult scheduleResult);

    @ApiOperation("更改发布状态")
    @PostMapping("/tmScheduleResult/changeReleaseStatus")
    AjaxResult changeReleaseStatus(@RequestParam("ids") String ids, @RequestParam("isRelease") String isRelease);
}
