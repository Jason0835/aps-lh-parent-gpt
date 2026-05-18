package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxScheFinishQty;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 成型排程完成量回报Mapper
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Mapper
public interface CxScheFinishQtyMapper extends CommBaseMapper<CxScheFinishQty> {

    /**
     * 根据唯一键查询已存在的数据
     *
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<CxScheFinishQty> selectByUniqueKeyList(@Param("list") List<CxScheFinishQty> list);

    /**
     * 根据分厂编号逻辑删除成型排程完成量数据
     *
     * @param factoryCode 分厂编号
     * @param updateBy    更新者
     * @param updateTime  更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE T_CX_SCHE_FINISH_QTY SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} AND IS_DELETE = 0")
    int logicDeleteByFactoryCode(@Param("factoryCode") String factoryCode, @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /**
     * 根据分厂编号和排程日期逻辑删除成型排程完成量数据
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期
     * @param updateBy     更新者
     * @param updateTime   更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE T_CX_SCHE_FINISH_QTY SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} AND DATE(SCHEDULE_DATE) = #{scheduleDate} AND IS_DELETE = 0")
    int logicDeleteByFactoryCodeAndScheduleDate(@Param("factoryCode") String factoryCode, @Param("scheduleDate") Date scheduleDate, @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /**
     * 逻辑删除今天及今天之前所有数据（将IS_DELETE置为1）
     * 用于清理任务：先删除所有历史数据（含今天），再从MES重新抓取每天最新版本数据
     *
     * @return 更新的记录数
     */
    @Update("UPDATE T_CX_SCHE_FINISH_QTY SET IS_DELETE = 1, UPDATE_BY = 'CLEAN_TASK', UPDATE_TIME = NOW() WHERE DATE(SCHEDULE_DATE) <= CURDATE() AND IS_DELETE = 0")
    int logicDeleteAllBeforeToday();

}
