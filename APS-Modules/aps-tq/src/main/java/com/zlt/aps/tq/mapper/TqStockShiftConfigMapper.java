package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 胎圈备库班数配置 Mapper接口
 *
 * @author zlt
 * @date 2026-06-25
 */
public interface TqStockShiftConfigMapper extends BaseMapper<TqStockShiftConfig> {

    /**
     * 查询胎圈备库班数配置列表
     * @param config 查询条件
     * @return 配置列表
     */
    List<TqStockShiftConfig> selectStockShiftConfigList(TqStockShiftConfig config);

    /**
     * 批量合并导入数据
     * @param list 导入数据列表
     */
    void mergeSql(@Param("list") List<TqStockShiftConfig> list);
}
