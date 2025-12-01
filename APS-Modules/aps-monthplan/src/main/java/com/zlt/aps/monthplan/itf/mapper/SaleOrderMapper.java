package com.zlt.aps.monthplan.itf.mapper;

import com.ruoyi.api.gateway.system.domain.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Chen
 * @date 2025/4/20
 */
@Mapper
public interface SaleOrderMapper {

    /**
     * 查询系统参数
     *
     * @param queryParam 参数
     * @return 结果
     */
    SysConfig getSysConfigByKey(SysConfig queryParam);
}
