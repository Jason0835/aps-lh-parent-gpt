package com.zlt.aps.gsq.engine.service.impl;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.mapper.GsqEngineMachineMapper;
import com.zlt.aps.gsq.engine.service.GsqEngineMachineService;
import com.zlt.aps.gsq.engine.vo.GsqSpecifyMachineVo;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GsqEngineMachineServiceImpl implements GsqEngineMachineService {

    @Resource
    private GsqEngineMachineMapper gsqEngineMachineMapper;
    
    /**
     * 加载有效钢丝圈机台
     * @return
     */
    public List<GsqMachineInfo> listGsqMachine() {
        return gsqEngineMachineMapper.listGsqMachine();
    }

    /**
     * 获得钢丝圈代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    public Map<String, String> getSpecifyMachineMap(String jobType) {
        Map<String, String> specifyMachineMap = new HashMap<>();
        List<GsqSpecifyMachineVo> list = gsqEngineMachineMapper.listGsqSpecifyMachine(jobType);  //查询钢丝圈定点机台信息
        for(GsqSpecifyMachineVo specifyMachineVo : list) {
            specifyMachineMap.put(specifyMachineVo.getSteelRingCode(), specifyMachineVo.getMachineIds());
        }
//        Map<String, String> specifyMachineMap = list.stream().collect(Collectors.toMap(GsqSpecifyMachineVo::getSteelRingCode, GsqSpecifyMachineVo::getMachineIds));
        return specifyMachineMap;
    }

    /**
     * 获取上一天规格已排产机台列表
     *
     * @param scheduleDate
     * @return
     */
    @Override
    public Map<String, String> getLastDayPlanMachine(Date scheduleDate) {
        return gsqEngineMachineMapper.listLastDayPlanMachine(scheduleDate).stream()
                .filter(r -> StringUtils.isNotEmpty(r.getMachineIds()) && StringUtils.isNotEmpty(r.getSteelRingCode()))
                .collect(Collectors.toMap(GsqSpecifyMachineVo::getSteelRingCode, GsqSpecifyMachineVo::getMachineIds));
    }
}
