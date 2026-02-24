package com.zlt.aps.lh.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.lh.api.domain.entity.LhTestNewTable;
import com.zlt.aps.lh.mapper.LhTestNewTableEntityMapper;
import com.zlt.aps.lh.service.LhTestNewTableService;
import com.zlt.core.dao.basedao.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.lh.api.domain.dto.AutoLhScheduleResultDTO;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/2/13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhTestNewTableServiceImpl extends AbstractDocService<LhTestNewTable> implements LhTestNewTableService {

    @Autowired
    private LhTestNewTableEntityMapper lhTestNewTableEntityMapper;
    @Autowired
    private BaseDao baseDao;

    @Override
    public void updateEmbryoCode(AutoLhScheduleResultDTO autoLhScheduleResultDTO) throws BusinessException {
        QueryWrapper<LhTestNewTable> testNewTableQueryWrapper = new QueryWrapper<>();
        List<LhTestNewTable> list = lhTestNewTableEntityMapper.selectList(testNewTableQueryWrapper);
        if (PubUtil.isEmpty(list)){
            return;
        }
        List<LhTestNewTable> resultList = new ArrayList<>();
        LhTestNewTable testNewTable1;
        for(LhTestNewTable testNewTable:list){
            String[] arr = testNewTable.getCol2().split("/");
            if (arr == null || arr.length == 0){
                continue;
            }
            for (int i=0;i < arr.length; i++){
                testNewTable1 = new LhTestNewTable();
                testNewTable1.setEmbryoCode(testNewTable.getEmbryoCode());
                testNewTable1.setCol2(arr[i]);
                testNewTable1.setCol3("XX");
                testNewTable1.setCol6("new");
                resultList.add(testNewTable1);
            }
        }
        baseDao.insertBatch(resultList);
        //lhTestNewTableEntityMapper.updateEmbryoCode1();
    }

    @Override
    public String getDocTypeCode() {
        return "OUT2046";
    }
}
