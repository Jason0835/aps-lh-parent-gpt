package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.LhflGlueStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 硫磺辅料终炼库存信息Mapper接口
 *
 * @author Liam
 * @date 2022-04-18
 */
public interface LhflGlueStockMapper extends BaseMapper<LhflGlueStock> {

    /**
     * 查询硫磺辅料终炼库存信息列表
     *
     * @param lhflGlueStock 硫磺辅料终炼库存信息
     * @return 硫磺辅料终炼库存信息集合
     */
    List<LhflGlueStock> selectLhflGlueStockList(LhflGlueStock lhflGlueStock);

    /**
     * 批量删除硫磺辅料终炼库存信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteLhflGlueStockByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listLhflGlueStockNotUnique(@Param("importList") List<LhflGlueStock> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertLhflGlueStockInfo(@Param("list") List<LhflGlueStock> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<LhflGlueStock> list);
}
