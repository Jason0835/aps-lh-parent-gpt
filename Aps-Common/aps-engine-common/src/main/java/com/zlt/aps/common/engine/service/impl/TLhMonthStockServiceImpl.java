package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.TLhMonthStock;
import com.zlt.aps.common.engine.mapper.TLhMonthStockMapper;
import com.zlt.aps.common.engine.service.TLhMonthStockService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class TLhMonthStockServiceImpl implements TLhMonthStockService {
    
    @Resource
    private TLhMonthStockMapper mapper;


    @Override
    public List<TLhMonthStock> getByParams(TLhMonthStock entity) {
        return mapper.getByParams(entity);
    }

    @Override
    public List<TLhMonthStock> selectBySapCodeAndMonth(List<String> codeList, String month) {
        if (CollectionUtil.isEmpty(codeList)) {
            return new ArrayList<>();
        }
        return mapper.selectBySapCodeAndMonth(codeList, month);
    }

    @Override
    public void mergeSql(List<TLhMonthStock> list) {
        mapper.mergeSql(list);
    }

}
