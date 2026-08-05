package com.zlt.aps.gsq.api.domain.vo;

import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 钢丝圈人工异步任务请求快照。
 *
 * <p>对齐胎侧 {@code TcOperationRequestSnapshot}。</p>
 *
 * <p>人工操作（插单/调量/转机台/删除）触发后，将原始请求序列化为该快照对象，
 * 存入 {@code GsqAutoScheduleTask.REQUEST_SNAPSHOT} 字段；
 * 异步执行器再反序列化为本对象执行，保证 {@code @Async} 线程拿到的请求与同步入口一致。</p>
 *
 * <p>因钢丝圈现状接口契约：</p>
 * <ul>
 *   <li>{@code insertTask} 接收 {@link GsqInsertTaskRequestVo}；</li>
 *   <li>{@code batchChangeQty} 接收 {@code List<GsqScheduleResult>}；</li>
 *   <li>{@code batchChangeMachine} 接收 {@code List<GsqScheduleResult>}；</li>
 *   <li>{@code batchDelete} 接收 {@code List<Long>}。</li>
 * </ul>
 *
 * @author APS
 */
@Data
public class GsqOperationRequestSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 插单请求（taskType=MANUAL_INSERT 时使用）。 */
    private GsqInsertTaskRequestVo insertRequest;

    /** 调量请求列表（taskType=MANUAL_CHANGE_QTY 时使用）。 */
    private List<GsqScheduleResult> changeQtyRequestList;

    /** 转机台请求列表（taskType=MANUAL_CHANGE_MACHINE 时使用）。 */
    private List<GsqScheduleResult> changeMachineRequestList;

    /** 删除结果ID列表（taskType=MANUAL_DELETE 时使用）。 */
    private List<Long> resultIdList;

    /** 操作人（Web 安全上下文固化，避免 @Async 线程无法获取）。 */
    private String operator;

    /** 操作原因（写入调度日志 reason 字段）。 */
    private String reason;

    /** 幂等键（避免重复派发同一请求）。 */
    private String idempotencyKey;
}
