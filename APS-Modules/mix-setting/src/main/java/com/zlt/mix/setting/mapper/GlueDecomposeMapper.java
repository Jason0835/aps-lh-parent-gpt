package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.GlueDecompose;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 终炼母炼分解Mapper接口
 *
 * @author Liam
 * @date 2022-03-28
 */
public interface GlueDecomposeMapper extends BaseMapper<GlueDecompose> {

    /**
     * 查询终炼母炼分解列表
     *
     * @param glueDecompose 终炼母炼分解
     * @return 终炼母炼分解集合
     */
    List<GlueDecompose> selectGlueDecomposeList(GlueDecompose glueDecompose);

    /**
     * 批量删除终炼母炼分解
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteGlueDecomposeByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listGlueDecomposeNotUnique(@Param("importList") List<GlueDecompose> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertGlueDecomposeInfo(@Param("list") List<GlueDecompose> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<GlueDecompose> list);
}
