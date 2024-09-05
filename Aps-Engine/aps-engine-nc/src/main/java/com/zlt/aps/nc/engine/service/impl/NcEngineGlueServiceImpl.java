package com.zlt.aps.nc.engine.service.impl;

import com.zlt.aps.nc.engine.mapper.NcEngineGlueMapper;
import com.zlt.aps.nc.engine.service.NcEngineGlueService;
import com.zlt.aps.nc.engine.vo.NcGlueOrderVo;
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
public class NcEngineGlueServiceImpl implements NcEngineGlueService {

    @Resource
    private NcEngineGlueMapper ncEngineGlueMapper;

    /**
     * 获取胶料序号map
     * @return
     */
    public Map<String, String> getGlueSeqMap() {
        Map<String, String> glueSeqMap = new HashMap<>();
        List<NcGlueOrderVo> glueOrderList = ncEngineGlueMapper.listGlueSeq();  //查询胶料顺序序号列表
        for(NcGlueOrderVo glueOrderVo : glueOrderList) {
            glueSeqMap.put(glueOrderVo.getGlueCode(), glueOrderVo.getGlueSeq());
        }
        return glueSeqMap == null ? new HashMap<>() : glueSeqMap;
    }
}
