package com.zlt.mix.setting.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.FactoryGlueAreaRelation;

/**
 * 分厂胶料与密炼区对应关系Mapper接口
 * 
 * @author zlt
 * @date 2022-11-22
 */
public interface FactoryGlueAreaRelationMapper extends BaseMapper<FactoryGlueAreaRelation>
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
    public int insertFactoryGlueAreaRelation(FactoryGlueAreaRelation tFactoryGlueAreaRelation);

    /**
     * 修改分厂胶料与密炼区对应关系
     * 
     * @param tFactoryGlueAreaRelation 分厂胶料与密炼区对应关系
     * @return 结果
     */
    public int updateFactoryGlueAreaRelation(FactoryGlueAreaRelation tFactoryGlueAreaRelation);

    /**
     * 删除分厂胶料与密炼区对应关系
     * 
     * @param id 分厂胶料与密炼区对应关系ID
     * @return 结果
     */
    public int deleteFactoryGlueAreaRelationById(Long id);

    /**
     * 批量删除分厂胶料与密炼区对应关系
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteFactoryGlueAreaRelationByIds(Long[] ids);
    
    /**
     *  校验数据库唯一分厂胶料与密炼区对应关系
     * @param list
     * @param importLogId
     * @param errorDetail
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listFactoryGlueAreaRelationNotUnique(@Param("importList") List<FactoryGlueAreaRelation> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);


    /**
     * 批量新增
     * @param list
     */
    void batchInsertFactoryGlueAreaRelationInfo(@Param("list") List<FactoryGlueAreaRelation> list);
    
    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<FactoryGlueAreaRelation> list);
}
