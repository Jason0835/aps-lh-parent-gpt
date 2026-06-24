package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.service.ITmPlanBootstrapService;
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
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }
        if (context.getScheduleDate() == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_SCHEDULE_DATE_EMPTY.getDefaultMessage());
        }
        if (StrUtil.isBlank(context.getOperator())) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_OPERATOR_EMPTY.getDefaultMessage());
        }
        if (StrUtil.isBlank(context.getBatchNo())) {
            context.setBatchNo(EngineConstants.TM_BATCH_NO_PREFIX + IdUtil.fastSimpleUUID());
        }
        if (StrUtil.isBlank(context.getTraceId())) {
            context.setTraceId(IdUtil.fastSimpleUUID());
        }
    }
}
