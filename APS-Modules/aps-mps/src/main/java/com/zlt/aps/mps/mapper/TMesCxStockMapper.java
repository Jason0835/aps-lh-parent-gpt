package com.zlt.aps.mps.mapper;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.mps.domain.TMesCxMonthStock;
import com.zlt.aps.mps.domain.TMesCxStock;
import com.zlt.aps.mps.domain.TMesSapStock;

import java.util.List;

/**
 * @Entity com.zlt.aps.mps.domain.TMesCxStock
 * @Entity com.zlt.aps.mps.domain.TMesCxMonthStock
 * @Entity com.zlt.aps.mps.domain.TMesSapStock
 */
public interface TMesCxStockMapper {

   List<TMesCxStock> getCxStockByDataVersion(@Param("dataVersion") String dataVersion);

   List<TMesCxMonthStock> getCxMonthStockByDataVersion(@Param("dataVersion") String dataVersion);

   List<TMesSapStock> getCxSapStockByDataVersion(@Param("dataVersion") String dataVersion);


//    /**
//     * 合并操作，如果记录存在则更新，否则新增
//     * 胎胚库存同步接口
//     * @param list 要合并的集合
//     */
//    public void mergeCxStockSql(List<TMesCxStock> list);
//
//    /**
//     * 合并操作，如果记录存在则更新，否则新增
//     * 胎胚月结库存表
//     * @param list 要合并的集合
//     */
//    public void mergeCxMonthStockSql(List<TMesCxMonthStock> list);
//
//    /**
//     * 合并操作，如果记录存在则更新，否则新增
//     * 成品库存同步接口
//     * @param list 要合并的集合
//     */
//    public void mergeCxSapStockSql(List<TMesSapStock> list);
}




