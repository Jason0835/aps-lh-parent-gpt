package com.zlt.aps.tm.engine.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import org.springframework.stereotype.Service;

/**
 * 胎面排程默认初始化步骤服务。
 *
 * <p>负责校验排程基础上下文，并在调用方未传入批次号或追踪号时生成稳定非空值。
 * 当前服务不加载数据库基础资料，避免越过后续业务门面和事务边界。</p>
 */
@Service
public class TmPlanBootstrapService implements ITmPlanBootstrapService {

    @Override
    public void bootstrap(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }
        if (context.getScheduleDate() == null) {
            throw new IllegalArgumentException("胎面排程日期不能为空");
        }
        if (StrUtil.isBlank(context.getOperator())) {
            throw new IllegalArgumentException("胎面排程操作人不能为空");
        }
        if (StrUtil.isBlank(context.getBatchNo())) {
            context.setBatchNo("TM" + IdUtil.fastSimpleUUID());
        }
        if (StrUtil.isBlank(context.getTraceId())) {
            context.setTraceId(IdUtil.fastSimpleUUID());
        }
    }
}
