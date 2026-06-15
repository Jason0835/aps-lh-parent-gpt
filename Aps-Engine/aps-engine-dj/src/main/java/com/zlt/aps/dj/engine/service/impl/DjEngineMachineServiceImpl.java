package com.zlt.aps.dj.engine.service.impl;

import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.engine.mapper.DjEngineMachineMapper;
import com.zlt.aps.dj.engine.service.DjEngineMachineService;
import com.zlt.aps.dj.engine.vo.DjSpecifyMachineVo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DjEngineMachineServiceImpl implements DjEngineMachineService {

    @Resource
    private DjEngineMachineMapper djEngineMachineMapper;

    /**
     * 获得垫胶代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    public Map<String, String> getSpecifyMachineMap(String jobType) {
        Map<String, String> specifyMachineMap = new HashMap<>();
        List<DjSpecifyMachineVo> list = djEngineMachineMapper.listDjSpecifyMachine(jobType);  //查询垫胶定点机台信息
        for(DjSpecifyMachineVo specifyMachineVo : list) {
            specifyMachineMap.put(specifyMachineVo.getLiningCode(), specifyMachineVo.getMachineIds());
        }
        return specifyMachineMap;
    }

    /**
     * 获得垫胶机台列表
     *
     * @return 结果
     */
    @Override
    public List<DjMachineInfo> listDjMachine() {
        return djEngineMachineMapper.listDjMachine();
    }
}
