package com.zlt.kettle.service;

import com.zlt.kettle.api.domain.JobRecord;
import com.zlt.kettle.api.domain.TransRecord;

import java.util.List;

public interface KettleBizLogServcie {

    /**
     * 读取JOB数据记录
     * @param jobRecord
     * @return
     */
    List<JobRecord> getJobRecordList(JobRecord jobRecord);

    /***
     * 读取trans数据记录
     * @param transRecord
     * @return
     */
    List<TransRecord> getTransRecordList(TransRecord transRecord);
}
