package com.zlt.aps.monthplan.common.utils;

import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.DpSimulatedOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import lombok.Data;
import org.springframework.util.CollectionUtils;

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
  private Map<String,Integer> orderQtyMap;
  private  List<MdmProductStock> finishedProductStocks;
  private  Map<String, List<MdmProductStock>> finishedProductStockMap;
  private  Map<String, String> productionTypeMap;
  private  List<SupplyOrderPool> supplyOrderPools;
  private  List<SalesOrderPool> allocationOrders;
  private  List<SalesOrderPool> postponeOrders;
  private  Map<String, Integer> originalMonthSurplusMap;
  private  Map<String, Integer> monthSurplusMap;
  private  Map<String, Integer>  monthlySaleQty;
  private  Integer  minProductionQty;
  private  Map<String, MdmMaterialInfo> materialInfoMap;
  private  List<MdmCycleSchStruConf> cycleSchStruConfs;
  private  List<DpSimulatedOffsetDetail> predictOffsetDetails;

  public PredictionContext(
      List<SalesOrderPool> salesOrders,
      Map<String,Integer> orderQtyMap,
      List<MdmProductStock> finishedProductStocks,
      Map<String, List<MdmProductStock>> finishedProductStockMap,
      Map<String, String> productionTypeMap,
      List<SupplyOrderPool> supplyOrderPools,
      List<SalesOrderPool> allocationOrders,
      List<SalesOrderPool> postponeOrders,
      Map<String, Integer> initialData,
      Map<String, Integer>  monthlySaleQty,
      Integer  minProductionQty,
      Map<String, MdmMaterialInfo> materialInfoMap,
      List<MdmCycleSchStruConf> cycleSchStruConfs) {
    this.salesOrders = salesOrders != null ? salesOrders : Collections.emptyList();
    this.orderQtyMap = orderQtyMap != null ? orderQtyMap : Collections.emptyMap();
    this.finishedProductStocks = finishedProductStocks != null ? finishedProductStocks : Collections.emptyList();
    this.finishedProductStockMap = finishedProductStockMap != null ? finishedProductStockMap : Collections.emptyMap();
    this.productionTypeMap = productionTypeMap != null ? productionTypeMap : Collections.emptyMap();
    this.supplyOrderPools = supplyOrderPools != null ? supplyOrderPools : Collections.emptyList();
    this.allocationOrders = allocationOrders != null ? allocationOrders : Collections.emptyList();
    this.postponeOrders = postponeOrders != null ? postponeOrders : Collections.emptyList();
    this.originalMonthSurplusMap = originalMonthSurplusMap != null ? originalMonthSurplusMap : Collections.emptyMap();
    this.monthSurplusMap = monthSurplusMap != null ? monthSurplusMap : Collections.emptyMap();
    this.monthlySaleQty = monthlySaleQty != null ? monthlySaleQty : Collections.emptyMap();
    this.minProductionQty = minProductionQty != null ? minProductionQty : 0;
    this.materialInfoMap = materialInfoMap != null ? materialInfoMap : Collections.emptyMap();
    this.cycleSchStruConfs = cycleSchStruConfs != null ? cycleSchStruConfs : Collections.emptyList();
    if(!CollectionUtils.isEmpty(initialData)) {
      // 深度拷贝：创建新的HashMap，确保与原始数据隔离
      this.originalMonthSurplusMap = Collections.unmodifiableMap(
          new HashMap<>(initialData)
      );
      // 工作Map是原始数据的可修改副本
      this.monthSurplusMap = new HashMap<>(this.originalMonthSurplusMap);
    }else{
      this.originalMonthSurplusMap = Collections.emptyMap();
      this.monthSurplusMap = Collections.emptyMap();
    }

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
