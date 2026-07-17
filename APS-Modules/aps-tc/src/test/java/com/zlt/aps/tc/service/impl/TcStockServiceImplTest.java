package com.zlt.aps.tc.service.impl;

import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.mapper.TcStockMapper;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Date;

/**
 * 胎侧MES库存快照版本幂等测试。
 */
public class TcStockServiceImplTest {

    /**
     * 相同MES数据版本重复同步时，不应失效并重建库存快照。
     */
    @Test
    public void shouldSkipSnapshotReplacementWhenDataVersionUnchanged() {
        TcStockMapper stockMapper = Mockito.mock(TcStockMapper.class);
        TcStock currentStock = new TcStock();
        currentStock.setDataVersion("TC-STOCK-001");
        Mockito.when(stockMapper.selectList(ArgumentMatchers.any()))
                .thenReturn(Collections.singletonList(currentStock));
        TcStock incomingStock = new TcStock();
        incomingStock.setDataVersion("TC-STOCK-001");

        new TcStockServiceImpl(stockMapper).logicDeleteAndSaveBatch(
                "116", new Date(0L), "MES", Collections.singletonList(incomingStock));

        Mockito.verify(stockMapper, Mockito.never()).update(
                ArgumentMatchers.isNull(), ArgumentMatchers.any());
    }
}
