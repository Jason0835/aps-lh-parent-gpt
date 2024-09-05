package com.zlt.aps.tc.engine.service.impl;

import com.zlt.aps.tc.engine.mapper.TcEngineGlueMapper;
import com.zlt.aps.tc.engine.service.TcEngineGlueService;
import com.zlt.aps.tc.engine.vo.TcGlueOrderVo;
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
public class TcEngineGlueServiceImpl implements TcEngineGlueService {

    @Resource
    private TcEngineGlueMapper tcEngineGlueMapper;

    /**
     * 获取胶料序号map
     * @return
     */
    public Map<String, String> getGlueSeqMap() {
        Map<String, String> glueSeqMap = new HashMap<>();
        List<TcGlueOrderVo> glueOrderList = tcEngineGlueMapper.listGlueSeq();  //查询胶料顺序序号列表
        for(TcGlueOrderVo glueOrderVo : glueOrderList) {
            glueSeqMap.put(glueOrderVo.getGlueCode(), glueOrderVo.getGlueSeq());
        }
        return glueSeqMap == null ? new HashMap<>() : glueSeqMap;
    }
}
