package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.BhgGlueStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 不合格胶库存信息Mapper接口
 *
 * @author Liam
 * @date 2022-04-12
 */
public interface BhgGlueStockMapper extends BaseMapper<BhgGlueStock> {

    /**
     * 查询不合格胶库存信息列表
     *
     * @param bhgGlueStock 不合格胶库存信息
     * @return 不合格胶库存信息集合
     */
    List<BhgGlueStock> selectBhgGlueStockList(BhgGlueStock bhgGlueStock);

    /**
     * 批量删除不合格胶库存信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteBhgGlueStockByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listBhgGlueStockNotUnique(@Param("importList") List<BhgGlueStock> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertBhgGlueStockInfo(@Param("list") List<BhgGlueStock> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<BhgGlueStock> list);
}
