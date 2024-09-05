package com.zlt.aps.mps.mapper;
import org.apache.ibatis.annotations.Param;
import com.zlt.aps.mps.domain.TMesStockBaseEntity;

import com.zlt.aps.mps.domain.*;

import java.util.List;

/**
 * @Entity com.zlt.aps.mps.domain.TMesTmStock
 */
public interface TMesStockBaseMapper {

    List<TMesStockBaseEntity> getTmByDataVersion(@Param("dataVersion") String dataVersion);
    void mergeTmSql(List<BaseStock> list);

    List<TMesStockBaseEntity> getTcByDataVersion(@Param("dataVersion") String dataVersion);
    void mergeTcSql(List<BaseStock> list);

    List<TMesStockBaseEntity> getNcByDataVersion(@Param("dataVersion") String dataVersion);
    void mergeNcSql(List<BaseStock> list);

    List<TMesStockBaseEntity> getTqByDataVersion(@Param("dataVersion") String dataVersion);
    void mergeTqSql(List<BaseStock> list);

    List<TMesStockBaseEntity> getGsqByDataVersion(@Param("dataVersion") String dataVersion);
    void mergeGsqSql(List<BaseStock> list);

    List<TMesStockBaseEntity> getGdyyByDataVersion(@Param("dataVersion") String dataVersion);
    void mergeGdyySql(List<BaseStock> list);

    List<TMesStockBaseEntity> getXwyyByDataVersion(@Param("dataVersion") String dataVersion);
    void mergeXwyySql(List<BaseStock> list);

    List<TMesCdBaseEntity> getCd15ByDataVersion(@Param("dataVersion") String dataVersion);
    void mergeCd15Sql(List<BaseStock> list);

    List<TMesCdBaseEntity> getCd15LineSideByDataVersion(@Param("dataVersion") String dataVersion);
    void mergeCd15LineSideSql(List<TMesCdBaseEntity> list);

    List<TMesCdBaseEntity> getCd90ByDataVersion(@Param("dataVersion") String dataVersion);
    void mergeCd90Sql(List<BaseStock> list);

    List<TMesCdBaseEntity> getCd90LineSideByDataVersion(@Param("dataVersion") String dataVersion);
    void mergeCd90LineSideSql(List<TMesCdBaseEntity> list);

}




