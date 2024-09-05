package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 施工信息Mapper接口
 * 
 * @author Joran.zhang
 * @date 2021-06-30
 */
public interface EngineConstructionInfoMapper 
{
    /**
     * 查询施工信息
     * 
     * @param id 施工信息ID
     * @return 施工信息
     */
    public EngineConstructionInfo selectEngineConstructionInfoById(Long id);

    /**
     * 查询施工信息列表
     * 
     * @param engineConstructionInfo 施工信息
     * @return 施工信息集合
     */
    public List<EngineConstructionInfo> selectEngineConstructionInfoList(EngineConstructionInfo engineConstructionInfo);

    /**
     * 新增施工信息
     * 
     * @param engineConstructionInfo 施工信息
     * @return 结果
     */
    public int insertEngineConstructionInfo(EngineConstructionInfo engineConstructionInfo);

    /**
     * 修改施工信息
     * 
     * @param engineConstructionInfo 施工信息
     * @return 结果
     */
    public int updateEngineConstructionInfo(EngineConstructionInfo engineConstructionInfo);

    /**
     * 删除施工信息
     * 
     * @param id 施工信息ID
     * @return 结果
     */
    public int deleteEngineConstructionInfoById(Long id);

    /**
     * 批量删除施工信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteEngineConstructionInfoByIds(Long[] ids);

    List<EngineConstructionInfo> selectEngineConstructionInfoListBatch(@Param("list") List<String> embryoCodeList);

    void mergeSql(List<EngineConstructionInfo> list);
}
