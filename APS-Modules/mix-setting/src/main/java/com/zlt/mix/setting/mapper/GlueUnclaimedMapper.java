package com.zlt.mix.setting.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import org.apache.ibatis.annotations.Param;
import com.zlt.mix.setting.api.domain.entity.GlueUnclaimed;

/**
 * 胶料白班待支领Mapper接口
 * 
 * @author zlt
 * @date 2022-09-05
 */
public interface GlueUnclaimedMapper extends BaseMapper<GlueUnclaimed> {

    /**
     * 查询胶料白班待支领列表
     * 
     * @param glueUnclaimed 胶料白班待支领
     * @return 胶料白班待支领集合
     */
    List<GlueUnclaimed> selectGlueUnclaimedList(GlueUnclaimed glueUnclaimed);

    /**
     * 批量删除胶料白班待支领
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteGlueUnclaimedByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listGlueUnclaimedNotUnique(@Param("importList") List<GlueUnclaimed> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     * @param list
     */
    void batchInsertGlueUnclaimedInfo(@Param("list") List<GlueUnclaimed> list);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    void mergeSql(List<GlueUnclaimed> list);
}
