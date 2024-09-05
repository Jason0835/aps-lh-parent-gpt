package com.zlt.aps.common.engine.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.engine.domain.BaseCxConstructionInfo;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;

import java.util.List;
import java.util.Map;


/**
 * 施工信息Service接口
 * 
 * @author Joran.zhang
 * @date 2021-06-30
 */
public interface EngineConstructionInfoService
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
     * 批量查询施工信息列表
     * @param embryoCodeList 胚胎代码List
     * @return 施工信息集合
     */
    public List<EngineConstructionInfo> selectEngineConstructionInfoListBatch(List<String> embryoCodeList);

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
     * 批量删除施工信息
     * 
     * @param ids 需要删除的施工信息ID
     * @return 结果
     */
    public int deleteEngineConstructionInfoByIds(Long[] ids);

    /**
     * 删除施工信息信息
     * 
     * @param id 施工信息ID
     * @return 结果
     */
    public int deleteEngineConstructionInfoById(Long id);

    /**
     * 校验施工信息唯一性
     */
    public String checkEngineConstructionInfoUnique(EngineConstructionInfo engineConstructionInfo);

    /**
     * 加载成型定额相关的属性
     * @return
     */
    public Map<String, BaseCxConstructionInfo> loadConstructionInfo();

    /**
     * 加载成型排程时根据胎胚代码组装相应的施工信息
     * @return
     */
    public Map<String,EngineConstructionInfo> loadEngineConstructionMap();

    /**
     * 组装定额相关内容
     * @param engineConstructionInfoMap
     * @return
     */
    public Map<String,BaseCxConstructionInfo> changeConstructionInfo(Map<String,EngineConstructionInfo> engineConstructionInfoMap);

    AjaxResult mergeSql(List<EngineConstructionInfo> list);
}
