package com.zlt.aps.tc.engine.service.impl;

import com.zlt.aps.tc.engine.mapper.TcEngineMachineMapper;
import com.zlt.aps.tc.engine.service.TcEngineMachineService;
import com.zlt.aps.tc.engine.vo.TcMouthPlateMachineVo;
import com.zlt.aps.tc.engine.vo.TcSpecifyMachineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TcEngineMachineServiceImpl implements TcEngineMachineService {

    @Resource
    private TcEngineMachineMapper tcEngineMachineMapper;

    /**
     * 获得胎侧代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    public Map<String, String> getSpecifyMachineMap(String jobType) {
        Map<String, String> specifyMachineMap = new HashMap<>();
        List<TcSpecifyMachineVo> list = tcEngineMachineMapper.listTcSpecifyMachine(jobType);  //查询胎侧定点机台信息
        for(TcSpecifyMachineVo specifyMachineVo : list) {
            specifyMachineMap.put(specifyMachineVo.getSidewallCode(), specifyMachineVo.getMachineIds());
        }
        return specifyMachineMap;
    }

    /**
     * 获得口型板代码和定点机台的map
     * @return
     */
    public Map<String, String> getMouthPlateMachineMap() {
        Map<String, String> mouthPlateMachineMap = new HashMap<>();
        List<TcMouthPlateMachineVo> list = tcEngineMachineMapper.listTcMouthPlateMachine();  //查询胎侧口型板信息
        for(TcMouthPlateMachineVo mouthPlateMachineVo : list) {
            mouthPlateMachineMap.put(mouthPlateMachineVo.getMouthPlateCode(), mouthPlateMachineVo.getMachineIds());
        }
        return mouthPlateMachineMap;
    }
}
