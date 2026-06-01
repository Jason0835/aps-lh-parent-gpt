package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.vo.MesPrecisionPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 精度计划下发Mapper接口
 * 写入MES中间表MES_PRECISION_PLAN（建在MES分库），通知MES来获取精度计划排程数据
 * 成型精度和硫化精度统一使用此表，通过PRECISION_TYPE区分
 *
 * @author APS Team
 */
@DS(DataSource.MES)
@Mapper
public interface PrecisionPlanIssueMapper {

    /**
     * 批量新增精度计划到APS中间表MES_PRECISION_PLAN
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchInsertPrecisionPlan(@Param("list") List<MesPrecisionPlan> list);

    /**
     * 根据机台编码和精度类型更新数据
     *
     * @param mesItem 数据项
     * @return 影响行数
     */
    int updateByMachineCodeAndPrecisionType(MesPrecisionPlan mesItem);

    /**
     * 根据分厂和精度类型删除数据
     *
     * @param factoryCode 分厂编码
     * @param precisionType 精度类型
     * @param dataVersion 版本号
     * @return 影响行数
     */
    int deleteByFactoryCodeAndPrecisionType(@Param("factoryCode") String factoryCode,
                                            @Param("precisionType") String precisionType,
                                            @Param("dataVersion") String dataVersion);
}
