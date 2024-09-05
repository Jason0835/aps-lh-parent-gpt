package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.CxEngineQuotaSetting;

import java.util.List;
import java.util.Map;

/**
  * 成型定额数据获取
  * @ClassName CxEngineQuotaSettingService
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/29 19:52
  * @Version 1.0
**/
public interface CxEngineQuotaSettingService {

    /**
     * 加载全部成型机台的定额设定信息
     * @return
     */
    public Map<String, List<CxEngineQuotaSetting>> listCxMachineQuotaSettingMap();

    /**
     * 提供根据规格描述进行解析轮胎类型
     * @param specDesc
     * @return
     */
    public String getTireTypeCode(String specDesc);

}
