package com.zlt.aps.cd15.component;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineConstructionMapper;
import com.zlt.aps.cd15.mapper.Cd15ShiftConfigMapper;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 斜裁排程 MES 班次展开测试。 */
public class Cd15ScheduleResultIssueAssemblerTest {

    @Test
    public void shouldExpandConfiguredShiftAndKeepSplitIdentity() {
        Cd15ScheduleResultIssueAssembler assembler = this.assembler();
        Cd15ScheduleResult source = this.source();
        source.setClass1PlanQty(120D);
        source.setClass1CxPlanQty(110D);
        source.setClass1ProduceOrder(3);

        List<Cd15ScheduleResultIssue> issues = assembler.assemble(
                Collections.singletonList(source), source.getScheduleDate(),
                "116", "TRACE-1");

        assertEquals(1, issues.size());
        Cd15ScheduleResultIssue issue = issues.get(0);
        assertEquals("ORDER-1", issue.getOrderNo());
        assertEquals("GROUP-1", issue.getGroupNo());
        assertEquals("class1", issue.getClassField());
        assertEquals(Double.valueOf(120D), issue.getPlanQty());
        assertEquals(Integer.valueOf(3), issue.getProduceOrder());
        assertEquals(Date.valueOf(LocalDate.of(2026, 7, 17)),
                issue.getScheduleDate());
        assertEquals("BELT-1", issue.getMaterialCode());
        assertEquals("胎胚规格A", issue.getEmbryoSpecDesc());
        assertEquals(new BigDecimal("1.5"), issue.getUnitConsume());
        assertEquals(Double.valueOf(50D), issue.getStockQty());
        assertEquals(Double.valueOf(40D), issue.getCxClass4Plan());
        assertFalse(issue.getClearExistingPlan());
    }

    @Test
    public void shouldIssueZeroForPreviouslyPublishedClearedShift() {
        Cd15ScheduleResultIssueAssembler assembler = this.assembler();
        Cd15ScheduleResult source = this.source();
        source.setPublishSuccessCount(1);
        source.setReleaseStatus(ApsConstant.WAIT_RELEASING);

        List<Cd15ScheduleResultIssue> issues = assembler.assemble(
                Collections.singletonList(source), source.getScheduleDate(),
                "116", "TRACE-2");

        assertEquals(1, issues.size());
        assertEquals(Double.valueOf(0D), issues.get(0).getPlanQty());
        assertTrue(issues.get(0).getClearExistingPlan());
    }

    /** 构造只启用 CLASS1 的装配器。 */
    @SuppressWarnings("unchecked")
    private Cd15ScheduleResultIssueAssembler assembler() {
        Cd15ShiftConfigMapper mapper = mock(Cd15ShiftConfigMapper.class);
        Cd15ShiftConfig config = new Cd15ShiftConfig();
        config.setFactoryCode("116");
        config.setClassField("CLASS1");
        config.setShiftName("中班");
        config.setScheduleDay(1);
        config.setDayShiftOrder(1);
        config.setIsActive(1);
        when(mapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(config));
        Cd15EngineConstructionMapper constructionMapper =
                mock(Cd15EngineConstructionMapper.class);
        MdmConstructionInfo construction = new MdmConstructionInfo();
        construction.setBeltCode1("BELT-1");
        construction.setEmbryoDesc("胎胚规格A");
        when(constructionMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(construction));
        return new Cd15ScheduleResultIssueAssembler(
                mapper, constructionMapper);
    }

    /** 构造分裁来源结果。 */
    private Cd15ScheduleResult source() {
        Cd15ScheduleResult source = new Cd15ScheduleResult();
        source.setId(1L);
        source.setFactoryCode("116");
        source.setScheduleDate(Date.valueOf(LocalDate.of(2026, 7, 18)));
        source.setOrderNo("ORDER-1");
        source.setGroupNo("GROUP-1");
        source.setMachineCode("G1101");
        source.setSteelStripCode("BELT-1");
        source.setBigRollCode("CSS24524");
        source.setStorageLaneCode("A01");
        source.setCuttingAngle("15");
        source.setCutMode("SPLIT");
        source.setUnitConsumeMillimeter(new BigDecimal("1500"));
        source.setStockQty(50D);
        source.setClass1CxPlanQty(10D);
        source.setClass2CxPlanQty(20D);
        source.setClass3CxPlanQty(30D);
        source.setClass4CxPlanQty(40D);
        return source;
    }
}
