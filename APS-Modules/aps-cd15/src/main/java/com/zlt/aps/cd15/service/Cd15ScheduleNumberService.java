package com.zlt.aps.cd15.service;

import com.zlt.aps.common.engine.service.impl.IncrementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** 生成斜裁自动排程批次号和工单号。 */
@Service
@RequiredArgsConstructor
public class Cd15ScheduleNumberService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private final IncrementService incrementService;

    /** 生成CD15加日期加3位序号的批次号。 */
    public String nextBatchNo(LocalDate scheduleDate) {
        return incrementService.getSequence3("CD15" + DATE_FORMAT.format(scheduleDate));
    }

    /** 生成批次号加4位序号的工单号。 */
    public String nextOrderNo(String batchNo) {
        return incrementService.getSequence4(batchNo);
    }
}
