package com.zlt.aps.gsq.service;

import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.vo.GsqInsertTaskRequestVo;

import java.util.List;

/**
 * 钢丝圈人工排程应用服务。
 *
 * <p>对齐胎侧 {@code TcManualScheduleApplicationService}，作为异步执行器与人工操作门面之间的
 * 业务编排层。负责请求校验、模板构建（插单场景的批次号/工单号/施工字段回填）等纯业务逻辑，
 * 再统一委托 {@link GsqManualOperationFacade} 完成锁、行锁、短事务、滚动和审计闭环。</p>
 *
 * <p>该层不直接持有数据库事务或分布式锁，保证职责单一，便于异步线程复用。</p>
 *
 * @author APS
 */
public interface GsqManualScheduleApplicationService {

    /**
     * 人工插单。
     *
     * <p>校验插单请求、回填施工字段（英寸）、生成批次号和工单号，
     * 构建 {@link GsqScheduleResult} 模板后委托门面执行插单滚动。</p>
     *
     * @param vo 插单请求
     * @return 受影响行数
     */
    int insertTask(GsqInsertTaskRequestVo vo);

    /**
     * 人工调量。
     *
     * @param request 调量请求
     * @return 受影响行数
     */
    int changeQty(GsqScheduleResult request);

    /**
     * 批量人工调量。
     *
     * @param requestList 调量请求列表
     * @return 受影响行数
     */
    int changeQtyBatch(List<GsqScheduleResult> requestList);

    /**
     * 批量人工转机台。
     *
     * @param requestList 转机台请求列表
     * @return 受影响行数
     */
    int changeMachine(List<GsqScheduleResult> requestList);

    /**
     * 批量人工删除。
     *
     * @param idList 排程结果 ID 列表
     * @return 删除行数
     */
    int remove(List<Long> idList);
}
