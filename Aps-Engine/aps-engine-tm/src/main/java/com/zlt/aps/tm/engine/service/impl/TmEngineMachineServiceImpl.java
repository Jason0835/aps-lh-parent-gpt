package com.zlt.aps.tm.engine.service.impl;

import com.zlt.aps.tm.engine.mapper.TmEngineMachineMapper;
import com.zlt.aps.tm.engine.service.TmEngineMachineService;
import com.zlt.aps.tm.engine.vo.TmMouthPlateMachineVo;
import com.zlt.aps.tm.engine.vo.TmSpecifyMachineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TmEngineMachineServiceImpl implements TmEngineMachineService {

    @Resource
    private TmEngineMachineMapper tmEngineMachineMapper;

    /**
     * 获得胎面代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    public Map<String, String> getSpecifyMachineMap(String jobType) {
        Map<String, String> specifyMachineMap = new HashMap<>();
        List<TmSpecifyMachineVo> list = tmEngineMachineMapper.listTmSpecifyMachine(jobType);  //查询胎面定点机台信息
        for(TmSpecifyMachineVo specifyMachineVo : list) {
            specifyMachineMap.put(specifyMachineVo.getTreadCode(), specifyMachineVo.getMachineIds());
        }
//        Map<String, String> specifyMachineMap = list.stream().collect(Collectors.toMap(TmSpecifyMachineVo::getTreadCode, TmSpecifyMachineVo::getMachineIds));
        return specifyMachineMap;
    }

    /**
     * 获得口型板代码和定点机台的map
     * @return
     */
    public Map<String, String> getMouthPlateMachineMap() {
        Map<String, String> mouthPlateMachineMap = new HashMap<>();
        List<TmMouthPlateMachineVo> list = tmEngineMachineMapper.listTmMouthPlateMachine();  //查询胎面口型板信息
        for(TmMouthPlateMachineVo mouthPlateMachineVo : list) {
            mouthPlateMachineMap.put(mouthPlateMachineVo.getMouthPlateCode(), mouthPlateMachineVo.getMachineIds());
        }
//        Map<String, String> specifyMachineMap = list.stream().collect(Collectors.toMap(TmMouthPlateMachineVo::getMouthPlateCode, TmMouthPlateMachineVo::getMachineIds));
        return mouthPlateMachineMap;
    }
}
