package com.zlt.mix.schedule.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;

/**
 * 硫磺辅料跨区接收Service接口
 *
 * @author cxy
 * @date 2022-08-30
 */
public interface MaterialSpanReceiveService extends IService<MaterialSpanReceive> {
    /**
     * 校验胶料跨区接收唯一性
     */
    String checkMaterialSpanReceiveUnique(MaterialSpanReceive materialSpanReceive);

    /**
     * 批量新增跨区接收请求记录
     *
     * @param materialSpanReceiveList 要批量保存的记录
     * @return 影响行数
     */
    int batchInsertMaterialSpanReceive(List<MaterialSpanReceive> materialSpanReceiveList);

    /**
     * 查询跨区接收列表
     *
     * @param entity 参数
     * @return 结果
     */
    List<MaterialSpanReceive> listMaterialSpanReceive(MaterialSpanReceive entity);

    /**
     * 根据id查询跨区接收信息
     *
     * @param entity id
     * @return 查询到的记录
     */
    MaterialSpanReceive getMaterialSpanReceiveInfo(MaterialSpanReceive entity);

    /**
     * 批量更新跨区接收记录
     *
     * @param receiveList 批量更新的记录
     * @return 影响行数
     */
    int mergeMaterialSpanReceive(List<MaterialSpanReceive> receiveList);

    /**
     * 根据排程日期、被委托密炼区查询未被接收的跨区请求总数
     *
     * @param materialSpanReceive 参数
     * @return 未接收的总数
     */
    Integer selectUnReceiveCount(MaterialSpanReceive materialSpanReceive);

    /**
     * 根据sendIds查询已接收的记录数
     *
     * @param sendIds sendIds
     * @return 已接收记录数
     */
    Integer getAlreadyReceivedCount(Long[] sendIds);

    /**
     * 根据Id查询已接收的记录数
     *
     * @param ids ids
     * @return 已接收记录数
     */
    Integer getAlreadyReceivedCountByIds(Long[] ids);

    /**
     * 根据send_id删除发送记录
     *
     * @param sendIds sendId
     * @return 结果
     */
    int deleteBySendIds(Long[] sendIds);


    /**
     *  删除还未接收的跨区接收记录
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     */
    void deleteNotReceived(String mixArea, Date scheduleDate);
}
