package com.zlt.aps.lh.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxParams;
import com.zlt.aps.lh.api.domain.entity.LhTestNewTable;
import com.zlt.aps.lh.mapper.LhTestNewTableEntityMapper;
import com.zlt.aps.lh.service.LhTestNewTableService;
import com.zlt.core.dao.basedao.BaseDao;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.CommonRedisService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.constants.LhPrefixConstants;
import com.zlt.aps.lh.api.domain.dto.AutoLhScheduleResultDTO;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhTestScheduleResult;
import com.zlt.aps.lh.mapper.LhMoldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultEntityMapper;
import com.zlt.aps.lh.mapper.LhTestScheduleResultEntityMapper;
import com.zlt.aps.lh.mapper.LhUnscheduledResultEntityMapper;
import com.zlt.aps.lh.service.LhTestScheduleResultService;
import com.zlt.aps.maindata.mapper.MdmProductConstructionEntityMapper;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.monthplan.api.domain.vo.MdmProductConstructionVO;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;

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
