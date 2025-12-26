package com.zlt.aps.cd15.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15LineSideStock;
import com.zlt.aps.cd15.mapper.Cd15LineSideStockMapper;
import com.zlt.aps.cd15.service.Cd15LineSideStockService;
import com.zlt.sync.api.service.ISyncDataLogsApiService;

/**
 * 15°裁断库存信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-31
 */
@Service
public class Cd15LineSideStockServiceImpl implements Cd15LineSideStockService {
    @Autowired
    private Cd15LineSideStockMapper cd15LineSideStockMapper;
    @Autowired
    private ISyncDataLogsApiService syncDataLogsService;

    /**
     * 查询15°裁断库存信息列表
     *
     * @param Cd15Stock 15°裁断库存信息
     * @return 15°裁断库存信息
     */
    @Override
    public List<Cd15LineSideStock> selectStockList(Cd15LineSideStock stock) {
        if (StringUtils.isNotEmpty(stock.getEndTime())) {
            stock.setEndTime(stock.getEndTime() + " 23:59:59");
        }
        return cd15LineSideStockMapper.selectStockList(stock);
    }

}
