package com.zlt.aps.common.core.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.zlt.aps.common.core.domain.vo.ScheduleAnalysisReportVO;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * 按计划分析报表工厂时区序列化查询时间。
 */
public class FactoryZoneDateSerializer extends JsonSerializer<Date> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ROOT);

    /**
     * 将查询时间按当前报表携带的工厂时区输出为带偏移量的ISO文本。
     *
     * @param value       待序列化的查询时间
     * @param generator   JSON输出生成器
     * @param serializers Jackson序列化上下文
     * @throws IOException JSON写入失败时抛出
     */
    @Override
    public void serialize(Date value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        Object currentValue = generator.getCurrentValue();
        ZoneId zoneId = ZoneOffset.UTC;
        if (currentValue instanceof ScheduleAnalysisReportVO) {
            String configuredZoneId = ((ScheduleAnalysisReportVO) currentValue).getQueriedAtZoneId();
            if (configuredZoneId != null && !configuredZoneId.trim().isEmpty()) {
                zoneId = ZoneId.of(configuredZoneId);
            }
        }
        generator.writeString(DATE_TIME_FORMATTER.format(value.toInstant().atZone(zoneId)));
    }
}
