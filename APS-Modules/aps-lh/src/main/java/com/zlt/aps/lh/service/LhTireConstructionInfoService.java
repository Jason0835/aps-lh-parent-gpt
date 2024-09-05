package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhTireConstructionInfo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 硫化外胎施工信息Service接口
 * 
 * @author zlt
 * @date 2021-11-15
 */
public interface LhTireConstructionInfoService
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

    /**
     * 新增硫化外胎施工信息
     * 
     * @param lhTireConstructionInfo 硫化外胎施工信息
     * @return 结果
     */
    @Transactional
    public int insertLhTireConstructionInfo(LhTireConstructionInfo lhTireConstructionInfo);

    /**
     * 修改硫化外胎施工信息
     * 
     * @param lhTireConstructionInfo 硫化外胎施工信息
     * @return 结果
     */
    @Transactional
    public int updateLhTireConstructionInfo(LhTireConstructionInfo lhTireConstructionInfo);

    /**
     * 批量删除硫化外胎施工信息
     * 
     * @param ids 需要删除的硫化外胎施工信息ID
     * @return 结果
     */
    @Transactional
    public int deleteLhTireConstructionInfoByIds(Long[] ids);

    /**
     * 删除硫化外胎施工信息信息
     * 
     * @param id 硫化外胎施工信息ID
     * @return 结果
     */
    @Transactional
    public int deleteLhTireConstructionInfoById(Long id);

    /**
     * 校验硫化外胎施工信息唯一性
     */
    public String checkLhTireConstructionInfoUnique(LhTireConstructionInfo lhTireConstructionInfo);

    /**
     * 导入硫化外胎施工信息数据
     */
    @Transactional
    public AjaxResult importData(List<LhTireConstructionInfo> list, boolean updateSupport, Long importLogId);

    /**
     * 根据sap查询对应的胎胚代码
     * @param lhTireConstructionInfo sap品号
     * @return 查询到的胎胚代码
     */
    public List<LhTireConstructionInfo> getEmbryoCodeListBySapCode(LhTireConstructionInfo lhTireConstructionInfo);
}
