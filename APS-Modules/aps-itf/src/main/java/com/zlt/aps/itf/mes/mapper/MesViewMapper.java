package com.zlt.aps.itf.mes.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MdmUnqualifiedStock;
import com.zlt.aps.monthplan.api.domain.entity.RawMaterialOutboundRecord;
import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialStock;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES视图Mapper
 *
 * @author Chen
 * @since 2026/1/4
 */
@Mapper
public interface MesViewMapper {

    /**
     * 查询成品库存列表
     *
     * @param productStockMonth 查询参数
     * @return 结果
     */
    List<MdmProductStock> selectProductStock(MdmProductStock productStockMonth);

    /**
     * 查询不合格库存列表
     *
     * @param productStockMonth 查询参数
     * @return 列表
     */
    List<MdmUnqualifiedStock> selectUnqualifiedStock(MdmUnqualifiedStock productStockMonth);

    /**
     * 查询原材料库存列表
     *
     * @param rawSpecialMaterialStock 查询参数
     * @return 列表
     */
    List<RawSpecialMaterialStock> selectRawSpecialMaterialStock(RawSpecialMaterialStock rawSpecialMaterialStock);

    /**
     * 获取原材料出库记录列表
     *
     * @param materialOutboundRecord 同步数据日志
     * @return 列表
     */
    List<RawMaterialOutboundRecord> syncRawMaterialOutboundRecord(RawMaterialOutboundRecord materialOutboundRecord);

}
