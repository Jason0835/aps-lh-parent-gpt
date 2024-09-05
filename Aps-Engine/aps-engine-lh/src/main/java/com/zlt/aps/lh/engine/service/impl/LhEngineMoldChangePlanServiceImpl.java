package com.zlt.aps.lh.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.lh.engine.constants.LhEngineConstants;
import com.zlt.aps.lh.engine.domain.LhEngineMoldChangePlan;
import com.zlt.aps.lh.engine.domain.MoldEngineAutoGenerageRecord;
import com.zlt.aps.lh.engine.mapper.LhEngineMoldChangePlanMapper;
import com.zlt.aps.lh.engine.mapper.MoldEngineAutoGenerageRecordMapper;
import com.zlt.aps.lh.engine.service.LhEngineMoldChangePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模具变动单Service业务层处理
 *
 * @author zlt
 * @date 2021-06-17
 */
@Service("lhEngineMoldChangePlanService")
public class LhEngineMoldChangePlanServiceImpl implements LhEngineMoldChangePlanService {
    @Autowired
    private LhEngineMoldChangePlanMapper lhEngineMoldChangePlanMapper;

    @Autowired
    private MoldEngineAutoGenerageRecordMapper moldEngineAutoGenerageRecordMapper;

    /**
     * 查询模具变动单
     *
     * @param id 模具变动单ID
     * @return 模具变动单
     */
    @Override
    public LhEngineMoldChangePlan selectLhEngineMoldChangePlanById(Long id) {
        return lhEngineMoldChangePlanMapper.selectLhEngineMoldChangePlanById(id);
    }

    /**
     * 查询模具变动单列表
     *
     * @param lhEngineMoldChangePlan 模具变动单
     * @return 模具变动单
     */
    @Override
    public List<LhEngineMoldChangePlan> selectLhEngineMoldChangePlanList(LhEngineMoldChangePlan lhEngineMoldChangePlan) {
        return lhEngineMoldChangePlanMapper.selectLhEngineMoldChangePlanList(lhEngineMoldChangePlan);
    }

    /**
     * 新增模具变动单
     *
     * @param lhEngineMoldChangePlan 模具变动单
     * @return 结果
     */
    @Override
    public int insertLhEngineMoldChangePlan(LhEngineMoldChangePlan lhEngineMoldChangePlan) {
        lhEngineMoldChangePlan.setCreateTime(DateUtils.getNowDate());
        return lhEngineMoldChangePlanMapper.insertLhEngineMoldChangePlan(lhEngineMoldChangePlan);
    }

    /**
     * 修改模具变动单
     *
     * @param lhEngineMoldChangePlan 模具变动单
     * @return 结果
     */
    @Override
    public int updateLhEngineMoldChangePlan(LhEngineMoldChangePlan lhEngineMoldChangePlan) {
        lhEngineMoldChangePlan.setUpdateTime(DateUtils.getNowDate());
        return lhEngineMoldChangePlanMapper.updateLhEngineMoldChangePlan(lhEngineMoldChangePlan);
    }

    /**
     * 批量删除模具变动单
     *
     * @param ids 需要删除的模具变动单ID
     * @return 结果
     */
    @Override
    public int deleteLhEngineMoldChangePlanByIds(Long[] ids) {
        return lhEngineMoldChangePlanMapper.deleteLhEngineMoldChangePlanByIds(ids);
    }

    /**
     * 删除模具变动单信息
     *
     * @param id 模具变动单ID
     * @return 结果
     */
    @Override
    public int deleteLhEngineMoldChangePlanById(Long id) {
        return lhEngineMoldChangePlanMapper.deleteLhEngineMoldChangePlanById(id);
    }

    /**
     * 批量生成模具变动单数据
     * @param lhEngineMoldChangePlanList
     * @return
     */
    @Override
    public int batchCreateMoldChangePlan(List<LhEngineMoldChangePlan> lhEngineMoldChangePlanList) {
        return lhEngineMoldChangePlanMapper.batchCreateMoldChangePlan(lhEngineMoldChangePlanList);
    }

    @Override
    public int deleteLhEngineMoldChangePlanByCxBatchNo(String cxBatchNo) {
        return lhEngineMoldChangePlanMapper.deleteLhEngineMoldChangePlanByCxBatchNo(cxBatchNo);
    }

    /**
     * 根据参数进行模具变动单数据删除
     * @param sourceCxOrder 成型工单号
     * @param list  原始硫化机台编码
     * @return
     */
    @Override
    public int deleteLhEngineMoldChangePlanByParams(String sourceCxOrder, List<String> list,List<Long> idList,String cxBatchNo,String moldBatchNo) {
        //1.先进行模具变动单数据转日志表
        lhEngineMoldChangePlanMapper.syncMoldChagePlanToLog(sourceCxOrder,list,idList,cxBatchNo,moldBatchNo);
        //2.模具变动单数据删除
        return lhEngineMoldChangePlanMapper.deleteLhEngineMoldChangePlanByParams(sourceCxOrder,list,idList,cxBatchNo,moldBatchNo);
    }

    /**
     *  根据模具变动单生成日期进行当前日期对应的模具变动单数据删除
     * @param scheduleDate
     * @return
     */
    @Override
    public int deleteLhEngineMoldChangePlanByScheduleDate(String scheduleDate) {
        int result=0;
       //1.根据模具变动单日期找到对应的模具变动单生成记录
        MoldEngineAutoGenerageRecord recordCondition=new MoldEngineAutoGenerageRecord();
        recordCondition.setAutoScheduleDate(scheduleDate);//当前日期模具变动单生成记录
        recordCondition.setStatus(LhEngineConstants.LH_AUTO_RECORD_STATUS_SUCCESS);
        List<MoldEngineAutoGenerageRecord> list =moldEngineAutoGenerageRecordMapper.selectMoldEngineAutoGenerageRecordList(recordCondition);
        if(StringUtils.isNotEmpty(list)){
            MoldEngineAutoGenerageRecord record=list.get(0);
            lhEngineMoldChangePlanMapper.syncMoldChagePlanToLog(null,null,null,null,record.getMoldBatchNo());
            result=lhEngineMoldChangePlanMapper.deleteLhEngineMoldChangePlanByParams(null,null,null,null,record.getMoldBatchNo());
        }
        return result;
    }


}
