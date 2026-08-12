package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 硫化精度计划Mapper接口
 *
 * @author APS Team
 */
public interface LhPrecisionPlanMapper extends BaseMapper<LhPrecisionPlan> {

    /**
     * 查询硫化精度计划列表
     *
     * @param vo 查询条件
     * @return 计划列表
     */
    List<LhPrecisionPlan> selectLhPrecisionPlanList(LhPrecisionPlanVo vo);

    /**
     * 根据机台编码和年份查询计划
     *
     * @param machineCode 机台编码
     * @param year 年份
     * @return 计划
     */
    LhPrecisionPlan selectByMachineCodeAndYear(@Param("machineCode") String machineCode, @Param("year") Integer year);

    /**
     * 查询机台最近一次已完成的计划
     *
     * @param machineCode 机台编码
     * @param year 年份
     * @return 计划
     */
    LhPrecisionPlan selectLastCompletedPlan(@Param("machineCode") String machineCode, @Param("year") Integer year);

    /**
     * 查询待预警的计划列表
     *
     * @param daysToDue 到期天数阈值
     * @return 计划列表
     */
    List<LhPrecisionPlan> selectPendingWarningPlans(@Param("daysToDue") Integer daysToDue);

    /**
     * 批量更新到期天数
     *
     * @return 更新数量
     */
    int batchUpdateDaysToDue();

    /**
     * 查询最接近计划排程精度日期且实际执行时间为空的硫化精度计划
     * 用于MES回填实际执行日期时匹配
     *
     * @param machineCode 机台编码
     * @param factoryCode 分厂编码
     * @param actualDate 实际执行日期
     * @return 最匹配的精度计划
     */
    LhPrecisionPlan selectNearestScheduleDatePlan(@Param("machineCode") String machineCode,
                                                   @Param("factoryCode") String factoryCode,
                                                   @Param("actualDate") java.util.Date actualDate);

    /**
     * 根据MES来源ID查询硫化精度计划
     * 用于防止同一MES数据重复生成精度计划
     *
     * @param mesSourceId MES来源ID（对应t_mdm_dev_maintenance_plan的主键ID）
     * @return 精度计划，如果已存在说明该MES数据已经处理过
     */
    LhPrecisionPlan selectByMesSourceId(@Param("mesSourceId") Long mesSourceId);

    /**
     * 根据多个MES来源ID批量查询硫化精度计划
     * 用于批量防重复校验
     *
     * @param mesSourceIds MES来源ID列表
     * @return 精度计划列表
     */
    List<LhPrecisionPlan> selectByMesSourceIdBatch(@Param("mesSourceIds") List<Long> mesSourceIds);

    /**
     * 逻辑删除指定分厂所有MES同步来源的硫化精度计划数据
     * 用于分发同步前清理旧数据（仅清理DATA_SOURCE='0'的MES同步数据，保留系统自动生成的数据）
     * WHERE必须包含FACTORY_CODE业务主键，否则会被BlockAttackInnerInterceptor拦截
     *
     * @param factoryCode 分厂编号
     * @return 受影响行数
     */
    @Update("UPDATE T_LH_PRECISION_PLAN SET IS_DELETE = 1, UPDATE_BY = 'MES', UPDATE_TIME = NOW() WHERE FACTORY_CODE = #{factoryCode} AND PRECISION_TYPE = '硫化精度' AND DATA_SOURCE = '0' AND IS_DELETE = 0")
    int logicDeleteMesSyncByFactoryCode(@Param("factoryCode") String factoryCode);

    /**
     * 根据机台编码列表和年份批量查询计划
     * 用于批量判断机台在指定年度是否已有计划
     *
     * @param machineCodes 机台编码列表
     * @param year 年份
     * @return 计划列表
     */
    List<LhPrecisionPlan> selectByMachineCodesAndYear(@Param("machineCodes") List<String> machineCodes, @Param("year") Integer year);

    /**
     * 批量查询所有有排程精度日期且实际执行日期为空的硫化精度计划
     * 用于MES批量回填实际执行日期时匹配
     *
     * @param machineCodes 机台编码列表
     * @param factoryCodes 分厂编码列表
     * @return 符合条件的精度计划列表
     */
    List<LhPrecisionPlan> selectPendingActualDatePlans(@Param("machineCodes") List<String> machineCodes,
                                                        @Param("factoryCodes") List<String> factoryCodes);

    /**
     * 批量查询实际执行日期为空的硫化精度计划
     * 用于硫化排程批量回填计划排程精度日期
     *
     * @param machineCodes 机台编码列表
     * @param factoryCodes 分厂编码列表
     * @return 符合条件的精度计划列表
     */
    List<LhPrecisionPlan> selectPendingScheduleDatePlans(@Param("machineCodes") List<String> machineCodes,
                                                          @Param("factoryCodes") List<String> factoryCodes);

    /**
     * 根据机台编码+分厂编码+计划日期组合批量查询已存在的计划（导入唯一性校验用）
     *
     * @param factoryCodes 分厂编码列表
     * @param machineCodes 机台编码列表
     * @param planDates 计划日期列表
     * @return 已存在的计划列表
     */
    List<LhPrecisionPlan> selectByFactoryMachinePlanBatch(@Param("factoryCodes") List<String> factoryCodes,
                                                           @Param("machineCodes") List<String> machineCodes,
                                                           @Param("planDates") List<java.util.Date> planDates);
}
