package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 硫化排程日完成量Mapper
 *
 * @author APS Team
 * @since 2026/04/13
 */
@Mapper
public interface LhDayFinishQtyMapper extends CommBaseMapper<LhDayFinishQty> {

    /**
     * 根据唯一键查询已存在的数据
     *
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<LhDayFinishQty> selectByUniqueKeyList(@Param("list") List<LhDayFinishQty> list);

    /**
     * 根据分厂编号逻辑删除硫化排程日完成量数据
     *
     * @param factoryCode 分厂编号
     * @param updateBy    更新者
     * @param updateTime  更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE T_LH_DAY_FINISH_QTY SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} AND IS_DELETE = 0")
    int logicDeleteByFactoryCode(@Param("factoryCode") String factoryCode, @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /**
     * 根据分厂编号和完成日期逻辑删除硫化排程日完成量数据
     *
     * @param factoryCode 分厂编号
     * @param finishDate  完成日期
     * @param updateBy    更新者
     * @param updateTime  更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE T_LH_DAY_FINISH_QTY SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} AND DATE(FINISH_DATE) = #{finishDate} AND IS_DELETE = 0")
    int logicDeleteByFactoryCodeAndFinishDate(@Param("factoryCode") String factoryCode, @Param("finishDate") Date finishDate, @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /**
     * 逻辑删除历史重复数据，保留每个历史完成日期DATA_VERSION最大（最新版本）的数据
     *
     * @return 更新的记录数
     */
    @Update("UPDATE T_LH_DAY_FINISH_QTY SET IS_DELETE = 1, UPDATE_BY = 'CLEAN_TASK', UPDATE_TIME = NOW() WHERE DATE(FINISH_DATE) < CURDATE() AND IS_DELETE = 0 AND " +
            "EXISTS (" +
            "SELECT 1 FROM (" +
            "SELECT FACTORY_CODE, DATE(FINISH_DATE) AS finish_day, MAX(DATA_VERSION) AS max_data_version " +
            "FROM T_LH_DAY_FINISH_QTY WHERE DATE(FINISH_DATE) < CURDATE() AND IS_DELETE = 0 " +
            "GROUP BY FACTORY_CODE, DATE(FINISH_DATE)" +
            ") latest " +
            "WHERE T_LH_DAY_FINISH_QTY.FACTORY_CODE = latest.FACTORY_CODE " +
            "AND DATE(T_LH_DAY_FINISH_QTY.FINISH_DATE) = latest.finish_day " +
            "AND T_LH_DAY_FINISH_QTY.DATA_VERSION < latest.max_data_version" +
            ")")
    int cleanHistoryDuplicateData();

}
