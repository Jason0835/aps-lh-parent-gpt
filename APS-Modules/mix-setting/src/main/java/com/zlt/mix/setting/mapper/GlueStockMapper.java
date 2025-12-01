package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.dto.GlueStockDto;
import com.zlt.mix.setting.api.domain.entity.GlueStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 终炼胶库存信息Mapper接口
 *
 * @author Gim
 * @date 2022-03-18
 */
public interface GlueStockMapper extends BaseMapper<GlueStock> {

    /**
     * 查询库存信息列表
     *
     * @param glueStock 库存信息
     * @return 库存信息集合
     */
    List<GlueStockDto> selectGlueStockList(GlueStock glueStock);

    /**
     * 批量删除库存信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteGlueStockByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listGlueStockNotUnique(@Param("importList") List<GlueStock> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     * @param list
     */
    void batchInsertGlueStockInfo(@Param("list") List<GlueStock> list);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    void mergeSql(List<GlueStock> list);
}
