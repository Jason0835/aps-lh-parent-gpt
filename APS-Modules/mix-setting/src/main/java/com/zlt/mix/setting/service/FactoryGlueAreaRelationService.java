package com.zlt.mix.setting.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.FactoryGlueAreaRelation;

/**
 * 分厂胶料与密炼区对应关系Service接口
 * 
 * @author zlt
 * @date 2022-11-22
 */
public interface FactoryGlueAreaRelationService
{
    /**
     * 查询分厂胶料与密炼区对应关系
     * 
     * @param id 分厂胶料与密炼区对应关系ID
     * @return 分厂胶料与密炼区对应关系
     */
    public FactoryGlueAreaRelation selectFactoryGlueAreaRelationById(Long id);

    /**
     * 查询分厂胶料与密炼区对应关系列表
     * 
     * @param tFactoryGlueAreaRelation 分厂胶料与密炼区对应关系
     * @return 分厂胶料与密炼区对应关系集合
     */
    public List<FactoryGlueAreaRelation> selectFactoryGlueAreaRelationList(FactoryGlueAreaRelation tFactoryGlueAreaRelation);

    /**
     * 新增分厂胶料与密炼区对应关系
     * 
     * @param tFactoryGlueAreaRelation 分厂胶料与密炼区对应关系
     * @return 结果
     */
    @Transactional
    public int insertFactoryGlueAreaRelation(FactoryGlueAreaRelation tFactoryGlueAreaRelation);

    /**
     * 修改分厂胶料与密炼区对应关系
     * 
     * @param tFactoryGlueAreaRelation 分厂胶料与密炼区对应关系
     * @return 结果
     */
    @Transactional
    public int updateFactoryGlueAreaRelation(FactoryGlueAreaRelation tFactoryGlueAreaRelation);

    /**
     * 批量删除分厂胶料与密炼区对应关系
     * 
     * @param ids 需要删除的分厂胶料与密炼区对应关系ID
     * @return 结果
     */
    @Transactional
    public int deleteFactoryGlueAreaRelationByIds(Long[] ids);

    /**
     * 删除分厂胶料与密炼区对应关系信息
     * 
     * @param id 分厂胶料与密炼区对应关系ID
     * @return 结果
     */
    @Transactional
    public int deleteFactoryGlueAreaRelationById(Long id);

    /**
     * 校验分厂胶料与密炼区对应关系唯一性
     */
    public String checkFactoryGlueAreaRelationUnique(FactoryGlueAreaRelation tFactoryGlueAreaRelation);

    /**
     * 导入分厂胶料与密炼区对应关系数据
     */
    @Transactional
    public AjaxResult importData(List<FactoryGlueAreaRelation> list, boolean updateSupport, Long importLogId);
}
