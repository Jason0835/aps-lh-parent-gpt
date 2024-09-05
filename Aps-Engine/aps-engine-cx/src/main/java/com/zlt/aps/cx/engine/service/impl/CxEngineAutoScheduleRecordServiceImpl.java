package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.cx.engine.domain.CxEngineAutoScheduleRecord;
import com.zlt.aps.cx.engine.mapper.CxEngineAutoScheduleRecordMapper;
import com.zlt.aps.cx.engine.service.CxEngineAutoScheduleRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 自动排程抓取记录逻辑层
 */
@Service("cxEngineAutoScheduleRecordService")
public class CxEngineAutoScheduleRecordServiceImpl implements CxEngineAutoScheduleRecordService {
    @Autowired
    private CxEngineAutoScheduleRecordMapper cxEngineAutoScheduleRecordMapper;
    @Override
    public CxEngineAutoScheduleRecord selectCxEngineAutoScheduleRecordById(Long id) {
        return cxEngineAutoScheduleRecordMapper.selectCxEngineAutoScheduleRecordById(id);
    }

    @Override
    public List<CxEngineAutoScheduleRecord> selectCxEngineAutoScheduleRecordList(CxEngineAutoScheduleRecord cxEngineAutoScheduleRecord) {
        return cxEngineAutoScheduleRecordMapper.selectCxEngineAutoScheduleRecordList(cxEngineAutoScheduleRecord);
    }

    @Override
    public int insertCxEngineAutoScheduleRecord(CxEngineAutoScheduleRecord cxEngineAutoScheduleRecord) {
        return cxEngineAutoScheduleRecordMapper.insertCxEngineAutoScheduleRecord(cxEngineAutoScheduleRecord);
    }

    @Override
    public int updateCxEngineAutoScheduleRecord(CxEngineAutoScheduleRecord cxEngineAutoScheduleRecord) {
        return cxEngineAutoScheduleRecordMapper.updateCxEngineAutoScheduleRecord(cxEngineAutoScheduleRecord);
    }

    @Override
    public int deleteCxEngineAutoScheduleRecordById(Long id) {
        return cxEngineAutoScheduleRecordMapper.deleteCxEngineAutoScheduleRecordById(id);
    }

    @Override
    public int deleteCxEngineAutoScheduleRecordByIds(Long[] ids) {
        return cxEngineAutoScheduleRecordMapper.deleteCxEngineAutoScheduleRecordByIds(ids);
    }

    @Override
    public int deleteAutoScheduleRecordByScheduleDate(String scheduleDate) {
        return cxEngineAutoScheduleRecordMapper.deleteAutoScheduleRecordByScheduleDate(scheduleDate);
    }

    /**
     * 生成自动排程抓取记录
     * @param scheduleDate
     * @param monthPlanApsVersion
     * @param cxBatchNo
     * @param status
     * @param remark
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)//加上这个注解可使日志插入不回滚
    public CxEngineAutoScheduleRecord generagAutoScheduleRecord(Date scheduleDate, String monthPlanApsVersion, String cxBatchNo, String status, String remark) {
        CxEngineAutoScheduleRecord record =new CxEngineAutoScheduleRecord();
        record.setMonthPlanApsVersion(monthPlanApsVersion);
        record.setCxBatchNo(cxBatchNo);
        record.setStatus(status);
        record.setScheduleDate(scheduleDate);
        record.setRemark(remark);
        record.setCreateBy(SecurityUtils.getUsername());
        cxEngineAutoScheduleRecordMapper.insertCxEngineAutoScheduleRecord(record);
        return record;
    }

    /**
     * 获取排程抓取成功状态的记录
     * @param scheduleDate
     * @return
     */
    @Override
    public CxEngineAutoScheduleRecord selectAutoScheduleRecordByScheduleDate(String scheduleDate) {
        return cxEngineAutoScheduleRecordMapper.selectAutoScheduleRecordByScheduleDate(scheduleDate);
    }
}
