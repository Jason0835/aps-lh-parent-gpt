package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineLossSetting;

import java.util.List;

/**
 * 成型损耗率设定Mapper接口
 * 
 * @author Joran.zhang
 * @date 2021-06-29
 */
public interface CxEngineLossSettingMapper 
{
    /**
     * 查询成型损耗率设定
     * 
     * @param id 成型损耗率设定ID
     * @return 成型损耗率设定
     */
    public CxEngineLossSetting selectCxEngineLossSettingById(Long id);

    /**
     * 查询成型损耗率设定列表
     * 
     * @param cxEngineLossSetting 成型损耗率设定
     * @return 成型损耗率设定集合
     */
    public List<CxEngineLossSetting> selectCxEngineLossSettingList(CxEngineLossSetting cxEngineLossSetting);

    /**
     * 新增成型损耗率设定
     * 
     * @param cxEngineLossSetting 成型损耗率设定
     * @return 结果
     */
    public int insertCxEngineLossSetting(CxEngineLossSetting cxEngineLossSetting);

    /**
     * 修改成型损耗率设定
     * 
     * @param cxEngineLossSetting 成型损耗率设定
     * @return 结果
     */
    public int updateCxEngineLossSetting(CxEngineLossSetting cxEngineLossSetting);

    /**
     * 删除成型损耗率设定
     * 
     * @param id 成型损耗率设定ID
     * @return 结果
     */
    public int deleteCxEngineLossSettingById(Long id);

    /**
     * 批量删除成型损耗率设定
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxEngineLossSettingByIds(Long[] ids);
}
