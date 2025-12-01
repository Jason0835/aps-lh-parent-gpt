package com.zlt.mix.setting.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.RecipeType;
import org.apache.ibatis.annotations.Param;

/**
 * 配方类型Mapper接口
 * 
 * @author Joran.zhang
 * @date 2022-05-31
 */
public interface RecipeTypeMapper extends BaseMapper<RecipeType> {

    /**
     * 查询配方类型列表
     * 
     * @param recipeType 配方类型
     * @return 配方类型集合
     */
    List<RecipeType> selectRecipeTypeList(RecipeType recipeType);

    /**
     * 批量删除配方类型
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteRecipeTypeByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listRecipeTypeNotUnique(@Param("importList") List<RecipeType> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     * @param list
     */
    void batchInsertRecipeTypeInfo(@Param("list") List<RecipeType> list);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    void mergeSql(List<RecipeType> list);

    /**
     * 单独验证名称是否重复
     * @param list
     * @param importLogId
     * @param errorDetail
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listRecipeTypeNameNotUnique(@Param("importList") List<RecipeType> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);
}
