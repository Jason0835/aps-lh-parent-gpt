package com.zlt.aps.tc.autoplan;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 胎侧自动排程 JSON 场景加载器。
 */
public class TcAutoPlanJsonScenarioLoader {

    private static final String RESOURCE_NAME = "tc-auto-plan/tc_auto_plan_rules.json";

    private final ObjectMapper objectMapper;

    /**
     * 创建忽略未知字段的场景加载器。
     */
    public TcAutoPlanJsonScenarioLoader() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 读取全部胎侧自动排程测试场景。
     *
     * @return JSON 中定义的场景列表
     * @throws IllegalArgumentException 场景文件不存在或 JSON 非法时抛出
     */
    public List<TcAutoPlanJsonScenario> loadAll() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(RESOURCE_NAME)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("未找到胎侧自动排程测试场景文件：" + RESOURCE_NAME);
            }
            TcAutoPlanJsonScenario[] scenarios = this.objectMapper.readValue(inputStream,
                    TcAutoPlanJsonScenario[].class);
            return Arrays.asList(scenarios);
        } catch (IOException exception) {
            throw new IllegalArgumentException("读取胎侧自动排程测试场景失败：" + RESOURCE_NAME, exception);
        }
    }

    /**
     * 获取场景对象转换器。
     *
     * @return 测试专用 ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }
}
