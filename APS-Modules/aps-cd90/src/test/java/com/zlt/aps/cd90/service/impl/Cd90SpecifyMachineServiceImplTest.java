package com.zlt.aps.cd90.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.cd90.api.domain.entity.Cd90SpecifyMachine;
import com.zlt.aps.cd90.mapper.Cd90SpecifyMachineMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Cd90SpecifyMachineServiceImplTest {

    @Mock
    private Cd90SpecifyMachineMapper cd90SpecifyMachineMapper;

    @InjectMocks
    private Cd90SpecifyMachineServiceImpl service;

    @Test
    void checkUniqueReturnsNotUniqueWhenSameFactoryClothMachineAndJobTypeExist() {
        Cd90SpecifyMachine specifyMachine = new Cd90SpecifyMachine();
        specifyMachine.setId(10L);
        specifyMachine.setFactoryCode("116");
        specifyMachine.setClothCode("C001");
        specifyMachine.setMachineCode("CD90-01");
        specifyMachine.setJobType("1");

        when(cd90SpecifyMachineMapper.selectCount(any())).thenReturn(1L);

        assertEquals(UserConstants.NOT_UNIQUE, service.checkUnique(specifyMachine));
    }

    @Test
    void checkUniqueTreatsBlankJobTypeAsEmptyString() {
        Cd90SpecifyMachine specifyMachine = new Cd90SpecifyMachine();
        specifyMachine.setFactoryCode("116");
        specifyMachine.setClothCode("C002");
        specifyMachine.setMachineCode("CD90-02");
        specifyMachine.setJobType(null);

        when(cd90SpecifyMachineMapper.selectCount(any())).thenReturn(0L);

        assertEquals(UserConstants.UNIQUE, service.checkUnique(specifyMachine));
        assertEquals("", specifyMachine.getJobType());
    }
}
