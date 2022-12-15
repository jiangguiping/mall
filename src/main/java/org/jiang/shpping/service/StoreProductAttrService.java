package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.entity.StoreProductAttr;

import java.util.List;

public interface StoreProductAttrService extends IService<StoreProductAttr> {

    /**
     * 获取商品规格列表
     * @param productId 商品id
     * @param type 商品类型
     * @return List
     */
    List<StoreProductAttr> getListByProductIdAndType(Integer productId, Integer type);
}
