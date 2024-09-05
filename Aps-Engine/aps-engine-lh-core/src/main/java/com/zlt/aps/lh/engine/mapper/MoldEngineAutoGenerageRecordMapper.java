package com.zlt.aps.lh.engine.mapper;


import com.zlt.aps.lh.engine.domain.MoldEngineAutoGenerageRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模具变动单生成记录Mapper接口
 * 
 * @author Joran.zhang
 * @date 2021-07-28
 */
public interface MoldEngineAutoGenerageRecordMapper 
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
     * 删除模具变动单生成记录
     * 
     * @param id 模具变动单生成记录ID
     * @return 结果
     */
    public int deleteMoldEngineAutoGenerageRecordById(Long id);

    /**
     * 批量删除模具变动单生成记录
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMoldEngineAutoGenerageRecordByIds(Long[] ids);

    /**
     * 根据日期进行生成记录删除
     * @param scheduleDate
     * @return
     */
    public int deleteMoldEngineAutoGenerageRecordByScheduleDate(@Param("scheduleDate") String scheduleDate);
}
