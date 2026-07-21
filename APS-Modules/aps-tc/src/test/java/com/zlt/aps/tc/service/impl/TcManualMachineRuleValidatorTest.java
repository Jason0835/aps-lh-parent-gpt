package com.zlt.aps.tc.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.domain.entity.*;
import com.zlt.aps.tc.mapper.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 胎侧人工普通转机台完整规则校验测试。
 */
@RunWith(MockitoJUnitRunner.class)
public class TcManualMachineRuleValidatorTest {

    @Mock
    private TcMachineInfoMapper machineInfoMapper;

    @Mock
    private TcShiftConfigMapper shiftConfigMapper;

    @Mock
    private TcMachineMaintenanceMapper machineMaintenanceMapper;

    @Mock
    private TcMachineSpeedMapper machineSpeedMapper;

    @Mock
    private TcMouthPlateMapper mouthPlateMapper;

    @Mock
    private TcGlueMachineRealMapper glueMachineRealMapper;

    @Mock
    private TcSpecifyMachineMapper specifyMachineMapper;

    @Mock
    private TcDjSharedMachineMapper djSharedMachineMapper;

    @Mock
    private TcScheduleResultMapper scheduleResultMapper;

    @Mock
    private TcParamsMapper paramsMapper;

    /**
     * 初始化一组允许普通转机的基础资料。
     */
    @Before
    public void setUp() {
        TcMachineInfo machineInfo = new TcMachineInfo();
        machineInfo.setFactoryCode("116");
        machineInfo.setMachineCode("TC02");
        machineInfo.setMachineStatus("1");
        machineInfo.setOpenShiftCode("CLASS1,CLASS2");
        machineInfo.setMaxCapacity(new BigDecimal("5500"));
        when(this.machineInfoMapper.selectList(any())).thenReturn(Collections.singletonList(machineInfo));

        TcShiftConfig shiftConfig = new TcShiftConfig();
        shiftConfig.setFactoryCode("116");
        shiftConfig.setShiftOrder(1);
        shiftConfig.setShiftCode("CLASS1");
        shiftConfig.setShiftHours(8);
        shiftConfig.setOpenFlag("1");
        when(this.shiftConfigMapper.selectList(any())).thenReturn(Collections.singletonList(shiftConfig));

        TcMouthPlate mouthPlate = new TcMouthPlate();
        mouthPlate.setFactoryCode("116");
        mouthPlate.setMouthPlateCode("MP01");
        mouthPlate.setMachineCode("TC02");
        mouthPlate.setPlateStatus("1");
        when(this.mouthPlateMapper.selectList(any())).thenReturn(Collections.singletonList(mouthPlate));
        when(this.glueMachineRealMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(this.specifyMachineMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(this.djSharedMachineMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(this.machineMaintenanceMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(this.machineSpeedMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(this.scheduleResultMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    /**
     * 验证全部资料允许时普通转机校验通过。
     */
    @Test
    public void validateTransferShouldPassAllRules() {
        this.validator().validateTransfer(this.sourceResult(), "TC02", 1);
    }

    /**
     * 验证目标机台不支持口型板时拒绝转机。
     */
    @Test(expected = ServiceException.class)
    public void validateTransferShouldRejectUnsupportedMouthPlate() {
        when(this.mouthPlateMapper.selectList(any())).thenReturn(Collections.emptyList());

        this.validator().validateTransfer(this.sourceResult(), "TC02", 1);
    }

    /**
     * 验证胶料机台规则未允许目标机台时拒绝转机。
     */
    @Test(expected = ServiceException.class)
    public void validateTransferShouldRejectGlueMachineRule() {
        TcGlueMachineReal rule = new TcGlueMachineReal();
        rule.setGlueCode("G01");
        rule.setBaseGlueCode("BG01");
        rule.setMachineCode("TC03");
        rule.setShiftCode("CLASS1");
        rule.setAllowFlag("1");
        rule.setEnableStatus("1");
        when(this.glueMachineRealMapper.selectList(any())).thenReturn(Collections.singletonList(rule));

        this.validator().validateTransfer(this.sourceResult(), "TC02", 1);
    }

    /**
     * 验证其他机台的胶料禁排规则不会被误判为目标机台白名单。
     */
    @Test
    public void validateTransferShouldIgnoreOtherMachineGlueBlacklist() {
        TcGlueMachineReal rule = new TcGlueMachineReal();
        rule.setGlueCode("G01");
        rule.setBaseGlueCode("BG01");
        rule.setMachineCode("TC03");
        rule.setShiftCode("CLASS1");
        rule.setAllowFlag("0");
        rule.setEnableStatus("1");
        when(this.glueMachineRealMapper.selectList(any())).thenReturn(Collections.singletonList(rule));

        this.validator().validateTransfer(this.sourceResult(), "TC02", 1);
    }

    /**
     * 验证定点生产规则未包含目标机台时拒绝转机。
     */
    @Test(expected = ServiceException.class)
    public void validateTransferShouldRejectFixedMachineRule() {
        TcSpecifyMachine rule = new TcSpecifyMachine();
        rule.setSidewallCode("SW01");
        rule.setMachineCode("TC03");
        rule.setJobType("0");
        rule.setEnableStatus("1");
        when(this.specifyMachineMapper.selectList(any())).thenReturn(Collections.singletonList(rule));

        this.validator().validateTransfer(this.sourceResult(), "TC02", 1);
    }

    /**
     * 验证胎侧垫胶共机未配置当前胎侧班次时拒绝转机。
     */
    @Test(expected = ServiceException.class)
    public void validateTransferShouldRejectSharedMachineShift() {
        TcDjSharedMachine rule = new TcDjSharedMachine();
        rule.setMachineCode("TC02");
        rule.setTcShiftCode("CLASS2");
        rule.setEnableStatus("1");
        when(this.djSharedMachineMapper.selectList(any())).thenReturn(Collections.singletonList(rule));

        this.validator().validateTransfer(this.sourceResult(), "TC02", 1);
    }

    /**
     * 验证目标机台当前班次已排量加转入量超过 5500 米时拒绝转机。
     */
    @Test(expected = ServiceException.class)
    public void validateTransferShouldRejectShiftCapacityOverflow() {
        TcScheduleResult assignedResult = this.sourceResult();
        assignedResult.setMachineCode("TC02");
        assignedResult.setClass1PlanQty(new BigDecimal("5000"));
        when(this.scheduleResultMapper.selectList(any())).thenReturn(Collections.singletonList(assignedResult));

        this.validator().validateTransfer(this.sourceResult(), "TC02", 1);
    }

    /**
     * 验证人工转机按生效的 TC_SHIFT_MAX_CAPACITY 参数收紧班产上限。
     */
    @Test(expected = ServiceException.class)
    public void validateTransferShouldUseConfiguredShiftCapacity() {
        TcParams params = new TcParams();
        params.setParamCode("TC_SHIFT_MAX_CAPACITY");
        params.setParamValue("1000");
        params.setEnableStatus("1");
        TcScheduleResult assignedResult = this.sourceResult();
        assignedResult.setMachineCode("TC02");
        assignedResult.setClass1PlanQty(new BigDecimal("900"));
        when(this.paramsMapper.selectList(any())).thenReturn(Collections.singletonList(params));
        when(this.scheduleResultMapper.selectList(any())).thenReturn(Collections.singletonList(assignedResult));

        this.validator().validateTransfer(this.sourceResult(), "TC02", 1);
    }

    /**
     * 验证维修产能扣减支持胎侧通用速度回退。
     */
    @Test(expected = ServiceException.class)
    public void validateTransferShouldUseSidewallCommonSpeedForMaintenance() {
        TcMachineMaintenance maintenance = new TcMachineMaintenance();
        maintenance.setFactoryCode("116");
        maintenance.setMachineCode("TC02");
        maintenance.setStopStartTime(Timestamp.valueOf("2026-07-15 00:00:00"));
        maintenance.setStopEndTime(Timestamp.valueOf("2026-07-15 01:00:00"));
        maintenance.setStopShift("CLASS1");
        TcMachineSpeed commonSpeed = new TcMachineSpeed();
        commonSpeed.setSidewallCode("SW01");
        commonSpeed.setProductSpeed(new BigDecimal("5000"));
        when(this.machineMaintenanceMapper.selectList(any())).thenReturn(Collections.singletonList(maintenance));
        when(this.machineSpeedMapper.selectList(any())).thenReturn(Collections.singletonList(commonSpeed));

        this.validator().validateTransfer(this.sourceResult(), "TC02", 1);
    }

    /**
     * 构造源排程结果。
     *
     * @return 源排程结果
     */
    private TcScheduleResult sourceResult() {
        TcScheduleResult result = new TcScheduleResult();
        result.setFactoryCode("116");
        result.setBatchNo("TC202607150001");
        result.setScheduleDate(Date.valueOf(LocalDate.of(2026, 7, 15)));
        result.setMachineCode("TC01");
        result.setSidewallCode("SW01");
        result.setGlueCode("G01");
        result.setBaseGlueCode("BG01");
        result.setMouthPlateCode("MP01");
        result.setClass1PlanQty(new BigDecimal("1000"));
        return result;
    }

    /**
     * 创建待测规则校验器。
     *
     * @return 人工普通转机规则校验器
     */
    private TcManualMachineRuleValidator validator() {
        return new TcManualMachineRuleValidator(this.machineInfoMapper, this.shiftConfigMapper,
                this.machineMaintenanceMapper, this.machineSpeedMapper, this.mouthPlateMapper,
                this.glueMachineRealMapper, this.specifyMachineMapper, this.djSharedMachineMapper,
                this.scheduleResultMapper, this.paramsMapper);
    }
}
