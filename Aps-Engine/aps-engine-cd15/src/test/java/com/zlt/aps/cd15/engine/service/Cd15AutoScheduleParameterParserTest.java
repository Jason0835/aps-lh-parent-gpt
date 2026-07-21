package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.service.impl.Cd15AutoScheduleParameterParser;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * 斜裁自动排程参数解析测试。
 */
public class Cd15AutoScheduleParameterParserTest {

    private final Cd15AutoScheduleParameterParser parser = new Cd15AutoScheduleParameterParser();

    /** 验证退役参数不再参与强类型参数快照和参数指纹。 */
    @Test
    public void shouldParseParametersByParamCodeWithoutRetiredWindowParameters() {
        List<Cd15Params> params = createValidParams();

        Cd15AutoScheduleParameters result = parser.parse("116", params, 6);

        assertEquals("SUM", result.getDemandCalcMode());
        assertEquals(6, result.getScheduleWindow());
        assertFalse(result.getSourceValues().containsKey("SYS0601013"));
        assertFalse(result.getSourceValues().containsKey("SYS0601015"));
        assertEquals(12, result.getRollTotalCount());
        assertEquals(5, result.getSameRollDiffSpecChangeMinutes());
        assertEquals(7, result.getDiffRollSameSpecChangeMinutes());
        assertEquals(9, result.getDiffRollDiffSpecChangeMinutes());
        assertEquals("CSTB5126", result.getSpecialRollUseUpCodes().get(0));
        assertEquals("CSTA623", result.getSpecialRollUseUpCodes().get(1));
        assertEquals(6, result.getSpecialRollLookaheadShifts());
        assertEquals(new BigDecimal("0"), result.getSpecialRollExtraStockLimit());
        assertEquals(new BigDecimal("2000"), result.getEqualShareThreshold());
        assertEquals(3, result.getPartialMinVehicleCount());
        assertEquals(24, result.getAgingPeriodHours());
        assertEquals(30, result.getTaskTimeoutMinutes());
    }

    /** 验证新增规格回看天数和需求前瞻天数按PARAM_CODE解析。 */
    @Test
    public void shouldParseNewSpecAdvanceParameters() {
        List<Cd15Params> params = createValidParams();

        Cd15AutoScheduleParameters result = parser.parse("116", params, 6);

        assertEquals(10, result.getNewSpecLookbackDays());
        assertEquals(2, result.getNewSpecAdvanceDays());
    }

    /** 验证 SYS0601034 为 0 时允许关闭新增规格提前生产。 */
    @Test
    public void shouldAllowZeroNewSpecLookbackDays() {
        List<Cd15Params> params = createValidParams();
        params.stream()
                .filter(item -> "SYS0601034".equals(item.getParamCode()))
                .findFirst()
                .get()
                .setParamValue("0");

        Cd15AutoScheduleParameters result = parser.parse("116", params, 6);

        assertEquals(0, result.getNewSpecLookbackDays());
        assertEquals(2, result.getNewSpecAdvanceDays());
    }

    /** 验证需求前瞻天数仍必须为正数，避免另一个参数也静默关闭新增规格提前生产。 */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectZeroNewSpecAdvanceDays() {
        List<Cd15Params> params = createValidParams();
        params.stream()
                .filter(item -> "SYS0601035".equals(item.getParamCode()))
                .findFirst()
                .get()
                .setParamValue("0");

        parser.parse("116", params, 6);
    }

    /** 验证输出窗口不能超过当前启用班次数。 */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectScheduleWindowGreaterThanEnabledShiftCount() {
        List<Cd15Params> params = createValidParams();
        params.stream()
                .filter(item -> "SYS0601014".equals(item.getParamCode()))
                .findFirst()
                .get()
                .setParamValue("6");

        parser.parse("116", params, 5);
    }

    /** 验证缺少必填PARAM_CODE时立即终止基础数据校验。 */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingRequiredParamCode() {
        List<Cd15Params> params = createValidParams();
        params.removeIf(item -> "SYS0601022".equals(item.getParamCode()));

        parser.parse("116", params, 6);
    }

    private List<Cd15Params> createValidParams() {
        List<Cd15Params> params = new ArrayList<>();
        params.add(param("SYS0601007", "2000"));
        params.add(param("SYS0601012", "SUM"));
        params.add(param("SYS0601014", "6"));
        params.add(param("SYS0601016", "2"));
        params.add(param("SYS0601017", "300"));
        params.add(param("SYS0601018", "G1301,G1302"));
        params.add(param("SYS0601019", "1"));
        params.add(param("SYS0601020", "3"));
        params.add(param("SYS0601021", "3000"));
        params.add(param("SYS0601022", "12"));
        params.add(param("SYS0601023", "5"));
        params.add(param("SYS0601024", "30"));
        params.add(param("SYS0601025", "0 0 1 * * ?"));
        params.add(param("SYS0601026", "7"));
        params.add(param("SYS0601027", "9"));
        params.add(param("SYS0601028", "CSTB5126,CSTA623"));
        params.add(param("SYS0601029", "6"));
        params.add(param("SYS0601030", "0"));
        params.add(param("SYS0601031", "3"));
        params.add(param("SYS0601032", "24"));
        params.add(param("SYS0601034", "10"));
        params.add(param("SYS0601035", "2"));
        params.add(param("SYS0601011", "87"));
        params.add(param("SYS0601003", "0"));
        return params;
    }

    private Cd15Params param(String paramCode, String paramValue) {
        Cd15Params param = new Cd15Params();
        param.setFactoryCode("116");
        param.setParamCode(paramCode);
        param.setParamValue(paramValue);
        return param;
    }
}
