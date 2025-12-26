package com.zlt.aps.cd90.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd90.api.domain.entity.Cd90LineSideStock;
import com.zlt.aps.cd90.mapper.Cd90LineSideStockMapper;
import com.zlt.aps.cd90.service.Cd90LineSideStockService;

/**
 * 90°裁断库存信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-31
 */
@Service
public class Cd90LineSideStockServiceImpl implements Cd90LineSideStockService {
    @Autowired
    private Cd90LineSideStockMapper cd90LineSideStockMapper;
    
    /**
     * 查询90°裁断库存信息列表
     *
     * @param Cd90Stock 90°裁断库存信息
     * @return 90°裁断库存信息
     */
    @Override
    public List<Cd90LineSideStock> selectStockList(Cd90LineSideStock stock) {
        if (StringUtils.isNotEmpty(stock.getEndTime())) {
            stock.setEndTime(stock.getEndTime() + " 23:59:59");
        }
        return cd90LineSideStockMapper.selectStockList(stock);
    }
}
