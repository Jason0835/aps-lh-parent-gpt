package com.zlt.mdm.auth.mapper;

import com.zlt.mdm.auth.api.domain.MdmSystemData;
import com.zlt.mdm.auth.api.domain.vo.UserSystemVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

public interface MdmSystemAuthMapper {

    /**
     * 查询系统权限列表，按用户ID来查
     * @param userId
     * @return
     */
    UserSystemVo selectSystemDatasByUserId(Long userId);

    List<MdmSystemData> selectSystemDataList();
}
