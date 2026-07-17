package com.zlt.aps.tc.service.query;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultExplain;
import com.zlt.aps.tc.api.domain.entity.TcScheduleUnplanned;
import com.zlt.aps.tc.api.domain.entity.TcShiftConfig;
import com.zlt.aps.tc.api.domain.vo.TcScheduleBoardQueryVo;
import com.zlt.aps.tc.api.domain.vo.TcScheduleBoardVo;
import com.zlt.aps.tc.mapper.TcScheduleResultExplainMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.mapper.TcScheduleUnplannedMapper;
import com.zlt.aps.tc.mapper.TcShiftConfigMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 胎侧排程看板查询服务测试。
 */
@RunWith(MockitoJUnitRunner.class)
public class TcScheduleBoardQueryServiceTest {

    @Mock
    private TcScheduleResultMapper scheduleResultMapper;

    @Mock
    private TcScheduleUnplannedMapper scheduleUnplannedMapper;

    @Mock
    private TcScheduleResultExplainMapper scheduleResultExplainMapper;

    @Mock
    private TcShiftConfigMapper shiftConfigMapper;

    /**
     * 初始化 Lambda 查询所需的实体元数据。
     */
    @BeforeClass
    public static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), TcScheduleResult.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), TcScheduleUnplanned.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), TcScheduleResultExplain.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), TcShiftConfig.class);
    }

    /**
     * 验证看板一次返回分页结果、班次列、当前批次、汇总和未排数量。
     */
    @Test
    public void queryBoardShouldReturnFlatRowsAndSummary() {
        TcScheduleResult result = new TcScheduleResult();
        result.setId(1L);
        result.setFactoryCode("116");
        result.setBatchNo("TC202607150001");
        result.setScheduleDate(Date.valueOf(LocalDate.of(2026, 7, 15)));
        result.setMachineCode("TC01");
        result.setSidewallCode("SW01");
        result.setClass1PlanQty(new BigDecimal("100"));
        result.setClass1FinishQty(new BigDecimal("20"));
        result.setTaskVersion(2L);
        Page<TcScheduleResult> resultPage = new Page<>(1, 20);
        resultPage.setRecords(Collections.singletonList(result));
        resultPage.setTotal(1L);

        TcShiftConfig shiftConfig = new TcShiftConfig();
        shiftConfig.setScheduleDate(result.getScheduleDate());
        shiftConfig.setShiftOrder(1);
        shiftConfig.setShiftCode("CLASS1");
        shiftConfig.setShiftName("中班");
        shiftConfig.setOpenFlag("1");

        when(this.scheduleResultMapper.selectList(any())).thenReturn(Collections.singletonList(result));
        when(this.scheduleResultMapper.selectPage(any(Page.class), any())).thenReturn(resultPage);
        when(this.shiftConfigMapper.selectList(any())).thenReturn(Collections.singletonList(shiftConfig));
        when(this.scheduleUnplannedMapper.selectCount(any())).thenReturn(3L);

        TcScheduleBoardQueryVo query = new TcScheduleBoardQueryVo();
        query.setFactoryCode("116");
        query.setStartDate(Date.valueOf(LocalDate.of(2026, 7, 15)));
        query.setEndDate(Date.valueOf(LocalDate.of(2026, 7, 17)));
        query.setPageNum(1);
        query.setPageSize(20);

        TcScheduleBoardVo board = this.service().queryBoard(query);

        assertEquals(1L, board.getScheduledPage().getTotal().longValue());
        assertEquals(Long.valueOf(2L), board.getScheduledPage().getRows().get(0).getTaskVersion());
        assertEquals(Long.valueOf(2L), board.getScheduledPage().getRows().get(0).getCurrentTaskVersion());
        assertEquals(1, board.getDateColumns().size());
        assertEquals("TC202607150001", board.getBatchMap().get("2026-07-15"));
        assertEquals(new BigDecimal("100"), board.getSummary().getTotalPlanQty());
        assertEquals(new BigDecimal("20"), board.getSummary().getTotalFinishQty());
        assertEquals(3L, board.getUnplannedCount().longValue());
    }

    /**
     * 验证胎侧筛选无已排行时仍按工厂日期恢复当前批次，并保持汇总为空。
     */
    @Test
    public void queryBoardShouldLoadCurrentBatchIndependentlyFromRowFilters() {
        TcScheduleResult currentBatchRow = new TcScheduleResult();
        currentBatchRow.setFactoryCode("116");
        currentBatchRow.setBatchNo("TC202607150009");
        currentBatchRow.setScheduleDate(Date.valueOf(LocalDate.of(2026, 7, 15)));
        Page<TcScheduleResult> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(Collections.emptyList());
        emptyPage.setTotal(0L);

        when(this.scheduleResultMapper.selectList(any()))
                .thenReturn(Collections.singletonList(currentBatchRow), Collections.emptyList());
        when(this.scheduleResultMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);
        when(this.shiftConfigMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(this.scheduleUnplannedMapper.selectCount(any())).thenReturn(2L);

        TcScheduleBoardQueryVo query = this.validQuery();
        query.setSidewallCode("NO_MATCH");
        TcScheduleBoardVo board = this.service().queryBoard(query);

        assertEquals("TC202607150009", board.getBatchMap().get("2026-07-15"));
        assertEquals(0L, board.getScheduledPage().getTotal().longValue());
        assertEquals(0L, board.getSummary().getResultCount().longValue());
        assertEquals(2L, board.getUnplannedCount().longValue());
    }

    /**
     * 验证仅查看未排任务时不读取已排分页大结果集。
     */
    @Test
    public void queryBoardShouldSkipScheduledPageForUnplannedAssignment() {
        TcScheduleResult currentBatchRow = new TcScheduleResult();
        currentBatchRow.setBatchNo("TC202607150010");
        currentBatchRow.setScheduleDate(Date.valueOf(LocalDate.of(2026, 7, 15)));
        when(this.scheduleResultMapper.selectList(any())).thenReturn(Collections.singletonList(currentBatchRow));
        when(this.shiftConfigMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(this.scheduleUnplannedMapper.selectCount(any())).thenReturn(1L);

        TcScheduleBoardQueryVo query = this.validQuery();
        query.setAssignStatus("UNPLANNED");
        TcScheduleBoardVo board = this.service().queryBoard(query);

        assertEquals(0L, board.getScheduledPage().getTotal().longValue());
        assertEquals(0L, board.getSummary().getResultCount().longValue());
        assertEquals(1L, board.getUnplannedCount().longValue());
        verify(this.scheduleResultMapper, never()).selectPage(any(Page.class), any());
    }

    /**
     * 验证整批任务全部未排时仍能从未排表恢复当前批次。
     */
    @Test
    public void queryBoardShouldRecoverBatchWhenAllTasksAreUnplanned() {
        TcScheduleUnplanned unplanned = new TcScheduleUnplanned();
        unplanned.setFactoryCode("116");
        unplanned.setScheduleDate(Date.valueOf(LocalDate.of(2026, 7, 15)));
        unplanned.setBatchNo("TC202607150011");
        when(this.scheduleResultMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(this.scheduleResultMapper.selectPage(any(Page.class), any())).thenReturn(new Page<>(1, 20));
        when(this.scheduleUnplannedMapper.selectList(any())).thenReturn(Collections.singletonList(unplanned));
        when(this.scheduleUnplannedMapper.selectCount(any())).thenReturn(1L);
        when(this.shiftConfigMapper.selectList(any())).thenReturn(Collections.emptyList());

        TcScheduleBoardVo board = this.service().queryBoard(this.validQuery());

        assertEquals("TC202607150011", board.getBatchMap().get("2026-07-15"));
        assertEquals(1L, board.getUnplannedCount().longValue());
    }

    /**
     * 验证未排解释通过批次号和稳定任务业务键关联。
     */
    @Test
    public void listUnplannedExplainShouldUseTaskBusinessKey() {
        TcScheduleUnplanned unplanned = new TcScheduleUnplanned();
        unplanned.setId(9L);
        unplanned.setBatchNo("TC202607150001");
        unplanned.setTaskBusinessKey("SW01#1");
        TcScheduleResultExplain explain = new TcScheduleResultExplain();
        explain.setTaskBusinessKey("SW01#1");
        when(this.scheduleUnplannedMapper.selectById(9L)).thenReturn(unplanned);
        when(this.scheduleResultExplainMapper.selectList(any())).thenReturn(Collections.singletonList(explain));

        assertEquals("SW01#1", this.service().listUnplannedExplain(9L).get(0).getTaskBusinessKey());
    }

    /**
     * 创建待测看板查询服务。
     *
     * @return 看板查询服务
     */
    private TcScheduleBoardQueryService service() {
        return new TcScheduleBoardQueryService(this.scheduleResultMapper, this.scheduleUnplannedMapper,
                this.scheduleResultExplainMapper, this.shiftConfigMapper);
    }

    /**
     * 构造有效看板查询条件。
     *
     * @return 有效查询条件
     */
    private TcScheduleBoardQueryVo validQuery() {
        TcScheduleBoardQueryVo query = new TcScheduleBoardQueryVo();
        query.setFactoryCode("116");
        query.setStartDate(Date.valueOf(LocalDate.of(2026, 7, 15)));
        query.setEndDate(Date.valueOf(LocalDate.of(2026, 7, 17)));
        query.setPageNum(1);
        query.setPageSize(20);
        return query;
    }
}
