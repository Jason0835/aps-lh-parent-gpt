package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultExplain;
import com.zlt.aps.tc.api.domain.vo.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧排程结果远程服务。
 *
 * <p>BootUI 页面控制层通过本接口经网关调用 aps-tc 后端 {@code TcScheduleResultController}，
 * path 前缀 {@code /tc} 由网关路由到 aps-tc 服务。</p>
 */
@FeignClient(contextId = "ITcScheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE,
        path = "${api.path.tc:/tc}")
public interface ITcScheduleResultRemoteService {

    // ===== 看板与未排查询 =====

    /**
     * 查询胎侧排程平铺看板。
     *
     * @param queryVO 看板查询条件
     * @return 已排分页、日期列、批次、汇总和未排数量
     */
    @ApiOperation("查询胎侧排程看板")
    @PostMapping("/tcScheduleResult/board")
    TcScheduleBoardVo board(@RequestBody TcScheduleBoardQueryVo queryVO);

    /**
     * 分页查询当前有效批次未排任务。
     *
     * @param queryVO 看板查询条件
     * @return 未排任务分页
     */
    @ApiOperation("查询胎侧未排任务")
    @PostMapping("/tcScheduleResult/unplanned/list")
    TcScheduleUnplannedPageVo listUnplanned(@RequestBody TcScheduleBoardQueryVo queryVO);

    /**
     * 懒加载已排结果解释。
     *
     * @param resultId 排程结果 ID
     * @return 解释明细
     */
    @ApiOperation("查询胎侧已排结果解释")
    @GetMapping("/tcScheduleResult/explain/result/{resultId}")
    List<TcScheduleResultExplain> listResultExplain(@PathVariable("resultId") Long resultId);

    /**
     * 懒加载未排任务解释。
     *
     * @param unplannedId 未排任务 ID
     * @return 解释明细
     */
    @ApiOperation("查询胎侧未排任务解释")
    @GetMapping("/tcScheduleResult/explain/unplanned/{unplannedId}")
    List<TcScheduleResultExplain> listUnplannedExplain(@PathVariable("unplannedId") Long unplannedId);

    /**
     * 查询人工插单和普通转机可用选项。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期(yyyy-MM-dd)
     * @return 施工、机台和六班选项
     */
    @ApiOperation("查询胎侧人工排程选项")
    @GetMapping("/tcScheduleResult/manual/options")
    TcManualOptionsVo manualOptions(@RequestParam("factoryCode") String factoryCode,
                                    @RequestParam("scheduleDate") String scheduleDate);

    // ===== 人工排程操作 =====

    /**
     * 执行胎侧人工插单。
     *
     * @param requestVO 插单请求
     * @return 新增结果行数
     */
    @ApiOperation("胎侧人工插单")
    @PostMapping("/tcScheduleResult/insertTask")
    AjaxResult insertTask(@RequestBody TcInsertTaskRequestVo requestVO);

    /**
     * 调整胎侧选中班次计划量。
     *
     * @param requestVO 调量请求
     * @return 受影响行数
     */
    @ApiOperation("调整胎侧班次计划量")
    @PostMapping("/tcScheduleResult/changeQty")
    AjaxResult changeQty(@RequestBody TcChangeQtyRequestVo requestVO);

    /**
     * 原子批量执行胎侧普通转机台。
     *
     * @param requestVO 转机台请求
     * @return 受影响行数
     */
    @ApiOperation("胎侧普通转机台")
    @PostMapping("/tcScheduleResult/changeMachine")
    AjaxResult changeMachine(@RequestBody TcChangeMachineRequestVo requestVO);

    /**
     * 按结果 ID 整行删除胎侧六班排程结果。
     *
     * @param resultIdList 排程结果 ID
     * @return 删除行数
     */
    @ApiOperation("整行删除胎侧排程结果")
    @DeleteMapping("/tcScheduleResult/remove")
    AjaxResult remove(@RequestBody List<Long> resultIdList);

    // ===== 自动排程 =====

    /**
     * 校验自动排程请求及旧结果覆盖条件。
     *
     * @param request 自动排程请求
     * @return 校验响应
     */
    @ApiOperation("校验胎侧自动排程请求")
    @PostMapping("/tcScheduleResult/validateAutoPlan")
    TcAutoScheduleResponseVo validateAutoPlan(@RequestBody TcAutoScheduleRequestVo request);

