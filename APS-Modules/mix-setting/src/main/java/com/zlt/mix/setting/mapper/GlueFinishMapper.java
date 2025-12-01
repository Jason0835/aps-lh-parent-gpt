package com.zlt.mix.setting.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import org.apache.ibatis.annotations.Param;
import com.zlt.mix.setting.api.domain.entity.GlueFinish;

/**
 * 炼胶时间信息Mapper接口
 *
 * @author Gim
 * @date 2022-03-29
 */
public interface GlueFinishMapper extends BaseMapper<GlueFinish> {

    /**
     * 查询炼胶时间信息列表
     *
     * @param glueFinish 炼胶时间信息
     * @return 炼胶时间信息集合
     */
    List<GlueFinish> selectGlueFinishList(GlueFinish glueFinish);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listGlueFinishNotUnique(@Param("importList") List<GlueFinish> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertGlueFinishInfo(@Param("list") List<GlueFinish> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<GlueFinish> list);
}
