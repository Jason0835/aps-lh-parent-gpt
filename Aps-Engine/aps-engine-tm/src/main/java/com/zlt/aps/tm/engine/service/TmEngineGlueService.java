package com.zlt.aps.tm.engine.service;

import com.zlt.aps.tm.engine.vo.TmScheduleResultVo;

import java.util.List;
import java.util.Map;

public interface TmEngineGlueService {

    /**
     * 获取胶料序号map
     * @return
     */
    Map<String, String> getGlueSeqMap();
}
