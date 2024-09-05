package com.zlt.kettle.service.impl;

import com.zlt.kettle.api.domain.JobRecord;
import com.zlt.kettle.api.domain.TransRecord;
import com.zlt.kettle.mapper.KettleBizLogMapper;
import com.zlt.kettle.service.KettleBizLogServcie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class KettleBizLogServcieImpl implements KettleBizLogServcie {

    @Autowired
    KettleBizLogMapper kettleBizLogMapper;
    /**
     * 读取JOB数据记录
     * @param jobRecord
     * @return
     */
    public List<JobRecord> getJobRecordList(JobRecord jobRecord){
        return  kettleBizLogMapper.getJobRecordList(jobRecord);
    }

    /***
     * 读取trans数据记录
     * @param transRecord
     * @return
     */
    public List<TransRecord> getTransRecordList(TransRecord transRecord){
        return  kettleBizLogMapper.getTransRecordList(transRecord);

    }
}
