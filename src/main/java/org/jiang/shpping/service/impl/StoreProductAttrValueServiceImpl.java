package org.jiang.shpping.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.StoreProductAttrValueDao;
import org.jiang.shpping.entity.StoreProductAttrValue;
import org.jiang.shpping.exception.BizException;
import org.jiang.shpping.service.StoreProductAttrValueService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class StoreProductAttrValueServiceImpl extends ServiceImpl<StoreProductAttrValueDao, StoreProductAttrValue>
        implements StoreProductAttrValueService {

    @Resource
    private StoreProductAttrValueDao dao;

    /**
     * 获取商品规格列表
     *
     * @param productId 商品id
     * @param type      商品类型
     * @return List
     */
    @Override
    public List<StoreProductAttrValue> getListByProductIdAndType(Integer productId, Integer type) {
        return dao.selectList(new LambdaQueryWrapper<StoreProductAttrValue>()
                .eq(StoreProductAttrValue::getProductId,productId)
                .eq(StoreProductAttrValue::getIsDel,false));

    }

    /**
     * 根据商品id和attrId获取列表集合
     *
     * @param productId 商品id
     * @param attrId    属性id
     * @param type      商品类型
     * @return 商品属性集合
     */
    @Override
    public List<StoreProductAttrValue> getListByProductIdAndAttrId(Integer productId, String attrId, Integer type) {
        LambdaQueryWrapper<StoreProductAttrValue> lambdaQueryWrapper = Wrappers.lambdaQuery();
        lambdaQueryWrapper.eq(StoreProductAttrValue::getProductId, productId);
        if(null != attrId){
            lambdaQueryWrapper.eq(StoreProductAttrValue::getId, attrId);
        }
        lambdaQueryWrapper.eq(StoreProductAttrValue::getIsDel, false);
        return dao.selectList(lambdaQueryWrapper);
    }

    /**
     * 根据id、类型查询
     *
     * @param id        ID
     * @param productId
     * @param type      类型
     * @return StoreProductAttrValue
     */
    @Override
    public StoreProductAttrValue getByIdAndProductIdAndType(Integer id, Integer productId, Integer type) {
        return dao.selectOne(new LambdaQueryWrapper<StoreProductAttrValue>()
                .eq(StoreProductAttrValue::getId,id)
                .eq(StoreProductAttrValue::getProductId,productId)
                .eq(StoreProductAttrValue::getIsDel,false));
    }

    /**
     * 添加(退货)/扣减库存
     * @param id 秒杀商品id
     * @param num 数量
     * @param operationType 类型：add—添加，sub—扣减
     * @param type 活动类型 0=商品，1=秒杀，2=砍价，3=拼团
     * @return Boolean
     */
    @Override
    public Boolean operationStock(Integer id, Integer num, String operationType, Integer type) {
        UpdateWrapper<StoreProductAttrValue> updateWrapper = new UpdateWrapper<>();
        if (operationType.equals("add")) {
            updateWrapper.setSql(StrUtil.format("stock = stock + {}", num));
            updateWrapper.setSql(StrUtil.format("sales = sales - {}", num));
            if (type > 0) {
                updateWrapper.setSql(StrUtil.format("quota = quota + {}", num));
            }
        }
        if (operationType.equals("sub")) {
            updateWrapper.setSql(StrUtil.format("stock = stock - {}", num));
            updateWrapper.setSql(StrUtil.format("sales = sales + {}", num));
            if (type > 0) {
                updateWrapper.setSql(StrUtil.format("quota = quota - {}", num));
                // 扣减时加乐观锁保证库存不为负
                updateWrapper.last(StrUtil.format("and (quota - {} >= 0)", num));
            } else {
                // 扣减时加乐观锁保证库存不为负
                updateWrapper.last(StrUtil.format("and (stock - {} >= 0)", num));
            }
        }
        updateWrapper.eq("id", id);
        updateWrapper.eq("type", type);
        boolean update = update(updateWrapper);
        if (!update) {
            throw new BizException("更新商品attrValue失败，attrValueId = " + id);
        }
        return update;
    }
}
