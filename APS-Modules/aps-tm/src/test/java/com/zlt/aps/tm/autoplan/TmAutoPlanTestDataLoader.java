package com.zlt.aps.tm.autoplan;

import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * 胎面自动排程 JSON 测试数据加载器。
 *
 * <p>负责从 src/test/resources/tm-auto-plan 读取场景 JSON，并转换成测试场景对象。
 * 日期字段使用 Hutool 解析，兼容 yyyy-MM-dd 和 yyyy-MM-dd HH:mm:ss。</p>
 */
public class TmAutoPlanTestDataLoader {

    private static final String RESOURCE_PREFIX = "tm-auto-plan/";

    private final ObjectMapper objectMapper;

    /**
     * 创建测试数据加载器。
     */
    public TmAutoPlanTestDataLoader() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                String value = parser.getValueAsString();
                return value == null || value.trim().isEmpty() ? null : DateUtil.parse(value.trim());
            }
        });
        this.objectMapper.registerModule(module);
    }

    /**
     * 按文件名读取测试场景。
     *
     * @param fileName JSON 文件名
     * @return 测试场景对象
     */
    public TmAutoPlanScenario load(String fileName) {
        String resourceName = RESOURCE_PREFIX + fileName;
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("未找到胎面自动排程测试场景文件：" + resourceName);
            }
            return objectMapper.readValue(inputStream, TmAutoPlanScenario.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取胎面自动排程测试场景失败：" + resourceName, ex);
        }
    }
}
