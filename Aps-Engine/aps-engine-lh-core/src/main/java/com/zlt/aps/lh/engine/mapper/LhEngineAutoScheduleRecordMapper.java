package com.zlt.aps.lh.engine.mapper;

import com.zlt.aps.lh.engine.domain.LhEngineAutoScheduleRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 硫化自动排程记录Mapper接口
 * 
 * @author Joran.zhang
 * @date 2021-07-14
 */
public interface LhEngineAutoScheduleRecordMapper
{
    /**
     * 查询硫化自动排程记录
     * 
     * @param id 硫化自动排程记录ID
     * @return 硫化自动排程记录
     */
    public LhEngineAutoScheduleRecord selectLhEngineAutoScheduleRecordById(Long id);

    /**
     * 查询硫化自动排程记录列表
     * 
     * @param lhEngineAutoScheduleRecord 硫化自动排程记录
     * @return 硫化自动排程记录集合
     */
    public List<LhEngineAutoScheduleRecord> selectLhEngineAutoScheduleRecordList(LhEngineAutoScheduleRecord lhEngineAutoScheduleRecord);

    /**
     * 新增硫化自动排程记录
     * 
     * @param lhEngineAutoScheduleRecord 硫化自动排程记录
     * @return 结果
     */
    public int insertLhEngineAutoScheduleRecord(LhEngineAutoScheduleRecord lhEngineAutoScheduleRecord);

    /**
     * 修改硫化自动排程记录
     * 
     * @param lhEngineAutoScheduleRecord 硫化自动排程记录
     * @return 结果
     */
    public int updateLhEngineAutoScheduleRecord(LhEngineAutoScheduleRecord lhEngineAutoScheduleRecord);

    /**
     * 删除硫化自动排程记录
     * 
     * @param id 硫化自动排程记录ID
     * @return 结果
     */
    public int deleteLhEngineAutoScheduleRecordById(Long id);

    /**
     * 批量删除硫化自动排程记录
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLhEngineAutoScheduleRecordByIds(Long[] ids);

    /**
     * 删除硫化自动排程记录
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int deleteAutoScheduleRecordByScheduleDate(@Param("scheduleDate") String scheduleDate);

    /**
     * 根据排程日期获取排程日期对应的抓取记录
     * @param scheduleDate
     * @return
     */
    public LhEngineAutoScheduleRecord selectAutoScheduleRecordByScheduleDate(@Param("scheduleDate") String scheduleDate);
}
