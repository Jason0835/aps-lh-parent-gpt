package com.zlt.aps.cd90.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.mapper.Cd90MachineInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Cd90MachineInfoServiceImplTest {

    @Mock
    private Cd90MachineInfoMapper cd90MachineInfoMapper;

    @InjectMocks
    private Cd90MachineInfoServiceImpl service;

    @Test
    void checkUniqueReturnsNotUniqueWhenSameFactoryMachineCodeExists() {
        Cd90MachineInfo machineInfo = new Cd90MachineInfo();
        machineInfo.setId(10L);
        machineInfo.setFactoryCode("116");
        machineInfo.setMachineCode("CD90-01");

        when(cd90MachineInfoMapper.selectCount(any())).thenReturn(1L);

        assertEquals(UserConstants.NOT_UNIQUE, service.checkUnique(machineInfo));
    }

    @Test
    void checkUniqueReturnsUniqueWhenNoSameFactoryMachineCodeExists() {
        Cd90MachineInfo machineInfo = new Cd90MachineInfo();
        machineInfo.setFactoryCode("116");
        machineInfo.setMachineCode("CD90-02");

        when(cd90MachineInfoMapper.selectCount(any())).thenReturn(0L);

        assertEquals(UserConstants.UNIQUE, service.checkUnique(machineInfo));
    }
}
