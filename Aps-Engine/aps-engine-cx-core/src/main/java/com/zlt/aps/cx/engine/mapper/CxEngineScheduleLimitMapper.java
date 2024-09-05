package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineScheduleLimit;

import java.util.List;

/**
  * 成型排产限制设定数据层接口
  * @ClassName CxEngineScheduleLimitMapper
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/25 14:26
  * @Version 1.0
**/
public interface CxEngineScheduleLimitMapper {

    /**
     *  加载成型限制设定
     * @param condition
     * @return
     */
    public List<CxEngineScheduleLimit> selectCxEngineScheduleLimitList(CxEngineScheduleLimit condition);

    /**
     * 加载含有成型机编号排产限制
     * @return
     */
    public List<CxEngineScheduleLimit> selectCxEngineScheduleLimitByMachineCodeList();
}
