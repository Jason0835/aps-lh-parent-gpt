/**
 * @Description:  获取baseData数据
 * @Author: qy
 * @Date: 2024/2/1
 **/

// import {
//   listOutBdTradeMode,
// } from "@/api/bd/outBdTradeMode";
// import { listDept } from "@/api/system/dept";


export const baseDataRequest = {
  // 'BASE_DATA_UNIT': listUnit,
  // 'BASE_DATA_LIST_COMPANY': listCompany,
  // 'BASE_DATA_LIST_GOODS_TYPE':listGoodsType,
  // 'BASE_DATA_LIST_LOAN_AGENCY_COMPANY': listLoanAgencyCompany,
  // 'BASE_DATA_LIST_OUT_BANK': listOutBankAccount,
  // 'BASE_DATA_LIST_SETTLE_PATH': listSettlePath,
  // 'BASE_DATA_LIST_TRD_AUTHENTICATE_ALL': listTrdAuthenticateAll,
  // 'BASE_DATA_LIST_WAREHOUSE': listWarehouseSelect,
  // // 'BASE_DATA_TRADE_MODE': listOutBdTradeMode,
  // 'BASE_DATA_LIST_SHIPPING_COMPANY': listShippingCompany,
}

export function getBaseData(type) {
  if (baseDataRequest[type]) {
    return baseDataRequest[type]()
  }
}
