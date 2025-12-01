package com.zlt.aps.nc.engine.service.impl;

import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.engine.mapper.NcEngineMachineMapper;
import com.zlt.aps.nc.engine.service.NcEngineMachineService;
import com.zlt.aps.nc.engine.vo.NcSpecifyMachineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NcEngineMachineServiceImpl implements NcEngineMachineService {

    @Resource
    private NcEngineMachineMapper ncEngineMachineMapper;

    /**
     * 获得内衬代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    public Map<String, String> getSpecifyMachineMap(String jobType) {
        Map<String, String> specifyMachineMap = new HashMap<>();
        List<NcSpecifyMachineVo> list = ncEngineMachineMapper.listNcSpecifyMachine(jobType);  //查询内衬定点机台信息
        for(NcSpecifyMachineVo specifyMachineVo : list) {
            specifyMachineMap.put(specifyMachineVo.getLiningCode(), specifyMachineVo.getMachineIds());
        }
        return specifyMachineMap;
    }

    /**
     * 获得内衬机台列表
     *
     * @return 结果
     */
    @Override
    public List<NcMachineInfo> listNcMachine() {
        return ncEngineMachineMapper.listNcMachine();
    }
}
