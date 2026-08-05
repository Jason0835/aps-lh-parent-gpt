package com.zlt.aps.tq.engine.service.facade;

import com.zlt.aps.tq.engine.domain.manual.TqManualRollingCommandBatch;
import com.zlt.aps.tq.engine.domain.manual.TqManualRollingContext;
import com.zlt.aps.tq.engine.domain.manual.TqManualRollingResult;
import com.zlt.aps.tq.engine.service.impl.TqManualRollingEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 胎圈排程操作门面。
 *
 * <p>统一承接插单、删除、转机台、调量等人工操作，内部委托给纯计算引擎。
 * 事务边界由 aps-tq 业务层控制。</p>
 */
@Service
public class TqScheduleOperationFacade {

    private final TqManualRollingEngineService manualRollingEngineService;

    @Autowired
    public TqScheduleOperationFacade(TqManualRollingEngineService manualRollingEngineService) {
        this.manualRollingEngineService = manualRollingEngineService;
    }

    /**
     * 批量执行人工滚动命令。
     *
     * @param commandBatch 人工操作命令批次
     * @param context      与数据库实体解耦的运行态上下文
     * @return 最终任务链、未排任务及数量变化
     */
    public TqManualRollingResult execute(TqManualRollingCommandBatch commandBatch,
                                         TqManualRollingContext context) {
        return this.manualRollingEngineService.execute(commandBatch, context);
    }
}
