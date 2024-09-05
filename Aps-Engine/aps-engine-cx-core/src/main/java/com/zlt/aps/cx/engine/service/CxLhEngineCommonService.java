package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.mapper.CxLhEngineCommonMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * 成型工序抽取公用逻辑
 */
public interface CxLhEngineCommonService {

    /**
     * 成型排程计划进行硫化机自动匹配
     * @param cxEngineScheduleResultList
     */
    public void cxScheduleAutoMachLhMachine(Date suppleDate, List<CxEngineScheduleResult> cxEngineScheduleResultList);
}
