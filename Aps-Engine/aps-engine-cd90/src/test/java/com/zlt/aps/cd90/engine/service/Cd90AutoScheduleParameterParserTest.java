package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.service.impl.Cd90AutoScheduleParameterParser;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 直裁自动排程参数解析测试。
 */
public class Cd90AutoScheduleParameterParserTest {

    private final Cd90AutoScheduleParameterParser parser = new Cd90AutoScheduleParameterParser();

    /**
     * 验证自动排程参数必须按PARAM_CODE解析为强类型参数快照。
     */
    @Test
    public void shouldParseParametersByParamCode() {
        List<Cd90Params> params = createValidParams();

        Cd90AutoScheduleParameters result = parser.parse("116", params, 6);

        assertEquals("SUM", result.getDemandCalcMode());
        assertEquals(4, result.getDemandWindow());
        assertEquals(6, result.getScheduleWindow());
        assertEquals(new BigDecimal("2.5"), result.getStockGuaranteeShifts());
        assertEquals(12, result.getRollTotalCount());
        assertEquals(30, result.getTaskTimeoutMinutes());
    }

    /**
     * 验证输出窗口不能超过当前启用班次数。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectScheduleWindowGreaterThanEnabledShiftCount() {
        List<Cd90Params> params = createValidParams();
        params.stream()
                .filter(item -> "SYS0701014".equals(item.getParamCode()))
                .findFirst()
                .get()
                .setParamValue("6");

        parser.parse("116", params, 5);
    }

    /**
     * 验证缺少必填PARAM_CODE时立即终止基础数据校验。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingRequiredParamCode() {
        List<Cd90Params> params = createValidParams();
        params.removeIf(item -> "SYS0701022".equals(item.getParamCode()));

        parser.parse("116", params, 6);
    }

    private List<Cd90Params> createValidParams() {
        List<Cd90Params> params = new ArrayList<>();
        params.add(param("SYS0701012", "SUM"));
        params.add(param("SYS0701013", "4"));
        params.add(param("SYS0701014", "6"));
        params.add(param("SYS0701015", "2.5"));
        params.add(param("SYS0701016", "2"));
        params.add(param("SYS0701017", "300"));
        params.add(param("SYS0701018", "G1301,G1302"));
        params.add(param("SYS0701019", "1"));
        params.add(param("SYS0701020", "3"));
        params.add(param("SYS0701021", "3000"));
        params.add(param("SYS0701022", "12"));
        params.add(param("SYS0701023", "5"));
        params.add(param("SYS0701024", "30"));
        params.add(param("SYS0701025", "0 0 1 * * ?"));
        params.add(param("SYS0701011", "87"));
        return params;
    }

    private Cd90Params param(String paramCode, String paramValue) {
        Cd90Params param = new Cd90Params();
        param.setFactoryCode("116");
        param.setParamCode(paramCode);
        param.setParamValue(paramValue);
        return param;
    }
}
