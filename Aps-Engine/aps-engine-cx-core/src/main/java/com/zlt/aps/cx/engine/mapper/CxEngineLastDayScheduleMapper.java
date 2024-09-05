package com.zlt.aps.cx.engine.mapper;


import com.zlt.aps.cx.engine.domain.CxMiddleNightFinishQty;

import java.util.List;

/**
  * 成型工序前一天计划增补相关数据库操作文件
  * @ClassName CxEngineLastDayScheduleMapper
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2022/2/8 9:38
  * @Version 1.0
**/
public interface CxEngineLastDayScheduleMapper {

    /**
     * 加载成型工序机台参数信息
     * @param cxMiddleNightFinishQty
     * @return
     */
    public List<CxMiddleNightFinishQty> listCxFinish(CxMiddleNightFinishQty cxMiddleNightFinishQty);
}
