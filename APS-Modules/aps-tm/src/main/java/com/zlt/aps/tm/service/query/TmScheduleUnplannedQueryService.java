package com.zlt.aps.tm.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.entity.TmScheduleUnplanned;
import com.zlt.aps.tm.api.domain.vo.TmScheduleUnplannedQueryVo;
import com.zlt.aps.tm.mapper.TmScheduleUnplannedMapper;
import org.springframework.stereotype.Service;

/**
 * 胎面未排任务查询服务。
 */
@Service
public class TmScheduleUnplannedQueryService {

    private final TmScheduleUnplannedMapper tmScheduleUnplannedMapper;

    /**
     * 创建胎面未排任务查询服务。
     *
     * @param tmScheduleUnplannedMapper 胎面未排任务 Mapper
     */
    public TmScheduleUnplannedQueryService(TmScheduleUnplannedMapper tmScheduleUnplannedMapper) {
        this.tmScheduleUnplannedMapper = tmScheduleUnplannedMapper;
    }

    /**
     * 分页查询胎面未排任务。
     *
     * @param queryVo 查询条件，工厂和排程日期必填，批次号可选
     * @return 未排任务分页结果
     * @throws ServiceException 查询范围不完整时抛出
     */
    public Page<TmScheduleUnplanned> listUnplanned(TmScheduleUnplannedQueryVo queryVo) {
        this.validateQuery(queryVo);
        int pageNum = queryVo.getPageNum() == null || queryVo.getPageNum() < 1 ? 1 : queryVo.getPageNum();
        int pageSize = queryVo.getPageSize() == null || queryVo.getPageSize() < 1 ? 20 : queryVo.getPageSize();
        LambdaQueryWrapper<TmScheduleUnplanned> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleUnplanned::getFactoryCode, queryVo.getFactoryCode());
        wrapper.eq(TmScheduleUnplanned::getScheduleDate, queryVo.getScheduleDate());
        wrapper.eq(StringUtils.isNotBlank(queryVo.getBatchNo()), TmScheduleUnplanned::getBatchNo, queryVo.getBatchNo());
        wrapper.orderByAsc(TmScheduleUnplanned::getScheduleDate, TmScheduleUnplanned::getBatchNo,
                TmScheduleUnplanned::getTreadCode, TmScheduleUnplanned::getId);
        return this.tmScheduleUnplannedMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 校验未排任务查询范围。
     *
     * @param queryVo 查询条件
     * @throws ServiceException 工厂或排程日期为空时抛出
     */
    private void validateQuery(TmScheduleUnplannedQueryVo queryVo) {
        if (queryVo == null || StringUtils.isBlank(queryVo.getFactoryCode()) || queryVo.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tm.schedule.unplannedQueryRequired"));
        }
    }
}
