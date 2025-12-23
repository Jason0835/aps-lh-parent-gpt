package com.zlt.sync.worker;

import com.ruoyi.common.core.utils.SpringUtils;
import com.zlt.sync.worker.processor.SyncDataReTryeProcessor;

/**
 * 转到具体类执行
 */
public class ProcessorFactory {

    public IProcessor createProcessor(String taskType) {

        IProcessor processor = null;
        switch (taskType) {
            case "SYNC_DATA":
                SyncDataReTryeProcessor kettleProcessor = SpringUtils.getBean(SyncDataReTryeProcessor.class);
                processor = kettleProcessor;
                break;

            default:
                break;
        }

        return processor;
    }
}
