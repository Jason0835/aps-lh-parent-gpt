package com.zlt.aps.monthplan.common.utils;

import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预测上下文
 * @author Yelq
 */
@Data
public class PredictionContext {
  private List<SalesOrderPool> salesOrders;
  private  List<MdmProductStock> finishedProductStocks;
  private Map<String, List<MdmProductStock>> finishedProductStockMap;
  private  Map<String, String> productionTypeMap;
  private  List<SupplyOrderPool> supplyOrderPools;
  private  List<SalesOrderPool> allocationOrders;
  private  List<SalesOrderPool> postponeOrders;
  private  Map<String, Integer> monthSurplusMap;
  private  Map<String, Integer>  monthlySaleQty;
  private  Integer  minProductionQty;
  private  Map<String, MdmMaterialInfo> materialInfoMap;
  private  OrderAllocationResult allocationResult;

  public PredictionContext(
      List<SalesOrderPool> salesOrders,
      List<MdmProductStock> finishedProductStocks,
      Map<String, List<MdmProductStock>> finishedProductStockMap,
      Map<String, String> productionTypeMap,
      List<SupplyOrderPool> supplyOrderPools,
      List<SalesOrderPool> allocationOrders,
      List<SalesOrderPool> postponeOrders,
      Map<String, Integer> monthSurplusMap,
      Map<String, Integer>  monthlySaleQty,
      Integer  minProductionQty,
      Map<String, MdmMaterialInfo> materialInfoMap) {
    this.salesOrders = salesOrders != null ? salesOrders : Collections.emptyList();
    this.finishedProductStocks = finishedProductStocks != null ? finishedProductStocks : Collections.emptyList();
    this.finishedProductStockMap = finishedProductStockMap != null ? finishedProductStockMap : new HashMap<>();
    this.productionTypeMap = productionTypeMap != null ? productionTypeMap : new HashMap<>();
    this.supplyOrderPools = supplyOrderPools != null ? supplyOrderPools : Collections.emptyList();
    this.allocationOrders = allocationOrders != null ? allocationOrders : Collections.emptyList();
    this.postponeOrders = postponeOrders != null ? postponeOrders : Collections.emptyList();
    this.monthSurplusMap = monthSurplusMap != null ? monthSurplusMap : new HashMap<>();
    this.monthlySaleQty = monthlySaleQty != null ? monthlySaleQty : new HashMap<>();
    this.minProductionQty = minProductionQty != null ? minProductionQty : 0;
    this.materialInfoMap = materialInfoMap != null ? materialInfoMap : new HashMap<>();
  }

  /**
   * 订单分配结果
   */
  @Data
  public static class OrderAllocationResult {
    private  List<DpOrderOffsetDetail> allocations;
    private  List<DpOrderOffsetDetail> netDemands;
    private  Map<String, List<MdmProductStock>> stockMap;

    public OrderAllocationResult(
        List<DpOrderOffsetDetail> allocations,
        List<DpOrderOffsetDetail> netDemands,
        Map<String, List<MdmProductStock>> stockMap) {
      this.allocations = allocations != null ? allocations : Collections.emptyList();
      this.netDemands = netDemands != null ? netDemands : Collections.emptyList();
      this.stockMap = stockMap != null ? stockMap : new HashMap<>();
    }
  }

}
