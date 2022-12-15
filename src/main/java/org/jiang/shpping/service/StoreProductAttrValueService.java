package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.entity.StoreProductAttrValue;

import java.util.List;

public interface StoreProductAttrValueService extends IService<StoreProductAttrValue> {

    /**
     * 获取商品规格列表
     * @param productId 商品id
     * @param type 商品类型
     * @return List
     */
    List<StoreProductAttrValue> getListByProductIdAndType(Integer productId, Integer type);

    /**
     * 根据商品id和attrId获取列表集合
     * @param productId 商品id
     * @param attrId 属性id
     * @param type 商品类型
     * @return 商品属性集合
     */
    List<StoreProductAttrValue> getListByProductIdAndAttrId(Integer productId, String attrId, Integer type);

    /**
     * 根据id、类型查询
     * @param id ID
     * @param type 类型
     * @return StoreProductAttrValue
     */
    StoreProductAttrValue getByIdAndProductIdAndType(Integer id, Integer productId, Integer type);

    /**
     * 添加/扣减库存
     * @param id 秒杀商品id
     * @param num 数量
     * @param operationType 类型：add—添加，sub—扣减
     * @param type 活动类型 0=商品，1=秒杀，2=砍价，3=拼团
     */
    Boolean operationStock(Integer id, Integer num, String operationType, Integer type);
}
