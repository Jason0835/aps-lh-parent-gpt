package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.engine.domain.CxEngineScheduleLimit;

import java.util.List;
import java.util.Map;

/**
  * 成型排产限制信息相关逻辑处理业务接口
  * @ClassName CxEngineScheduleLimitService
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/24 16:00
  * @Version 1.0
**/
public interface CxEngineScheduleLimitService {

    /**
     * 加载所有排产限制设置信息
     * @param
     * @return
     */
    public List<CxEngineScheduleLimit> selectCxEngineScheduleLimitByMachineCodeList();

    /**
     * 获取排产限制设置信息
     * @return
     */
    public Map<String,CxEngineScheduleLimit> getCxScheduleLimitMap();

    /**
     * 带有成型机台编号数据规格限制组装
     * @return
     */
    public Map<String,List<CxEngineScheduleLimit>> getCxScheduleLimitMachineCodeMap();
}
