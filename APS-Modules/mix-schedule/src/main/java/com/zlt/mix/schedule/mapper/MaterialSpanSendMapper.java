package com.zlt.mix.schedule.mapper;

import java.util.Date;
import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;
import org.apache.ibatis.annotations.Param;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanSend;

/**
 * 硫磺辅料跨区发送Mapper接口
 *
 * @author cxy
 * @date 2022-08-30
 */
public interface MaterialSpanSendMapper extends BaseMapper<MaterialSpanSend> {

    /**
     * 根据条件查询硫磺辅料日计划跨区发送列表
     *
     * @param entity 查询条件
     * @return 结果
     */
    List<MaterialSpanSend> listMaterialSpanSend(MaterialSpanSend entity);

    /**
     * 批量新增跨区发送请求记录
     * @param materialSpanSendList 要批量保存的记录
     * @return 影响行数
     */
    int batchInsertMaterialSpanSend(@Param("list") List<MaterialSpanSend> materialSpanSendList);

    /**
     * 批量更新跨区发送记录,仅更新发布状态，更新人，更新时间，通过接收表的 send_id关联更新
     * @param receiveList 批量更新的记录
     * @return 影响行数
     */
    int mergeMaterialSpanSend(@Param("list") List<MaterialSpanReceive> receiveList);

    /**
     * 查询出批量新增的数据中，在系统中已经存在的数据
     * @param list 要校验的数据列表
     * @return 结果
     */
    List<ImportErrorLog> listMaterialSpanSendNotUnique(@Param("list") List<MaterialSpanSend> list);

    /**
     * 根据id查询已接收的记录数
     * @param ids id
     * @return 已接收记录数
     */
    Integer getAlreadyReceivedCount(Long[] ids);

    /**
     * 根据id删除发送记录
     * @param ids id
     * @return 结果
     */
    int deleteByIds(Long[] ids);

    /**
     *  删除还未接收的跨区发送记录
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     */
    void deleteNotReceived(@Param("mixArea") String mixArea, @Param("scheduleDate") Date scheduleDate);
}
