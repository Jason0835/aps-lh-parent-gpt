package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.dto.LhInsertOrderValidateResultDTO;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;

import java.util.Date;
import java.util.List;

/**
 * 硫化排程结果服务接口
 *
 * @author APS
 */
public interface ILhScheduleResultService {

    /**
     * 根据排程日期和工厂查询排程结果
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编号
     * @return 排程结果列表
     */
    List<LhScheduleResult> selectByDateAndFactory(Date scheduleDate, String factoryCode);

    /**
     * 查询前日排程结果
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编号
     * @return 前日排程结果列表
     */
    List<LhScheduleResult> selectPreviousSchedule(Date scheduleDate, String factoryCode);

    /**
     * 根据排程日期和工厂删除排程结果（仅删除 {@code isDelete = 0} 的记录）
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编号
     * @return 删除记录数
     */
    int deleteByDateAndFactory(Date scheduleDate, String factoryCode);

    /**
     * 批量插入排程结果
     *
     * @param list 排程结果列表
     * @return 插入记录数
     */
    int insertBatch(List<LhScheduleResult> list);

    /**
     * 检查排程日期是否已下发 MES：仅统计 {@code isRelease = 1}（已发布）且 {@code isDelete = 0}（{@link com.zlt.aps.lh.api.enums.DeleteFlagEnum#NORMAL}）的记录数
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编号
     * @return 已发布记录数，大于 0 表示该日该厂存在已下发 MES 的排程结果
     */
    int countReleasedByDate(Date scheduleDate, String factoryCode);

    /**
     * 生成下一个排程批次号（LHPC+yyyyMMdd+流水），流水按目标排程日由 Redis 原子自增全局分配
     * （见 {@link com.zlt.aps.lh.component.LhBatchNoRedisGenerator}）
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编号
     * @return 新批次号
     */
    String generateNextBatchNo(Date scheduleDate, String factoryCode);

    /**
     * 更新排程结果发布状态
     *
     * @param item 排程结果（需包含id和isRelease）
     */
    void updateReleaseStatus(LhScheduleResult item);

    List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> getCxLhScheduleResultList(Date scheduleDate);

    /**
     * 根据ID列表查询硫化排程结果（cx-lh-api实体）
     *
     * @param ids 主键ID列表
     * @return 排程结果列表
     */
    List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> getCxLhScheduleResultListByIds(List<Long> ids);

    /**
     * 插单校验
     *
     * @param dto 插单请求数据
     * @return 校验结果
     */
    LhInsertOrderValidateResultDTO validateInsertOrder(LhOrderInsertDTO dto);

    /**
     * 获取SKU关联数据（硫化余量/胎胚库存/硫化班产/示方类型）
     * <p>用于插单页面选择新物料时实时获取关联信息</p>
     *
     * @param dto 包含factoryCode、materialCode、scheduleDate的请求对象
     * @return SKU关联数据
     */
    LhInsertOrderValidateResultDTO getSkuRelatedData(LhOrderInsertDTO dto);

    /**
     * 执行插单操作
     * <p>保存排程结果到硫化排程结果表，数据来源记录为插单，发布状态默认为待发布</p>
     *
     * @param dto 插单请求数据
     */
    void insertOrder(LhOrderInsertDTO dto);
}
