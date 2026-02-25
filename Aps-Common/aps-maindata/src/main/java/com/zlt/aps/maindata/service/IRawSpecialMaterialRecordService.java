package com.zlt.aps.maindata.service;


import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.mp.api.domain.entity.RawSpecialMaterialRecord;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IRawSpecialMaterialRecordService.java
 * 描    述：IRawSpecialMaterialRecordService特殊材料清单后端接口
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IRawSpecialMaterialRecordService  extends IDocService<RawSpecialMaterialRecord>{

    /**
     * 判断是否特殊材料
     * @param targetEmbryoCode
     * @param mdmMaterialConsumeDetailList
     * @param specialMaterialList
     * @return
     */
    boolean hasSpecialMaterial(String targetEmbryoCode, List<MdmMaterialConsumeDetail> mdmMaterialConsumeDetailList,
                               List<RawSpecialMaterialRecord> specialMaterialList);

}
