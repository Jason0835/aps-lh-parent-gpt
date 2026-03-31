package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MdmTreadStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 胎面库存Mapper接口
 */
@Mapper
public interface MdmTreadStockEntityMapper extends BaseMapper<MdmTreadStock> {

    /**
     * 根据唯一键查询
     * @param stockDate 库存日期
     * @param materialCode 胎面物料编码
     * @param factoryCode 分厂编码
     * @return 查询结果
     */
    MdmTreadStock selectByUniqueKey(@Param("stockDate") String stockDate,
                                     @Param("materialCode") String materialCode,
                                     @Param("factoryCode") String factoryCode);

    /**
     * 批量根据唯一键查询
     * @param list 数据列表
     * @return 查询结果
     */
    List<MdmTreadStock> selectByUniqueKeyList(@Param("list") List<MdmTreadStock> list);
}
