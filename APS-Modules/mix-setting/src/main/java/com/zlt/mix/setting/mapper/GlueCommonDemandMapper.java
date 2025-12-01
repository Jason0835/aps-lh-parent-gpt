package com.zlt.mix.setting.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.GlueCommonDemand;

/**
 * 密炼机常用大规格设置Mapper接口
 * 
 * @author zlt
 * @date 2023-02-05
 */
public interface GlueCommonDemandMapper extends BaseMapper<GlueCommonDemand> {

    /**
     * 查询密炼机常用大规格设置列表
     * 
     * @param glueCommonDemand 密炼机常用大规格设置
     * @return 密炼机常用大规格设置集合
     */
    List<GlueCommonDemand> selectGlueCommonDemandList(GlueCommonDemand glueCommonDemand);

    /**
     * 批量删除密炼机常用大规格设置
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteGlueCommonDemandByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listGlueCommonDemandNotUnique(@Param("importList") List<GlueCommonDemand> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     * @param list
     */
    void batchInsertGlueCommonDemandInfo(@Param("list") List<GlueCommonDemand> list);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    void mergeSql(List<GlueCommonDemand> list);
}