    /**
     * 提交胎侧自动排程异步任务。
     *
     * @param request 自动排程请求
     * @return 待执行任务响应
     */
    @ApiOperation("提交胎侧自动排程任务")
    @PostMapping("/tcScheduleResult/autoPlan")
    TcAutoScheduleResponseVo autoPlan(@RequestBody TcAutoScheduleRequestVo request);

    /**
     * 查询指定胎侧自动排程任务。
     *
     * @param taskId 对外任务编号
     * @return 任务进度和结果摘要
     */
    @ApiOperation("查询胎侧自动排程任务")
    @GetMapping("/tcScheduleResult/autoPlan/task/{taskId}")
    TcAutoScheduleResponseVo getAutoPlanTask(@PathVariable("taskId") String taskId);

    /**
     * 查询指定工厂和排程日期最近一次任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期(yyyy-MM-dd)
     * @return 最近任务进度和结果摘要
     */
    @ApiOperation("查询最近一次胎侧自动排程任务")
    @GetMapping("/tcScheduleResult/autoPlan/task/latest")
    TcAutoScheduleResponseVo getLatestAutoPlanTask(@RequestParam("factoryCode") String factoryCode,
                                                   @RequestParam("scheduleDate") String scheduleDate);

    /**
     * 清理胎侧自动排程 Redis 基础资料缓存。
     *
     * @param factoryCode 工厂编码，为空时清理全部胎侧自动排程缓存
     * @param scheduleDate 排程日期(yyyy-MM-dd)
     * @return 清理数量
     */
    @ApiOperation("清理胎侧自动排程Redis缓存")
    @PostMapping("/tcScheduleResult/clearAutoPlanRedisCache")
    AjaxResult clearAutoPlanRedisCache(@RequestParam(value = "factoryCode", required = false) String factoryCode,
                                       @RequestParam(value = "scheduleDate", required = false) String scheduleDate);

    // ===== 发布 =====

    /**
     * 校验所选胎侧结果是否允许发布。
     *
     * @param requestVO 发布请求
     * @return 校验结果
     */
    @ApiOperation("校验胎侧排程发布")
    @PostMapping("/tcScheduleResult/release/validate")
    TcReleaseValidateVo validateRelease(@RequestBody TcReleaseRequestVo requestVO);

    /**
     * 创建胎侧排程异步发布任务。
     *
     * @param requestVO 发布请求
     * @return 发布任务
     */
    @ApiOperation("发布胎侧排程结果")
    @PostMapping("/tcScheduleResult/release")
    TcReleaseTaskVo release(@RequestBody TcReleaseRequestVo requestVO);

    /**
     * 查询指定发布任务。
     *
     * @param taskId 发布任务ID
     * @return 发布任务
     */
    @ApiOperation("查询胎侧发布任务")
    @GetMapping("/tcScheduleResult/release/task/{taskId}")
    TcReleaseTaskVo getReleaseTask(@PathVariable("taskId") String taskId);

    /**
     * 查询指定工厂日期最近发布任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期(yyyy-MM-dd)
     * @return 最近发布任务
     */
    @ApiOperation("查询最近胎侧发布任务")
    @GetMapping("/tcScheduleResult/release/task/latest")
    TcReleaseTaskVo getLatestReleaseTask(@RequestParam("factoryCode") String factoryCode,
                                         @RequestParam("scheduleDate") String scheduleDate);

    // ===== 定时任务内部接口 =====

    /**
     * 检查并提交胎侧自动滚动任务。
     *
     * @param request 检查请求
     * @return 检查结果
     */
    @ApiOperation("检查胎侧自动滚动窗口")
    @PostMapping("/tcScheduleResult/internal/checkTimedRolling")
    AjaxResult checkTimedRolling(@RequestBody TcRollingCheckRequestVo request);

    /**
     * 恢复发布超时任务。
     *
     * @return 恢复结果
     */
    @ApiOperation("恢复胎侧发布超时任务")
    @PostMapping("/tcScheduleResult/internal/recoverReleaseTimeout")
    AjaxResult recoverReleaseTimeout();
}
