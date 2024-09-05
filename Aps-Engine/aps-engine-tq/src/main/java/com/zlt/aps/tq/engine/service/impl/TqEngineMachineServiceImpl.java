package com.zlt.aps.tq.engine.service.impl;

import com.zlt.aps.tq.engine.mapper.TqEngineMachineMapper;
import com.zlt.aps.tq.engine.service.TqEngineMachineService;
import com.zlt.aps.tq.engine.vo.TqMouthPlateMachineVo;
import com.zlt.aps.tq.engine.vo.TqSpecifyMachineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TqEngineMachineServiceImpl implements TqEngineMachineService {

    @Resource
    private TqEngineMachineMapper tqEngineMachineMapper;

    /**
     * 获得胎圈代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    public Map<String, String> getSpecifyMachineMap(String jobType) {
        Map<String, String> specifyMachineMap = new HashMap<>();
        List<TqSpecifyMachineVo> list = tqEngineMachineMapper.listTqSpecifyMachine(jobType);  //查询胎圈定点机台信息
        for(TqSpecifyMachineVo specifyMachineVo : list) {
            specifyMachineMap.put(specifyMachineVo.getBeadCode(), specifyMachineVo.getMachineIds());
        }
//        Map<String, String> specifyMachineMap = list.stream().collect(Collectors.toMap(TqSpecifyMachineVo::getBeadCode, TqSpecifyMachineVo::getMachineIds));
        return specifyMachineMap;
    }

    /**
     * 获得口型板代码和定点机台的map
     * @return
     */
    public Map<String, String> getMouthPlateMachineMap() {
        Map<String, String> mouthPlateMachineMap = new HashMap<>();
        List<TqMouthPlateMachineVo> list = tqEngineMachineMapper.listTqMouthPlateMachine();  //查询胎圈口型板信息
        for(TqMouthPlateMachineVo mouthPlateMachineVo : list) {
            mouthPlateMachineMap.put(mouthPlateMachineVo.getMouthPlateCode(), mouthPlateMachineVo.getMachineIds());
        }
//        Map<String, String> specifyMachineMap = list.stream().collect(Collectors.toMap(TqMouthPlateMachineVo::getMouthPlateCode, TqMouthPlateMachineVo::getMachineIds));
        return mouthPlateMachineMap;
    }
}
