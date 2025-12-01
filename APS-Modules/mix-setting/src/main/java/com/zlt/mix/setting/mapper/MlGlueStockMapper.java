package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.dto.MlGlueStockDto;
import com.zlt.mix.setting.api.domain.entity.MlGlueStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 母炼库存信息Mapper接口
 *
 * @author Liam
 * @date 2022-04-12
 */
public interface MlGlueStockMapper extends BaseMapper<MlGlueStock> {

    /**
     * 查询母炼库存信息列表
     *
     * @param mlGlueStock 母炼库存信息
     * @return 母炼库存信息集合
     */
    List<MlGlueStockDto> selectMlGlueStockList(MlGlueStock mlGlueStock);

    /**
     * 批量删除母炼库存信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteMlGlueStockByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listMlGlueStockNotUnique(@Param("importList") List<MlGlueStock> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertMlGlueStockInfo(@Param("list") List<MlGlueStock> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<MlGlueStock> list);
    
    /**
     * 删除记录
     * @param stockList
     */
    void batchDelete(List<MlGlueStock> stockList);
}
