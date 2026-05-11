package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 库存Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface CxStockMapper extends CommBaseMapper<CxStock> {

    /**
     * 根据唯一键查询已存在的数据（仅未删除）
     *
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<CxStock> selectByUniqueKeyList(@Param("list") List<CxStock> list);

    /**
     * 根据分厂编号和数据来源逻辑删除成型库存
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

    /**
     * 根据分厂编号、数据来源和库存日期逻辑删除成型库存
     *
     * @param factoryCode 分厂编号
     * @param dataSource  数据来源
     * @param stockDate   库存日期
     * @param updateBy    更新者
     * @param updateTime  更新时间
     * @return 更新的记录数
     */
    int logicDeleteByFactoryCodeAndDataSourceAndStockDate(@Param("factoryCode") String factoryCode,
                                                          @Param("dataSource") String dataSource,
                                                          @Param("stockDate") Date stockDate,
                                                          @Param("updateBy") String updateBy,
                                                          @Param("updateTime") Date updateTime);

    /**
     * 根据ID列表逻辑删除成型库存
     *
     * @param ids        ID列表
     * @param updateBy   更新者
     * @param updateTime 更新时间
     * @return 更新的记录数
     */
    int logicDeleteByIds(@Param("ids") List<Long> ids,
                         @Param("updateBy") String updateBy,
                         @Param("updateTime") Date updateTime);

    /**
     * 根据唯一键恢复已逻辑删除的成型库存数据
     *
     * @param list       唯一键列表
     * @param dataSource 数据来源
     * @param updateBy   更新者
     * @param updateTime 更新时间
     * @return 更新的记录数
     */
    int recoverByUniqueKeyList(@Param("list") List<CxStock> list,
                               @Param("dataSource") String dataSource,
                               @Param("updateBy") String updateBy,
                               @Param("updateTime") Date updateTime);

    /**
     * 根据分厂编号和数据来源查询成型库存ID列表（仅未删除）
     *
     * @param factoryCode 分厂编号
     * @param dataSource  数据来源
     * @return 库存列表（仅含ID和唯一键）
     */
    List<CxStock> selectByFactoryCodeAndDataSource(@Param("factoryCode") String factoryCode, @Param("dataSource") String dataSource);

    /**
     * 根据分厂编号和数据来源查询全部成型库存（包含已删除）
     *
     * @param factoryCode 分厂编号
     * @param dataSource  数据来源
     * @return 库存列表
     */
    List<CxStock> selectAllByFactoryCodeAndDataSource(@Param("factoryCode") String factoryCode, @Param("dataSource") String dataSource);

    /**
     * 根据分厂编号和数据来源物理删除成型库存（真删除，非逻辑删除）
     *
     * @param factoryCode 分厂编号
     * @param dataSource  数据来源
     * @return 删除的记录数
     */
    int physicalDeleteByFactoryCodeAndDataSource(@Param("factoryCode") String factoryCode, @Param("dataSource") String dataSource);
}
