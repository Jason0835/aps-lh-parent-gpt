package com.zlt.aps.itf.mes.mapper;

import com.zlt.aps.monthplan.api.domain.entity.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES接口Mapper
 *
 * @author Chen
 * @since 2025/12/16
 */
@Mapper
public interface MesItfMapper {

    /**
     * 查询SKU与模具关系列表
     *
     * @param mdmSkuMouldRel 查询参数
     * @return 结果
     */
    List<MdmSkuMouldRel> selectSkuMouldRelList(MdmSkuMouldRel mdmSkuMouldRel);

    /**
     * 模具台账列表
     *
     * @param modelInfo 模具台账
     * @return 列表
     */
    List<MdmModelInfo> selectModelInfoList(MdmModelInfo modelInfo);

    /**
     * 查询成品库存列表
     *
     * @param productStockMonth 查询参数
     * @return 结果
     */
    List<ProductStockMonth> selectProductStock(ProductStockMonth productStockMonth);

    /**
     * 查询不合格库存列表
     *
     * @param productStockMonth 查询参数
     * @return 列表
     */
    List<MdmUnqualifiedStock> selectUnqualifiedStock(MdmUnqualifiedStock productStockMonth);

    /**
     * 查询不合格库存列表
     *
     * @param rawSpecialMaterialStock 查询参数
     * @return 列表
     */
    List<RawSpecialMaterialStock> selectRawSpecialMaterialStock(RawSpecialMaterialStock rawSpecialMaterialStock);
}
