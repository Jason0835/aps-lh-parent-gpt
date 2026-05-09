package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.entity.LhChipStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * 芯片库存 Mapper
 *
 * @author APS Team
 * @date 2026-04-02
 */
@Mapper
public interface LhChipStockMapper extends BaseMapper<LhChipStock> {

    /**
     * 根据分厂编号和数据来源逻辑删除芯片库存
     *
     * @param factoryCode 分厂编号
     * @param dataSource  数据来源
     * @param updateBy    更新者
     * @param updateTime  更新时间
     * @return 更新的记录数
     */
    int logicDeleteByFactoryCodeAndDataSource(@Param("factoryCode") String factoryCode,
                                              @Param("dataSource") String dataSource,
                                              @Param("updateBy") String updateBy,
                                              @Param("updateTime") Date updateTime);
}
