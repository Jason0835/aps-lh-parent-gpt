package com.zlt.aps.itf.mes.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 直裁班次配置查询 Mapper（APS 数据源）。
 * <p>用于查询 APS 库的 t_cd90_shift_config，不使用类级 @DS 注解，
 * 数据源由调用方通过 DynamicDataSourceContextHolder.push(DataSource.APS) 切换。</p>
 *
 * @author APS Team
 */
@Mapper
public interface Cd90ShiftQueryMapper {

    /**
     * 查询指定工厂启用的班次配置列表。
     *
     * @param factoryCode 工厂编码
     * @return 启用的班次配置列表
     */
    List<Cd90ShiftConfig> listActiveShiftConfigs(@Param("factoryCode") String factoryCode);
}
