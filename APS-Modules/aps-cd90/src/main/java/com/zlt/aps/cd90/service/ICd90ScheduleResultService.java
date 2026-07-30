package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.vo.Cd90ChangeQtyRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90RollingCheckRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90ScheduleResultTemplateImportVO;
import com.zlt.aps.cd90.api.domain.vo.Cd90TransferMachineRequest;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

public interface ICd90ScheduleResultService extends IDocService<Cd90ScheduleResult> {

    /**
     * 删除排程结果，不触发滚动重排；删除后只压缩同工厂、日期、机台的 CLASS1 后续生产顺位。
     *
     * @param ids 待删除排程结果主键
     * @return 删除结果
     */
    AjaxResult removeScheduleResults(List<Long> ids);

    /**
     * 按固定生产计划模板整体覆盖导入排程结果。
     *
     * @param rows 导入明细
     * @param condition 工厂和排程日期条件
     * @param updateSupport 是否覆盖参数
     * @return 导入结果
     */
    AjaxResult importScheduleTemplate(List<Cd90ScheduleResultTemplateImportVO> rows,
                                      Cd90ScheduleResult condition,
                                      boolean updateSupport);

    /**
     * 执行直裁自动排程。
     *
     * @param scheduleResult 自动排程条件，当前使用工厂编码和排程日期
     * @return 自动排程结果
     */
    AjaxResult autoSchedule(Cd90ScheduleResult scheduleResult);

    /** 查询插单弹窗使用的启用班次日期。 */
    AjaxResult shiftDates(Cd90InsertOrderRequest request);

    /** 校验插单请求及锁定顺位。 */
    AjaxResult validateInsert(Cd90InsertOrderRequest request);

    /** 创建插单异步任务。 */
    AjaxResult insertOrder(Cd90InsertOrderRequest request);

    /** 查询插单异步任务。 */
    AjaxResult getInsertTask(String taskId);

    /** 校验转机台请求。 */
    AjaxResult validateTransferMachine(Cd90TransferMachineRequest request);

    /** 创建转机台异步任务。 */
    AjaxResult transferMachine(Cd90TransferMachineRequest request);

    /** 查询转机台异步任务。 */
    AjaxResult getTransferMachineTask(String taskId);

    /** 调量预校验。 */
    AjaxResult validateChangeQty(Cd90ChangeQtyRequest request);

    /** 创建调量异步任务。 */
    AjaxResult changeQty(Cd90ChangeQtyRequest request);

    /** 查询调量异步任务。 */
    AjaxResult getChangeQtyTask(String taskId);

    /** 检查交班窗口并按稳定输入创建定时滚动任务。 */
    AjaxResult checkTimedRolling(Cd90RollingCheckRequest request);

    /** 查询定时滚动排程任务。 */
    AjaxResult getTimedRollingTask(String taskId);

    /**
     * 按排程日期 + 工厂编码查询未删除的排程结果。
     *
     * @param scheduleDate 排程日期
     * @param factoryCode 工厂编码
     * @return 排程结果列表
     */
    List<Cd90ScheduleResult> selectByDateAndFactory(Date scheduleDate, String factoryCode);

    /**
     * 按 id 列表批量查询排程结果（含班次字段）。
     *
     * @param ids id 列表
     * @return 排程结果列表
     */
    List<Cd90ScheduleResult> getCd90ScheduleResultListByIds(List<Long> ids);

    /**
     * 批量更新发布状态。独立短事务（REQUIRES_NEW），不受外层 MES 调用异常影响。
     *
     * <p>成功场景：置 IS_RELEASE="1"、publishSuccessCount 累加、newestPublishTime 刷新为当前时间。
     * 失败场景：仅置 isRelease 为传入的目标状态，不累加计数。</p>
     *
     * @param list 待更新的排程结果
     * @param targetStatus 目标状态，取值见 ApsConstant（IS_RELEASE / FAILURE_RELEASE / RELEASING 等）
     * @return 受影响行数
     */
    int batchUpdateReleaseStatus(List<Cd90ScheduleResult> list, String targetStatus);

    /**
     * 使用固定模板导出直裁排程结果。
     *
     * @param currentResults 已按现有导出条件查询的本批排程结果
     * @param queryVO 导出条件，必须包含工厂和排程日期
     * @return Excel文件字节
     */
    byte[] exportData(List<Cd90ScheduleResult> currentResults, Cd90ScheduleResult queryVO);
}
