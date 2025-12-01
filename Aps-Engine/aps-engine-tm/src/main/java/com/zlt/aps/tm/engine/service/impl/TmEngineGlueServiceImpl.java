package com.zlt.aps.tm.engine.service.impl;

import com.zlt.aps.tm.engine.mapper.TmEngineGlueMapper;
import com.zlt.aps.tm.engine.service.TmEngineGlueService;
import com.zlt.aps.tm.engine.vo.TmGlueOrderVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胶料顺序service
 */
@Slf4j
@Service
public class TmEngineGlueServiceImpl implements TmEngineGlueService {

    @Resource
    private TmEngineGlueMapper tmEngineGlueMapper;

    /**
     * 获取胶料序号map
     * @return
     */
    public Map<String, String> getGlueSeqMap() {
        Map<String, String> glueSeqMap = new HashMap<>();
        List<TmGlueOrderVo> glueOrderList = tmEngineGlueMapper.listGlueSeq();  //查询胶料顺序序号列表
        for(TmGlueOrderVo glueOrderVo : glueOrderList) {
            glueSeqMap.put(String.join("|", glueOrderVo.getMachineId(), glueOrderVo.getGlueCode()), glueOrderVo.getGlueSeq());
        }
//        glueSeqMap = glueOrderList.stream().collect(Collectors.toMap(TmGlueOrderVo::getGlueCode, TmGlueOrderVo::getGlueSeq));
        return glueSeqMap == null ? new HashMap<>() : glueSeqMap;
    }
}
