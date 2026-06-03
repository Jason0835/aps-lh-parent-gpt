package com.zlt.aps.tq.service.impl;

import com.zlt.aps.tq.api.domain.entity.TqNewScheduleResult;
import com.zlt.aps.tq.mapper.TqNewScheduleResultMapper;
import com.zlt.aps.tq.service.ITqNewScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import org.springframework.stereotype.Service;

/**
 * 胎圈排程结果Service实现类（新版）
 *
 * @author APS
 */
@Service
public class TqNewScheduleResultServiceImpl extends AbstractDocService<TqNewScheduleResult> implements ITqNewScheduleResultService {

    @Override
    public String getDocTypeCode() {
        return "TQ_NEW_SCHEDULE_RESULT";
    }
}
