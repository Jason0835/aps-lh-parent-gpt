package com.zlt.aps.lh.engine.enums;

import lombok.Getter;

/**
 * 库存地点枚举类型
 */
@Getter
public enum StorageLocationEnum {

    DOMESTIC_ASSORT("1","国内配套"),DOMESTIC_SALES("2","国内营销"),ABROAD_ASSORT("3","海外配套"),ABROAD_SALES("4","海外营销");
    private String storageCode;
    private String storageName;

    private StorageLocationEnum(String storageCode, String storageName){
        this.storageCode=storageCode;
        this.storageName=storageName;
    }

    /**
     * 根据下标获取
     * @param storageCode
     * @return
     */
    public static StorageLocationEnum getStorageLocationEnums(String storageCode) {
        for (StorageLocationEnum storageLocationEnum : StorageLocationEnum.values()) {
            if (storageLocationEnum.getStorageCode().equals(storageCode)) {
                return storageLocationEnum;
            }
        }
        return null;
    }
}
