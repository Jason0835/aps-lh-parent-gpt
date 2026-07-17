package com.zlt.aps.tc.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 胎侧人工调整 JSON 场景加载器。
 */
public class TcManualScheduleJsonScenarioLoader {

    /** 场景资源路径。 */
    private static final String RESOURCE_NAME = "tc-manual-schedule/tc_manual_schedule_rules.json";

    private final ObjectMapper objectMapper;

    /**
     * 创建忽略未知字段的场景加载器。
     */
    public TcManualScheduleJsonScenarioLoader() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 读取全部人工调整场景。
     *
     * @return JSON 中定义的场景列表
     * @throws IllegalArgumentException 场景文件不存在或内容非法时抛出
     */
    public List<TcManualScheduleJsonScenario> loadAll() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(RESOURCE_NAME)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("未找到胎侧人工调整测试场景文件：" + RESOURCE_NAME);
            }
            TcManualScheduleJsonScenario[] scenarioArray = this.objectMapper.readValue(inputStream,
                    TcManualScheduleJsonScenario[].class);
            return Arrays.asList(scenarioArray);
        } catch (IOException exception) {
            throw new IllegalArgumentException("读取胎侧人工调整测试场景失败：" + RESOURCE_NAME, exception);
        }
    }
}
