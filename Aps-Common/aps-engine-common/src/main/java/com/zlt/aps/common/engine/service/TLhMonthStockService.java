package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.TLhMonthStock;

import java.util.List;

/**
 * @author Gim
 */
public interface TLhMonthStockService {

    List<TLhMonthStock> getByParams(TLhMonthStock entity);

    List<TLhMonthStock> selectBySapCodeAndMonth(List<String> codeList, String month);

    void mergeSql(List<TLhMonthStock> list);
}
