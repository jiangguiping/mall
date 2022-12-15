package org.jiang.shpping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.StoreProductAttrDao;
import org.jiang.shpping.entity.StoreProductAttr;
import org.jiang.shpping.service.StoreProductAttrService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class StoreProductAttrServiceImpl extends ServiceImpl<StoreProductAttrDao, StoreProductAttr>
        implements StoreProductAttrService {

    @Resource
    private StoreProductAttrDao dao;

    /**
     * 获取商品规格列表
     *
     * @param productId 商品id
     * @param type      商品类型
     * @return List
     */
    @Override
    public List<StoreProductAttr> getListByProductIdAndType(Integer productId, Integer type) {
        return dao.selectList(new LambdaQueryWrapper<StoreProductAttr>()
                .eq(StoreProductAttr::getProductId,productId)
                .eq(StoreProductAttr::getType,type)
                .eq(StoreProductAttr::getIsDel,false));
    }
}
