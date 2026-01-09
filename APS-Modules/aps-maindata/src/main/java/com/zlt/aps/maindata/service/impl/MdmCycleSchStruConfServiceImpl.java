package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.text.Convert;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.enums.OperationBusinessEnums;
import com.zlt.aps.maindata.mapper.MdmCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMonCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.service.IMdmCycleSchStruConfService;
import com.zlt.aps.maindata.utils.RemoteImportExcelUtils;
import com.zlt.aps.monthplan.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.aps.monthplan.api.service.IRemoteImportErrorLogService;
import com.zlt.aps.monthplan.api.service.IRemoteImportLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmCycleSchStruConfServiceImpl.java
 * 描    述：MdmCycleSchStruConfServiceImpl周期排产结构配置业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MdmCycleSchStruConfServiceImpl extends AbstractDocService<MdmCycleSchStruConf> implements IMdmCycleSchStruConfService {
    private final RedisService redisService;
    private final MdmCycleSchStruConfEntityMapper mdmCycleSchStruConfEntityMapper;
    private final MdmMonCycleSchStruConfEntityMapper monCycleSchStruConfEntityMapper;

    @Autowired
    private IRemoteImportLogService iRemoteImportLogService;

    @Autowired
    private IRemoteImportErrorLogService iRemoteImportErrorLogService;

    @Override
    protected String getDocTypeCode() {
        return "MDM0142";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0142");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmCycleSchStruConf docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmCycleSchStruConf.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "structureName"));
    }

    /**
     * 异步导入
     */
    @Async
    @Override
    public void importDataAsync(List<MdmCycleSchStruConf> list, boolean updateSupport, Long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes) {
        try {
            RequestContextHolder.setRequestAttributes(attributes, true);

            AjaxResult result = this.importData(list, updateSupport, importLogId);
            Date endTime = DateUtils.getNowDate();
            importLog.setRowCount(list.size());
            importLog.setBeginTime(beginTime);
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
            RemoteImportExcelUtils.updateImportLogAndFormatMsg(importLog, result, iRemoteImportLogService);
            RemoteImportExcelUtils.saveImportErrorLogs(result, iRemoteImportErrorLogService);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * 生成月周期排产结构配置
     *
     * @param mdmCycleSchStruConf 参数
     * @return 结果
     */
    @Override
    public AjaxResult genMonthCycleSchStruConf(MdmCycleSchStruConf mdmCycleSchStruConf) {
        /*
        1、用户选择年、月，检查月周期排产结构是否正在生成
                (1) 检查维度：操作业务：CREATE_MONTH_CYCLE_CTRUCTRUE + 年、月
                (2) 若在，提示信息："xxx年-xx月的周期排产结构正在生成，请稍候"
        2、根据周期性排产结构配置表，获取周期性结构排产各自最近的排产年、月(通过月度排产计划表)；
        根据周转月数，判断在选择的年、月结构是否需要进行周期性排产
        如：12.00R20-JD727结构，配置的周转月数为2，该结构最近的排产年月为2025-09月，则当制作2025-11月计划时，则该结构可生成周期性排产订单
        若该结构最近的排产年月为2025-08月，则当制作2025-11月计划时，该结构不生成周期性排产订单
        3、得到可周期性排产的结构，写入月周期排产结构配置表中
                (1) 先删除年份、月份的对应周期排产结构数据
                (2) 在新增年份、月份的最新周期排产结构数据
        4、操作成功后，提示信息："月周期排产结构生成成功"
        */
        Integer year = mdmCycleSchStruConf.getYear();
        Integer month = mdmCycleSchStruConf.getMonth();
        String value = Convert.toStr(redisService.getCacheObject(OperationBusinessEnums.CREATE_MONTH_CYCLE_STRUCTURE.getCode() + "_" + year + "_" + month));
        if (StringUtils.isNotBlank(value)) {
            throw new RuntimeException(String.format(I18nUtil.getMessage("ui.data.alert.mdmCycleSchStruConf.generating"), year, month));
        }
        // 查询月度排产计划表关联查询出 对应参数月份-配置月份的数据
        List<MdmMonCycleSchStruConf> resultList = monCycleSchStruConfEntityMapper.selectMonthCycleSchStruConf(mdmCycleSchStruConf);
        if (CollectionUtils.isNotEmpty(resultList)) {
            LambdaUpdateWrapper<MdmMonCycleSchStruConf> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(MdmMonCycleSchStruConf::getYear, year)
                    .eq(MdmMonCycleSchStruConf::getMonth, month)
                    .set(BaseEntity::getIsDelete, ApsConstant.DEL_FLAG_DEL);
            monCycleSchStruConfEntityMapper.update(null, updateWrapper);
            baseDao.saveBatch(resultList);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.data.alert.mdmCycleSchStruConf.generateDataNull"));
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.data.alert.mdmCycleSchStruConf.generateSuccess"));
    }

    @Override
    public List<MdmCycleSchStruConf> findCycleSchStruConf() {
        LambdaQueryWrapper<MdmCycleSchStruConf> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmCycleSchStruConf::getIsDelete, YesOrNoEnum.NO.getValue());
        return mdmCycleSchStruConfEntityMapper.selectList(wrapper);
    }
}
