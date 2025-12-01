package com.zlt.mix.setting.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import org.apache.ibatis.annotations.Param;
import com.zlt.mix.setting.api.domain.entity.GlueSafeStock;

/**
 * 安全库存Mapper接口
 * 
 * @author Gim
 * @date 2022-03-21
 */
public interface GlueSafeStockMapper extends BaseMapper<GlueSafeStock> {

    /**
     * 查询安全库存列表
     * 
     * @param glueSafeStock 安全库存
     * @return 安全库存集合
     */
    List<GlueSafeStock> selectGlueSafeStockList(GlueSafeStock glueSafeStock);

    /**
     * 批量删除安全库存
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteGlueSafeStockByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listGlueSafeStockNotUnique(@Param("importList") List<GlueSafeStock> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     * @param list
     */
    void batchInsertGlueSafeStockInfo(@Param("list") List<GlueSafeStock> list);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    void mergeSql(List<GlueSafeStock> list);

    /**
     * 根据密炼区和胶料名称更改安全库存
     */
    void updateSafeStockByMixAreaAndGlue(GlueSafeStock glueSafeStock);
}
