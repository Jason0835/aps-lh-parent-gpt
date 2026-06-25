//package com.zlt.aps.dj.engine.service.impl;
//
//import com.zlt.aps.dj.engine.mapper.DjEngineGlueMapper;
//import com.zlt.aps.dj.engine.service.DjEngineGlueService;
//import com.zlt.aps.dj.engine.vo.DjGlueOrderVo;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 胶料顺序service
// */
//@Slf4j
//@Service
//public class DjEngineGlueServiceImpl implements DjEngineGlueService {
//
//    @Resource
//    private DjEngineGlueMapper djEngineGlueMapper;
//
//    /**
//     * 获取胶料序号map
//     * @return
//     */
//    public Map<String, String> getGlueSeqMap() {
//        Map<String, String> glueSeqMap = new HashMap<>();
//        List<DjGlueOrderVo> glueOrderList = djEngineGlueMapper.listGlueSeq();  //查询胶料顺序序号列表
//        for(DjGlueOrderVo glueOrderVo : glueOrderList) {
//            glueSeqMap.put(glueOrderVo.getGlueCode(), glueOrderVo.getGlueSeq());
//        }
//        return glueSeqMap == null ? new HashMap<>() : glueSeqMap;
//    }
//}
