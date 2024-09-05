package com.zlt.aps.lh.engine.service.impl;

import java.util.List;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.lh.engine.domain.MoldEngineAutoGenerageRecord;
import com.zlt.aps.lh.engine.service.MoldEngineAutoGenerageRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zlt.aps.lh.engine.mapper.MoldEngineAutoGenerageRecordMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * 模具变动单生成记录Service业务层处理
 * 
 * @author Joran.zhang
 * @date 2021-07-28
 */
@Service
public class MoldEngineAutoGenerageRecordServiceImpl implements MoldEngineAutoGenerageRecordService
{
    @Autowired
    private MoldEngineAutoGenerageRecordMapper moldEngineAutoGenerageRecordMapper;

    /**
     * 查询模具变动单生成记录
     * 
     * @param id 模具变动单生成记录ID
     * @return 模具变动单生成记录
     */
    @Override
    public MoldEngineAutoGenerageRecord selectMoldEngineAutoGenerageRecordById(Long id)
    {
        return moldEngineAutoGenerageRecordMapper.selectMoldEngineAutoGenerageRecordById(id);
    }

    /**
     * 查询模具变动单生成记录列表
     * 
     * @param moldEngineAutoGenerageRecord 模具变动单生成记录
     * @return 模具变动单生成记录
     */
    @Override
    public List<MoldEngineAutoGenerageRecord> selectMoldEngineAutoGenerageRecordList(MoldEngineAutoGenerageRecord moldEngineAutoGenerageRecord)
    {
        return moldEngineAutoGenerageRecordMapper.selectMoldEngineAutoGenerageRecordList(moldEngineAutoGenerageRecord);
    }

    /**
     * 新增模具变动单生成记录
     * 
     * @param moldEngineAutoGenerageRecord 模具变动单生成记录
     * @return 结果
     */
    @Override
    public int insertMoldEngineAutoGenerageRecord(MoldEngineAutoGenerageRecord moldEngineAutoGenerageRecord)
    {
        moldEngineAutoGenerageRecord.setBaseVale(null);
        return moldEngineAutoGenerageRecordMapper.insertMoldEngineAutoGenerageRecord(moldEngineAutoGenerageRecord);
    }

    /**
     * 修改模具变动单生成记录
     * 
     * @param moldEngineAutoGenerageRecord 模具变动单生成记录
     * @return 结果
     */
    @Override
    public int updateMoldEngineAutoGenerageRecord(MoldEngineAutoGenerageRecord moldEngineAutoGenerageRecord)
    {
        moldEngineAutoGenerageRecord.setBaseVale(moldEngineAutoGenerageRecord.getId());
        return moldEngineAutoGenerageRecordMapper.updateMoldEngineAutoGenerageRecord(moldEngineAutoGenerageRecord);
    }

    /**
     * 批量删除模具变动单生成记录
     * 
     * @param ids 需要删除的模具变动单生成记录ID
     * @return 结果
     */
    @Override
    public int deleteMoldEngineAutoGenerageRecordByIds(Long[] ids)
    {
        return moldEngineAutoGenerageRecordMapper.deleteMoldEngineAutoGenerageRecordByIds(ids);
    }

    /**
     * 删除模具变动单生成记录信息
     * 
     * @param id 模具变动单生成记录ID
     * @return 结果
     */
    @Override
    public int deleteMoldEngineAutoGenerageRecordById(Long id)
    {
        return moldEngineAutoGenerageRecordMapper.deleteMoldEngineAutoGenerageRecordById(id);
    }

    /**
     * 根据日期进行删除
     * @param scheduleDate
     * @return
     */
    @Override
    public int deleteMoldEngineAutoGenerageRecordByScheduleDate(String scheduleDate) {
        return moldEngineAutoGenerageRecordMapper.deleteMoldEngineAutoGenerageRecordByScheduleDate(scheduleDate);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)//加上这个注解可使日志插入不回滚
    public void reGenerageRecord(String cxBatchNo, String moldBatchNo, String scheduleDate, String status) {
        //删除生成记录
        deleteMoldEngineAutoGenerageRecordByScheduleDate(scheduleDate);
        //重新生成
        createRecord(cxBatchNo,moldBatchNo,scheduleDate,status);
    }

    /**
     * 创建模具变动单抓取记录
     * @param cxBatchNo
     * @param moldBatchNo
     * @param scheduleDate
     * @param status
     */
    private void createRecord(String cxBatchNo, String moldBatchNo, String scheduleDate, String status) {
        MoldEngineAutoGenerageRecord moldEngineAutoGenerageRecord =new MoldEngineAutoGenerageRecord();
        moldEngineAutoGenerageRecord.setCxBatchNo(cxBatchNo);
        moldEngineAutoGenerageRecord.setMoldBatchNo(moldBatchNo);
        moldEngineAutoGenerageRecord.setGenerageDate(DateUtils.parseDate(scheduleDate));
        moldEngineAutoGenerageRecord.setStatus(status);
        moldEngineAutoGenerageRecord.setCreateTime(DateUtils.getNowDate());
        insertMoldEngineAutoGenerageRecord(moldEngineAutoGenerageRecord);
    }

}
