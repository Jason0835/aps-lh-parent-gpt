package com.zlt.aps.gsq.engine.service.facade;

import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingCommandBatch;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingContext;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingResult;
import com.zlt.aps.gsq.engine.service.impl.GsqManualRollingEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 钢丝圈排程操作门面。
 *
 * <p>统一承接插单、删除、转机台、调量等人工操作，内部委托给纯计算引擎。
 * 事务边界由 aps-gsq 业务层控制。</p>
 */
@Service
public class GsqScheduleOperationFacade {

    private final GsqManualRollingEngineService manualRollingEngineService;

    @Autowired
    public GsqScheduleOperationFacade(GsqManualRollingEngineService manualRollingEngineService) {
        this.manualRollingEngineService = manualRollingEngineService;
    }

    /**
     * 批量执行人工滚动命令。
     *
     * @param commandBatch 人工操作命令批次
     * @param context      与数据库实体解耦的运行态上下文
     * @return 最终任务链、未排任务及数量变化
     */
    public GsqManualRollingResult execute(GsqManualRollingCommandBatch commandBatch,
                                          GsqManualRollingContext context) {
        return this.manualRollingEngineService.execute(commandBatch, context);
    }
}
