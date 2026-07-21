package com.zlt.aps.tc.autoplan;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 胎侧旧聚合规则场景核对测试。
 *
 * <p>26 个规则用例已经迁入独立 JSON 场景的 {@code ruleCases}。本类和旧聚合 JSON
 * 仅作为迁移核对基线保留，不再参与日常回归。</p>
 */
@Ignore("旧聚合规则场景已迁入独立 JSON 场景，仅保留核对基线")
public class TcAutoPlanJsonScenarioTest {

    /**
     * 保留旧入口，手工取消停用时仍可验证聚合文件中的全部规则用例。
     */
    @Test
    public void shouldPassAllTcAutoPlanJsonScenarios() {
        List<TcAutoPlanJsonScenario> scenarios = new TcAutoPlanJsonScenarioLoader().loadAll();
        assertEquals("旧聚合规则场景数量必须保持 26 个", 26, scenarios.size());
        TcAutoPlanRuleScenarioRunner runner = new TcAutoPlanRuleScenarioRunner();
        scenarios.forEach(runner::executeScenario);
    }
}
