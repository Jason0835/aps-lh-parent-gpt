package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 精度计划Mapper
 *
 * @author APS Team
 */
@Mapper
public interface CxPrecisionPlanMapper extends CommBaseMapper<CxPrecisionPlan> {

    List<CxPrecisionPlan> selectPendingWarningPlans(@Param("daysToDue") Integer daysToDue);

    int batchUpdateDaysToDue();

    /**
     * 逻辑删除指定分厂所有MES同步来源的成型精度计划数据
     * 用于分发同步前清理旧数据（仅清理DATA_SOURCE='0'的MES同步数据，保留系统自动生成的数据）
     * WHERE必须包含FACTORY_CODE业务主键，否则会被BlockAttackInnerInterceptor拦截
     *
     * @param factoryCode 分厂编号
     * @return 受影响行数
     */
    @Update("UPDATE T_CX_PRECISION_PLAN SET IS_DELETE = 1, UPDATE_BY = 'MES', UPDATE_TIME = NOW() WHERE FACTORY_CODE = #{factoryCode} AND PRECISION_TYPE = '成型精度' AND DATA_SOURCE = '0' AND IS_DELETE = 0")
    int logicDeleteMesSyncByFactoryCode(@Param("factoryCode") String factoryCode);
}
