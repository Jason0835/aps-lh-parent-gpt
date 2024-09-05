package com.zlt.aps.lh.engine.service;

import com.zlt.aps.lh.engine.domain.MoldEngineAutoGenerageRecord;

import java.util.List;


/**
 * 模具变动单生成记录Service接口
 * 
 * @author Joran.zhang
 * @date 2021-07-28
 */
public interface MoldEngineAutoGenerageRecordService
{
    /**
     * 查询模具变动单生成记录
     * 
     * @param id 模具变动单生成记录ID
     * @return 模具变动单生成记录
     */
    public MoldEngineAutoGenerageRecord selectMoldEngineAutoGenerageRecordById(Long id);

    /**
     * 查询模具变动单生成记录列表
     * 
     * @param moldEngineAutoGenerageRecord 模具变动单生成记录
     * @return 模具变动单生成记录集合
     */
    public List<MoldEngineAutoGenerageRecord> selectMoldEngineAutoGenerageRecordList(MoldEngineAutoGenerageRecord moldEngineAutoGenerageRecord);

    /**
     * 新增模具变动单生成记录
     * 
     * @param moldEngineAutoGenerageRecord 模具变动单生成记录
     * @return 结果
     */
    public int insertMoldEngineAutoGenerageRecord(MoldEngineAutoGenerageRecord moldEngineAutoGenerageRecord);

    /**
     * 修改模具变动单生成记录
     * 
     * @param moldEngineAutoGenerageRecord 模具变动单生成记录
     * @return 结果
     */
    public int updateMoldEngineAutoGenerageRecord(MoldEngineAutoGenerageRecord moldEngineAutoGenerageRecord);

    /**
     * 批量删除模具变动单生成记录
     * 
     * @param ids 需要删除的模具变动单生成记录ID
     * @return 结果
     */
    public int deleteMoldEngineAutoGenerageRecordByIds(Long[] ids);

    /**
     * 删除模具变动单生成记录信息
     * 
     * @param id 模具变动单生成记录ID
     * @return 结果
     */
    public int deleteMoldEngineAutoGenerageRecordById(Long id);

    /**
     * 根据日期进行生成记录删除
     * @param scheduleDate
     * @return
     */
    public int deleteMoldEngineAutoGenerageRecordByScheduleDate(String scheduleDate);

    /**
     * 重新生成
     * @param cxBatchNo
     * @param moldBatchNo
     * @param scheduleDate
     */
    public void reGenerageRecord(String cxBatchNo, String moldBatchNo, String scheduleDate, String status);

}
