package com.zlt.aps.cd90.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

/** 自动排程结果替换范围，为后续插单和调量滚动预留。 */
@Data
@Builder
public class Cd90ScheduleReplaceScope {
    private String factoryCode;
    private LocalDate scheduleDate;
    private String startClassField;
    private String endClassField;
    @Builder.Default
    private Set<String> machineCodes = Collections.emptySet();
}
