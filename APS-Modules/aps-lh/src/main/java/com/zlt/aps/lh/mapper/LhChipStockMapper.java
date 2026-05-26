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

    /**
     * 原子累加完成量（含版本号乐观锁）
     * 利用数据库行锁保证原子性，通过DATA_VERSION做乐观锁防止并发重复累加
     *
     * @param factoryCode     分厂编号
     * @param chipCode        芯片编号
     * @param delta           待累加的完成量增量
     * @param expectedVersion 期望的版本号（乐观锁），为null或空时不做版本校验
     * @param newVersion      更新后的新版本号
     * @param updateBy        更新者
     * @return 影响的行数，0表示版本号不匹配或记录不存在
     */
    int atomicAddFinishQty(@Param("factoryCode") String factoryCode,
                           @Param("chipCode") String chipCode,
                           @Param("delta") Integer delta,
                           @Param("expectedVersion") String expectedVersion,
                           @Param("newVersion") String newVersion,
                           @Param("updateBy") String updateBy);

    /**
     * 原子覆盖完成量（含版本号乐观锁）
     * 利用数据库行锁保证原子性，通过DATA_VERSION做乐观锁防止并发覆盖
     *
     * @param factoryCode     分厂编号
     * @param chipCode        芯片编号
     * @param finishQty       新的完成量值
     * @param expectedVersion 期望的版本号（乐观锁），为null或空时不做版本校验
     * @param newVersion      更新后的新版本号
     * @param updateBy        更新者
     * @return 影响的行数，0表示版本号不匹配或记录不存在
     */
    int atomicOverwriteFinishQty(@Param("factoryCode") String factoryCode,
                                 @Param("chipCode") String chipCode,
                                 @Param("finishQty") Integer finishQty,
                                 @Param("expectedVersion") String expectedVersion,
                                 @Param("newVersion") String newVersion,
                                 @Param("updateBy") String updateBy);
}
