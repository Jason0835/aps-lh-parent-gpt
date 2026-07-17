package com.zlt.aps.tm.component;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 胎面排程批次号生成组件。
 *
 * <p>自动排程和模板导入共用同一进程内时钟，避免同一毫秒内生成重复批次号。</p>
 */
@Component
public class TmScheduleBatchNoGenerator {

    /** 进程内最后一次批次号时间戳。 */
    private final AtomicLong lastBatchTimeMillis = new AtomicLong(0L);

    /**
     * 生成胎面排程批次号。
     *
     * @return 批次号，格式为 TMyyyyMMddHHmmssSSS
     */
    public String generate() {
        long currentMillis = System.currentTimeMillis();
        long uniqueMillis = this.lastBatchTimeMillis.updateAndGet(lastMillis ->
                currentMillis > lastMillis ? currentMillis : lastMillis + 1);
        return TmScheduleConstants.AUTO_PLAN_BATCH_NO_PREFIX
                + DateUtil.format(new Date(uniqueMillis), "yyyyMMddHHmmssSSS");
    }
}
