package com.zlt.aps.tc.engine.service.impl;

import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcMachineMaintenance;
import com.zlt.aps.tc.engine.mapper.TcEngineMachineMapper;
import com.zlt.aps.tc.engine.service.TcEngineMachineService;
import com.zlt.aps.tc.engine.vo.TcMouthPlateMachineVo;
import com.zlt.aps.tc.engine.vo.TcSpecifyMachineVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<TcMouthPlateMachineVo> list = tcEngineMachineMapper.listTcMouthPlateMachine(ApsConstant.STATUS_ENABLE);  //查询胎侧口型板信息
        for(TcMouthPlateMachineVo mouthPlateMachineVo : list) {
            mouthPlateMachineMap.put(mouthPlateMachineVo.getMouthPlateCode(), mouthPlateMachineVo.getMachineIds());
        }
        return mouthPlateMachineMap;
    }

    /**
     * 获得口型板代码和定点机台的map
     * @return
     */
    public Map<String, String> getDisableMouthPlateMachineMap() {
        Map<String, String> mouthPlateMachineMap = new HashMap<>();
        List<TcMouthPlateMachineVo> list = tcEngineMachineMapper.listTcMouthPlateMachine(ApsConstant.STATUS_DISABLE);  //查询胎侧口型板信息
        for(TcMouthPlateMachineVo mouthPlateMachineVo : list) {
            mouthPlateMachineMap.put(mouthPlateMachineVo.getMouthPlateCode(), mouthPlateMachineVo.getMachineIds());
        }
        return mouthPlateMachineMap;
    }


    /**
     * 获得机台信息
     *
     * @return 机台信息
     */
    @Override
    public List<TcMachineInfo> listTcMachine() {
        return tcEngineMachineMapper.listTcMachine();
    }

    /**
     * 获取机台维修计划需扣减的生产定额
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public Map<String, BigDecimal> selectMachineSubQuota(String scheduleDate) {
        List<TcMachineMaintenance> machineMaintenanceList = tcEngineMachineMapper.selectMachineMaintenance(scheduleDate);
        if (CollectionUtils.isEmpty(machineMaintenanceList)) {
            return Collections.emptyMap();
        }
        return machineMaintenanceList.stream().collect(Collectors
                .toMap(item -> GenerageMapKeyUtils.createMapKey(String.valueOf(item.getMachineId()), item.getStopShift()),
                        item -> {
//                            Long startTime = item.getStopStartTime().getTime();
//                            Long endTime = item.getStopEndTime().getTime();
//                            long stopTime = (endTime - startTime) / 1000 / 60 / 60;
                            BigDecimal stopTime = item.getStopTime();
                            return BigDecimalUtils.div(stopTime, new BigDecimal("12"), 2).multiply(item.getQuota());
                        }, BigDecimalUtils::add));
    }
}
