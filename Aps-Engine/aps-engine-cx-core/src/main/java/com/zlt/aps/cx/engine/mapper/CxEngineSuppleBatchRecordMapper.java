package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineSuppleBatchRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成型前日计划增补批次Mapper接口
 * 
 * @author Joran.zhang
 * @date 2022-02-09
 */
public interface CxEngineSuppleBatchRecordMapper 
{
    /**
     * 查询成型前日计划增补批次
     * 
     * @param id 成型前日计划增补批次ID
     * @return 成型前日计划增补批次
     */
    public CxEngineSuppleBatchRecord selectCxEngineSuppleBatchRecordById(Long id);

    /**
     * 查询成型前日计划增补批次列表
     * 
     * @param cxEngineSuppleBatchRecord 成型前日计划增补批次
     * @return 成型前日计划增补批次集合
     */
    public List<CxEngineSuppleBatchRecord> selectCxEngineSuppleBatchRecordList(CxEngineSuppleBatchRecord cxEngineSuppleBatchRecord);

    /**
     * 新增成型前日计划增补批次
     * 
     * @param cxEngineSuppleBatchRecord 成型前日计划增补批次
     * @return 结果
     */
    public int insertCxEngineSuppleBatchRecord(CxEngineSuppleBatchRecord cxEngineSuppleBatchRecord);

    /**
     * 修改成型前日计划增补批次
     * 
     * @param cxEngineSuppleBatchRecord 成型前日计划增补批次
     * @return 结果
     */
    public int updateCxEngineSuppleBatchRecord(CxEngineSuppleBatchRecord cxEngineSuppleBatchRecord);

    /**
     * 删除成型前日计划增补批次
     * 
     * @param id 成型前日计划增补批次ID
     * @return 结果
     */
    public int deleteCxEngineSuppleBatchRecordById(Long id);

    /**
     * 批量删除成型前日计划增补批次
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxEngineSuppleBatchRecordByIds(Long[] ids);

    /**
     * 根据增补日期删除增补批次数据
     * @param suppleDate 增补计划日期
     * @return
     */
    public int deleteCxEngineSuppleBatchRecordBySuppleDate(@Param("suppleDate") String suppleDate);


}
