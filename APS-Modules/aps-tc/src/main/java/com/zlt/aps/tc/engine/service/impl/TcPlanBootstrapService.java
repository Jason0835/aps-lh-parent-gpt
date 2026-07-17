package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.service.ITcPlanBootstrapService;
import org.springframework.stereotype.Service;

/**
 * 胎侧排程默认初始化步骤服务。
 *
 * <p>负责校验排程基础上下文，并在调用方未传入批次号或追踪号时生成稳定非空值。
 * 当前服务不加载数据库基础资料，避免越过后续业务门面和事务边界。</p>
 */
@Service
public class TcPlanBootstrapService implements ITcPlanBootstrapService {

    @Override
    public void bootstrap(TcScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }
        if (context.getScheduleDate() == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_SCHEDULE_DATE_EMPTY.getDefaultMessage());
        }
        if (StrUtil.isBlank(context.getOperator())) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_OPERATOR_EMPTY.getDefaultMessage());
        }
        if (StrUtil.isBlank(context.getBatchNo())) {
            context.setBatchNo(EngineConstants.TC_BATCH_NO_PREFIX + IdUtil.fastSimpleUUID());
        }
        if (StrUtil.isBlank(context.getTraceId())) {
            context.setTraceId(IdUtil.fastSimpleUUID());
        }
    }
}
