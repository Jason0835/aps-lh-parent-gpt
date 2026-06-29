package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zlt.aps.tq.api.domain.entity.TqWarningRecord;
import com.zlt.aps.tq.mapper.TqWarningRecordMapper;
import com.zlt.aps.tq.service.ITqWarningRecordService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 胎圈排程预警记录Service实现类
 *
 * <p>实现预警记录的增删改查、批量保存和处理状态更新。</p>
 *
 * @author APS
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TqWarningRecordServiceImpl extends AbstractDocService<TqWarningRecord> implements ITqWarningRecordService {

    @Resource
    private TqWarningRecordMapper tqWarningRecordMapper;

    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "TQ_WARNING_RECORD";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TQ_WARNING_RECORD");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 预警记录无需唯一性校验
        return Collections.emptyList();
    }

    /**
     * 批量保存预警记录
     *
     * @param warningRecords 预警记录列表
     * @return 保存成功的记录数
     */
    @Override
    public int saveBatchWarningRecords(List<TqWarningRecord> warningRecords) {
        if (warningRecords == null || warningRecords.isEmpty()) {
            return 0;
        }
        // 使用BaseDao批量保存
        baseDao.saveBatch(warningRecords);
        return warningRecords.size();
    }

    /**
     * 处理预警记录（更新处理状态、处理人和处理意见）
     *
     * @param id       预警记录ID
     * @param handler  处理人
     * @param opinion  处理意见
     * @return 操作结果
     */
    @Override
    public int handleWarning(Long id, String handler, String opinion) {
        LambdaUpdateWrapper<TqWarningRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TqWarningRecord::getId, id)
               .set(TqWarningRecord::getStatus, "1")
               .set(TqWarningRecord::getHandler, handler)
               .set(TqWarningRecord::getHandleTime, new Date())
               .set(TqWarningRecord::getHandleOpinion, opinion);
        return tqWarningRecordMapper.update(null, wrapper);
    }
}
