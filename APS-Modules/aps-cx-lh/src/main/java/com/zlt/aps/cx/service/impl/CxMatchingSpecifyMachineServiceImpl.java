package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.constants.CxEngineConstants;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxMatchingSpecifyMachineList;
import com.zlt.aps.cx.mapper.entity.CxMatchingSpecifyMachineListMapper;
import com.zlt.aps.cx.service.CxMatchingSpecifyMachineService;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxMachineInfoVo;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineClsBEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineClsEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineStatusEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineCls;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineClsB;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CxMatchingSpecifyMachineServiceImpl implements CxMatchingSpecifyMachineService {

    @Autowired
    private MdmMoldingMachineEntityMapper entityMapper;
    @Autowired
    private CxMatchingSpecifyMachineListMapper cxSpecifyMachineListMapper;
    @Autowired
    private MdmMoldingMachineClsBEntityMapper mdmMoldingMachineClsBEntityMapper;
    @Autowired
    private MdmMoldingMachineStatusEntityMapper mdmMoldingMachineStatusEntityMapper;
    @Autowired
    private MdmMoldingMachineClsEntityMapper mdmMoldingMachineClsEntityMapper;

    @Override
    public List<CxMatchingSpecifyMachineList> viewList(CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) {
        return cxSpecifyMachineListMapper.viewList(cxMatchingSpecifyMachineList);
    }


    /**
     * title: 获取可用成型机
     * @param scheduleDate 计划日期
     * @return Map<String, CxMachineInfoVo> key 成型机编号 value CxMachineInfoVo
     */
    @Override
    public  Map<String, CxMachineInfoVo> getAvailableMoldingMachine(Date scheduleDate) {
        Map<String, CxMachineInfoVo> result = new HashMap<>();
        // Step 2.1: 获取成型机信息表 MdmMoldingMachine
        QueryWrapper<MdmMoldingMachine> queryMdmMoldingMachineWrapper = new QueryWrapper<>();
        List<MdmMoldingMachine> moldingMachines = entityMapper.selectList(queryMdmMoldingMachineWrapper);

        // Step 2.2: 获取成型机类型表 MdmMoldingMachineCls,依据类型分组
        QueryWrapper<MdmMoldingMachineCls> queryMdmMoldingMachineClsWrapper = new QueryWrapper<>();
        List<MdmMoldingMachineCls> moldingMachineCls = mdmMoldingMachineClsEntityMapper.selectList(queryMdmMoldingMachineClsWrapper);
        Map<Long, List<MdmMoldingMachineCls>> moldingMachineClassCodeMap = moldingMachineCls.stream()
                .collect(Collectors.groupingBy(MdmMoldingMachineCls::getId));

        // Step 2.3: 获取成型机类型子表 MdmMoldingMachineClsB,依据主键ID分组
        QueryWrapper<MdmMoldingMachineClsB> queryMdmMoldingMachineClassWrapper = new QueryWrapper<>();
        List<MdmMoldingMachineClsB> moldingMachineClsB = mdmMoldingMachineClsBEntityMapper.selectList(queryMdmMoldingMachineClassWrapper);
        Map<Long, List<MdmMoldingMachineClsB>> moldingMachineClassItemCodeMap = moldingMachineClsB.stream()
                .collect(Collectors.groupingBy(MdmMoldingMachineClsB::getMoldingMachineClassId));


        // Step 2.6: 构建可用成型机列表
        moldingMachines.forEach(moldingMachine -> {
            CxMachineInfoVo item = new CxMachineInfoVo();
            BeanUtils.copyProperties(moldingMachine, item);

            //拷贝成型机类型
            List<MdmMoldingMachineCls> machineClsList = moldingMachineClassCodeMap.get(item.getMoldingMachineClassId());
            if (machineClsList != null && !machineClsList.isEmpty()) {
                MdmMoldingMachineCls copiedCls = new MdmMoldingMachineCls();
                BeanUtils.copyProperties(machineClsList.get(0), copiedCls);
                item.setMoldingMachineCls(copiedCls);
                item.setMouldMethod(copiedCls.getMouldMethod());
            }

            //拷贝成型机类型子表
            List<MdmMoldingMachineClsB> classBList = moldingMachineClassItemCodeMap.get(item.getMoldingMachineClassId());
            if (classBList != null) {
                List<MdmMoldingMachineClsB> copiedClassBList = new ArrayList<>();
                classBList.forEach(subclass -> {
                    MdmMoldingMachineClsB copiedSubclass = new MdmMoldingMachineClsB();
                    BeanUtils.copyProperties(subclass, copiedSubclass);
                    copiedClassBList.add(copiedSubclass);
                });
                item.setMoldingMachineClassList(copiedClassBList);
            }

            if (CxEngineConstants.MACHINE_STATUS_ENABLE.equals(item.getMachineStatus())) {
                result.put(item.getMoldingMachineCode(), item);
            }
        });
        return result;
    }
}
