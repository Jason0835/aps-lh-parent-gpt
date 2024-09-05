package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhTireConstructionInfo;

import java.util.List;

/**
 * 硫化外胎施工信息Mapper接口
 * 
 * @author zlt
 * @date 2021-11-15
 */
public interface LhTireConstructionInfoMapper 
{
    /**
     * 查询硫化外胎施工信息
     * 
     * @param id 硫化外胎施工信息ID
     * @return 硫化外胎施工信息
     */
    public LhTireConstructionInfo selectLhTireConstructionInfoById(Long id);

    /**
     * 查询硫化外胎施工信息列表
     * 
     * @param lhTireConstructionInfo 硫化外胎施工信息
     * @return 硫化外胎施工信息集合
     */
    public List<LhTireConstructionInfo> selectLhTireConstructionInfoList(LhTireConstructionInfo lhTireConstructionInfo);


    public List<LhTireConstructionInfo> checkLhTireConstructionInfoUnique(LhTireConstructionInfo lhTireConstructionInfo);
    /**
     * 新增硫化外胎施工信息
     * 
     * @param lhTireConstructionInfo 硫化外胎施工信息
     * @return 结果
     */
    public int insertLhTireConstructionInfo(LhTireConstructionInfo lhTireConstructionInfo);

    /**
     * 修改硫化外胎施工信息
     * 
     * @param lhTireConstructionInfo 硫化外胎施工信息
     * @return 结果
     */
    public int updateLhTireConstructionInfo(LhTireConstructionInfo lhTireConstructionInfo);

    /**
     * 删除硫化外胎施工信息
     * 
     * @param id 硫化外胎施工信息ID
     * @return 结果
     */
    public int deleteLhTireConstructionInfoById(Long id);

    /**
     * 批量删除硫化外胎施工信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLhTireConstructionInfoByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<LhTireConstructionInfo> list);

    /**
     * 根据sap查询对应的胎胚代码
     * @param lhTireConstructionInfo sap品号
     * @return 查询到的胎胚代码
     */
    public List<LhTireConstructionInfo> getEmbryoCodeListBySapCode(LhTireConstructionInfo lhTireConstructionInfo);
}
