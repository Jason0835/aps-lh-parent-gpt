package com.zlt.mix.schedule.service;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;

import java.util.List;
import java.util.Set;

/**
 * 用户资源权限Service接口
 *
 * @author Liam
 * @date 2022-07-12
 */
public interface PermissionService {

    /**
     * 获取有对应密炼区的权限
     *
     * @return 密炼区的code列表
     */
    List<String> haveMixAreaPermission();

    /**
     * 获取当前用户的权限集合
     *
     * @return 权限的Set集合
     */
    Set<String> getPermission();

    /**
     * 如果没有权限，添加错误日志修改id=-999L
     * @param obj 校验对象
     * @param permissionKey 校验的权限key
     * @param permissionSet 权限Set集合
     * @param importLogId 导入的日志id
     * @param errorNum 错误行数
     * @param errorDetail 错误详情
     * @param importErrorLogs 错误日志列表
     */
    void addImportError(Object obj, String permissionKey, Set<String> permissionSet,
                        Long importLogId, Integer errorNum, String errorDetail, List<ImportErrorLog> importErrorLogs);

}
