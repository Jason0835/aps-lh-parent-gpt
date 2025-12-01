package com.zlt.mix.schedule.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.security.service.TokenService;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.schedule.service.PermissionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 用户资源权限Service业务层处理
 *
 * @author Liam
 * @date 2022-07-12
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private TokenService tokenService;


    @Override
    public Set<String> getPermission() {
        return tokenService.getLoginUser().getPermissions().values().stream().findFirst().orElse(null);
    }

    @Override
    public void addImportError(Object obj, String permissionKey,Set<String> permissionSet,
                               Long importLogId, Integer errorNum, String errorDetail, List<ImportErrorLog> importErrorLogs) {
        if (StringUtils.isNotEmpty(permissionKey) && !permissionSet.contains(permissionKey)) {
            ReflectUtils.setFieldValue(obj, "id", -999L);
            ImportUtil.addImportErrorLog(importLogId, errorNum, errorDetail, importErrorLogs);
        }

    }

    @Override
    public List<String> haveMixAreaPermission() {
        Set<String> permission = getPermission();
        if (CollectionUtils.isEmpty(permission)) {
            return new ArrayList<>(0);
        }
        String[] mixAreaPermission = ZltConstant.MIX_AREA_PERMISSIONS;
        //如果为admin权限
        if (permission.contains(ZltConstant.ADMIN_PERMISSION)) {
            return new ArrayList<>(Arrays.asList(mixAreaPermission));
        }
        //校验权限
        List<String> list = new ArrayList<>();
        for (String i : mixAreaPermission) {
            if (permission.contains(i)) {
                list.add(i);
            }
        }
        return list;
    }

}
