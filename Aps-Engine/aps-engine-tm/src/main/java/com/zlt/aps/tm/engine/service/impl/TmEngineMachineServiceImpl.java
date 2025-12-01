package com.zlt.aps.tm.engine.service.impl;

import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmMachineMaintenance;
import com.zlt.aps.tm.engine.mapper.TmEngineMachineMapper;
import com.zlt.aps.tm.engine.service.TmEngineMachineService;
import com.zlt.aps.tm.engine.vo.TmMouthPlateMachineVo;
import com.zlt.aps.tm.engine.vo.TmSpecifyMachineVo;
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
        List<TmMouthPlateMachineVo> list = tmEngineMachineMapper.listTmMouthPlateMachine(ApsConstant.STATUS_ENABLE);  //查询胎面口型板信息
        for(TmMouthPlateMachineVo mouthPlateMachineVo : list) {
            mouthPlateMachineMap.put(mouthPlateMachineVo.getMouthPlateCode(), mouthPlateMachineVo.getMachineIds());
        }
//        Map<String, String> specifyMachineMap = list.stream().collect(Collectors.toMap(TmMouthPlateMachineVo::getMouthPlateCode, TmMouthPlateMachineVo::getMachineIds));
        return mouthPlateMachineMap;
    }


    /**
     * 获得口型板代码和定点机台的map
     * @return
     */
    public Map<String, String> getDisableMouthPlateMachineMap() {
        Map<String, String> mouthPlateMachineMap = new HashMap<>();
        List<TmMouthPlateMachineVo> list = tmEngineMachineMapper.listTmMouthPlateMachine(ApsConstant.STATUS_DISABLE);  //查询胎面口型板信息
        for(TmMouthPlateMachineVo mouthPlateMachineVo : list) {
            mouthPlateMachineMap.put(mouthPlateMachineVo.getMouthPlateCode(), mouthPlateMachineVo.getMachineIds());
        }
//        Map<String, String> specifyMachineMap = list.stream().collect(Collectors.toMap(TmMouthPlateMachineVo::getMouthPlateCode, TmMouthPlateMachineVo::getMachineIds));
        return mouthPlateMachineMap;
    }

    /**
     * 获得机台信息
     *
     * @return 结果
     */
    @Override
    public List<TmMachineInfo> listTmMachine() {
        return tmEngineMachineMapper.listTmMachine();
    }

    /**
     * 获取机台维修计划需扣减的生产定额
     * K：GenerageMapKeyUtils.createMapKey(机台ID, 停机班次)，V：机台需扣减的生产定额
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public Map<String, BigDecimal> selectMachineSubQuota(String scheduleDate) {
        List<TmMachineMaintenance> machineMaintenanceList = tmEngineMachineMapper.selectMachineMaintenance(scheduleDate);
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
