package com.zlt.aps.common.engine.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.engine.domain.BaseCxConstructionInfo;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;

import java.util.List;
import java.util.Map;


/**
 * 投产施工信息Service接口
 * 
 * @author Joran.zhang
 * @date 2021-12-02
 */
public interface EngineProductConstructionInfoService
{
    /**
     * 加载成型定额相关的属性
     * @return
     */
    public Map<String, BaseCxConstructionInfo> loadConstructionInfo();

    /**
     * 加载成型排程时根据胎胚代码组装相应的施工信息
     * @return
     */
    public Map<String,EngineProductConstructionInfo> loadEngineConstructionMap();

    /**
     * 组装定额相关内容
     * @param engineConstructionInfoMap
     * @return
     */
    public Map<String,BaseCxConstructionInfo> changeConstructionInfo(Map<String, EngineProductConstructionInfo> engineConstructionInfoMap);

}
