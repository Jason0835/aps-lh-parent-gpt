package com.zlt.aps.tm.engine.service;

import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import org.springframework.stereotype.Service;

/**
 * 胎面库存预测默认步骤服务。
 *
 * <p>当前只提供可注入、可测试的步骤落点，不实现库存供应时长算法。
 * 后续接入库存数据时应在本服务内补充数据加载和快照记录。</p>
 */
@Service
public class TmInventoryPredictService implements ITmInventoryPredictService {

    @Override
    public void predict(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }
    }
}
