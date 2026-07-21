package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.mapper.TcStockMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Date;

/**
 * 胎侧库存快照替换测试。
 */
public class TcStockServiceImplTest {

    /**
     * 初始化 MyBatis-Plus 实体元数据，支持单元测试构造 Lambda 更新条件。
     */
    @BeforeClass
    public static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TcStock.class);
    }

    /**
     * MES返回空库存时，仍应失效指定工厂和日期的旧快照。
     */
    @Test
    public void shouldInvalidateOldSnapshotWhenIncomingStockIsEmpty() {
        TcStockMapper stockMapper = Mockito.mock(TcStockMapper.class);

        new TcStockServiceImpl(stockMapper).logicDeleteAndSaveBatch(
                "116", new Date(0L), "MES", Collections.emptyList());

        Mockito.verify(stockMapper).update(
                ArgumentMatchers.isNull(), ArgumentMatchers.any());
    }
}
