package com.zlt.aps.lh.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.lh.engine.domain.LhEngineAutoScheduleRecord;
import com.zlt.aps.lh.engine.mapper.LhEngineAutoScheduleRecordMapper;
import com.zlt.aps.lh.engine.service.LhEngineAutoScheduleRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
  * 硫化工序自动排程记录逻辑层
  * @ClassName LhEngineAutoScheduleRecordServiceImpl
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/8/5 13:54
  * @Version 1.0
**/
@Service("lhEngineAutoScheduleRecordService")
@Slf4j
public class LhEngineAutoScheduleRecordServiceImpl implements LhEngineAutoScheduleRecordService
{

    @Autowired
    private LhEngineAutoScheduleRecordMapper lhEngineAutoScheduleRecordMapper;

    @Override
    public LhEngineAutoScheduleRecord selectLhEngineAutoScheduleRecordById(Long id) {
        return lhEngineAutoScheduleRecordMapper.selectLhEngineAutoScheduleRecordById(id);
    }

    @Override
    public List<LhEngineAutoScheduleRecord> selectLhEngineAutoScheduleRecordList(LhEngineAutoScheduleRecord lhEngineAutoScheduleRecord) {
        return lhEngineAutoScheduleRecordMapper.selectLhEngineAutoScheduleRecordList(lhEngineAutoScheduleRecord);
    }

    @Override
    public int insertLhEngineAutoScheduleRecord(LhEngineAutoScheduleRecord lhEngineAutoScheduleRecord) {
        return lhEngineAutoScheduleRecordMapper.insertLhEngineAutoScheduleRecord(lhEngineAutoScheduleRecord);
    }

    @Override
    public int updateLhEngineAutoScheduleRecord(LhEngineAutoScheduleRecord lhEngineAutoScheduleRecord) {
        return lhEngineAutoScheduleRecordMapper.updateLhEngineAutoScheduleRecord(lhEngineAutoScheduleRecord);
    }

    @Override
    public int deleteLhEngineAutoScheduleRecordById(Long id) {
        return lhEngineAutoScheduleRecordMapper.deleteLhEngineAutoScheduleRecordById(id);
    }

    @Override
    public int deleteLhEngineAutoScheduleRecordByIds(Long[] ids) {
        return lhEngineAutoScheduleRecordMapper.deleteLhEngineAutoScheduleRecordByIds(ids);
    }

    @Override
    public int deleteAutoScheduleRecordByScheduleDate(String scheduleDate) {
        return lhEngineAutoScheduleRecordMapper.deleteAutoScheduleRecordByScheduleDate(scheduleDate);
    }

    @Override
    public LhEngineAutoScheduleRecord selectAutoScheduleRecordByScheduleDate(String scheduleDate) {
        return lhEngineAutoScheduleRecordMapper.selectAutoScheduleRecordByScheduleDate(scheduleDate);
    }

    /**
     * 重新生成自动排程抓取记录
     * @param cxBatchNo
     * @param lhBatchNo
     * @param scheduleDate
     * @param status
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)//加上这个注解可使日志插入不回滚
    public void reGenerageRecord(String cxBatchNo, String lhBatchNo, String scheduleDate, String status) {
        //删除原有记录
        deleteAutoScheduleRecordByScheduleDate(scheduleDate);
        createRecord(cxBatchNo,lhBatchNo,scheduleDate,status);
    }

    /**
     * 创建硫化自动排程抓取记录
     * @param cxBatchNo
     * @param lhBatchNo
     * @param scheduleDate
     * @param status
     */
    private void createRecord(String cxBatchNo, String lhBatchNo, String scheduleDate, String status) {
        LhEngineAutoScheduleRecord lhEngineAutoScheduleRecord =new LhEngineAutoScheduleRecord();
        lhEngineAutoScheduleRecord.setCxBatchNo(cxBatchNo);
        lhEngineAutoScheduleRecord.setLhBatchNo(lhBatchNo);
        lhEngineAutoScheduleRecord.setScheduleDate(DateUtils.parseDate(scheduleDate));
        lhEngineAutoScheduleRecord.setStatus(status);
        lhEngineAutoScheduleRecord.setCreateTime(DateUtils.getNowDate());
        insertLhEngineAutoScheduleRecord(lhEngineAutoScheduleRecord);
    }

}
