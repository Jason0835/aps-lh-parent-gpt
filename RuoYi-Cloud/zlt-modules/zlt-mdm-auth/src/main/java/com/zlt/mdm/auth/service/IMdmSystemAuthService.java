package com.zlt.mdm.auth.service;

import com.zlt.mdm.auth.api.domain.MdmSystemData;
import com.zlt.mdm.auth.api.domain.vo.UserSystemVo;

import java.util.List;

public interface IMdmSystemAuthService {

    /**
     * 查询系统权限列表，按用户ID来查
     * @param userId
     * @return
     */
    UserSystemVo selectSystemDataByUserId(Long userId);

    /***
     * 读取所有配置的系统清单
     * @return
     */
    List<MdmSystemData> selectSystemDataList();
}
