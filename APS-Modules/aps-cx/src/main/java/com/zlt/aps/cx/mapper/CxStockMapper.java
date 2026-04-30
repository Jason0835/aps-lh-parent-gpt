package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库存Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface CxStockMapper extends CommBaseMapper<CxStock> {

    /**
     * 根据唯一键查询已存在的数据
     *
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<CxStock> selectByUniqueKeyList(@Param("list") List<CxStock> list);

    /**
     * 根据分厂编号和数据来源删除成型库存
     *
     * @param factoryCode 分厂编号
     * @param dataSource  数据来源
     * @return 删除的记录数
     */
    int deleteByFactoryCodeAndDataSource(@Param("factoryCode") String factoryCode, @Param("dataSource") String dataSource);

    /**
     * 根据分厂编号和数据来源查询成型库存ID列表
     *
     * @param factoryCode 分厂编号
     * @param dataSource  数据来源
     * @return 库存列表（仅含ID和唯一键）
     */
    List<CxStock> selectByFactoryCodeAndDataSource(@Param("factoryCode") String factoryCode, @Param("dataSource") String dataSource);
}
