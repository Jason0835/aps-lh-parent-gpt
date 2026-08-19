package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface TqStockMapper extends BaseMapper<TqStock> {

    List<TqStock> checkStockListUnic(@Param("entity") TqStock stock);

    void mergeSql(@Param("list") List<TqStock> list);

    /**
     * 根据库存日期字符串（yyyy-MM-dd）逻辑删除胎圈库存
     * 只删除指定库存日期的数据，历史数据保留
     * 使用字符串日期比较，规避JVM时区与MySQL时区不一致导致的Date参数偏移
     *
     * @param stockDateStr 库存日期字符串，格式yyyy-MM-dd
     * @param updateBy     更新者
     * @param updateTime   更新时间
     * @return 更新的记录数
     */
    int logicDeleteByStockDate(@Param("stockDateStr") String stockDateStr,
                               @Param("updateBy") String updateBy,
                               @Param("updateTime") Date updateTime);

    /**
     * MES库存同步专用批量插入
     * 显式指定CREATE_BY/UPDATE_BY等值，绕过MyBatis-Plus MetaObjectHandler自动填充，
     * 确保Feign调用上下文（syncUser）下审计字段仍为"MES"
     *
     * @param list 待插入的胎圈库存列表
     */
    void batchInsertMesStock(@Param("list") List<TqStock> list);
}
