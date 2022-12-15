package org.jiang.shpping.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Store;
import org.jiang.shpping.dao.StoreCartDao;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.entity.*;
import org.jiang.shpping.exception.BizException;
import org.jiang.shpping.request.CartNumRequest;
import org.jiang.shpping.request.CartRequest;
import org.jiang.shpping.response.CartInfoResponse;
import org.jiang.shpping.service.*;
import org.jiang.shpping.utils.Constants;
import org.jiang.shpping.utils.UserHolder;
import org.jiang.shpping.vo.PageResult;
import org.jiang.shpping.vo.Result;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StoreCartServiceImpl extends ServiceImpl<StoreCartDao, StoreCart> implements StoreCartService {

    @Resource
    private StoreCartDao dao;

    @Autowired
    private ProductService ProductService;

    @Autowired
    private UserService userService;

    @Autowired
    private StoreProductAttrValueService storeProductAttrValueService;

    @Autowired
    private SystemUserLevelService systemUserLevelService;


    /**
     * 新增购物车数据
     *
     * @param storeCartRequest 新增购物车参数
     * @return 新增结果
     */
    @Override
    public String saveCate(CartRequest storeCartRequest) {

        //判断商品正常
         StoreProduct product = ProductService.getById(storeCartRequest.getProductId());
        if (ObjectUtil.isNull(product) || product.getIsDel() || !product.getIsShow()) {
            throw new BizException("未找到对应商品");
        }
        List<StoreProductAttrValue> attrValues = storeProductAttrValueService.getListByProductIdAndAttrId(product.getId(),
                storeCartRequest.getProductAttrUnique(),
                Constants.PRODUCT_TYPE_NORMAL);

        if (CollUtil.isEmpty(attrValues)) {
            throw new BizException("未找到对应的商品SKU");
        }

         Integer uid = UserHolder.getUser().getUid();
        StoreCart storeCartPram = new StoreCart();
        storeCartPram.setProductAttrUnique(storeCartRequest.getProductAttrUnique());
        storeCartPram.setUid(uid);
        List<StoreCart> existCarts = getByEntity(storeCartPram);

        if (existCarts.size() > 0) { // 购物车添加数量
            StoreCart forUpdateStoreCart = existCarts.get(0);
            forUpdateStoreCart.setCartNum(forUpdateStoreCart.getCartNum() + storeCartRequest.getCartNum());
            boolean updateResult = updateById(forUpdateStoreCart);
            if (!updateResult) {
                throw new BizException("添加购物车失败");
            }
            return forUpdateStoreCart.getId()+"";
        }else {
            StoreCart storeCart = new StoreCart();
            BeanUtils.copyProperties(storeCartRequest, storeCart);
            storeCart.setUid(uid);
            storeCart.setType("product");
            if (dao.insert(storeCart) <= 0) {
                throw new BizException("添加购物车失败");
            }
            return storeCart.getId() + "";
        }
    }

    /**
     * 删除购物车信息
     * @param ids 待删除id
     * @return 删除结果状态
     */
    @Override
    public Boolean deleteCartByIds(List<Long> ids) {
        return dao.deleteBatchIds(ids) > 0;
    }

    /**
     * 修改购物车商品数量
     *
     * @param id     购物车id
     * @param number 数量
     */
    @Override
    public Boolean updateCartNum(Integer id, Integer number) {
        if (ObjectUtil.isNull(number)) {
            throw new BizException("商品数量不合法");
        }
        if (number <= 0 || number > 99) {
            throw new BizException("商品数量不能小于1大于99");
        }
        StoreCart storeCart = getById(id);

        if (ObjectUtil.isNull(storeCart)) {
            throw new BizException("当前购物车不存在");
        }
        if (storeCart.getCartNum().equals(number)) {
            return true;
        }
        storeCart.setCartNum(number);
        return updateById(storeCart);
    }

    /**
     * 获取当前购物车数量
     *
     * @param request 请求参数
     * @return 数量
     */
    @Override
    public Map<String, Integer> getUserCount(CartNumRequest request) {

        Integer userId = UserHolder.getUser().getUid();
        Map<String, Integer> map = new HashMap<>();
        int num;
        if (request.getType().equals("total")) {
            num = getUserCountByStatus(userId, request.getNumType());
        } else {
            num = getUserSumByStatus(userId, request.getNumType());
        }
        map.put("count", num);
        return map;
    }

    /**
     * 根据有效标识符获取出数据
     * @param pageDTO 分页参数
     *
     * @return 购物车列表
     */
    @Override
    public PageResult<CartInfoResponse> getPageList(PageDTO pageDTO) {
        Integer uid = UserHolder.getUser().getUid();
        Page<StoreCart> page =new Page<>(pageDTO.getPage(),pageDTO.getLimit());
        Page<StoreCart> storeCarts = dao.selectPage(page, new LambdaQueryWrapper<StoreCart>()
                .eq(StoreCart::getUid, uid)
                .eq(StoreCart::getStatus, true)
                .eq(StoreCart::getIsNew, false)
                .orderByDesc(StoreCart::getId));
        log.debug("获取购物车: {}", storeCarts.getRecords());
        if (CollUtil.isEmpty(storeCarts.getRecords())) {
            return new  PageResult(new ArrayList(),0);
        }

         Integer userLevelId = UserHolder.getUser().getLevel();
        SystemUserLevel userLevel = null;
        if (userLevelId > 0) {
            userLevel = systemUserLevelService.getByLevelId(userLevelId);
        }

        List<CartInfoResponse> response = new ArrayList<>();
        for (StoreCart storeCart : storeCarts.getRecords()) {
            CartInfoResponse cartInfoResponse = new CartInfoResponse();
            BeanUtils.copyProperties(storeCart, cartInfoResponse);
            // 获取商品信息
            StoreProduct storeProduct = ProductService.getCartByProId(storeCart.getProductId());
//            log.debug("获取商品id: {}", storeCart.getProductId());
//            log.debug("获取商品信息: {}", storeProduct);
            cartInfoResponse.setImage(storeProduct.getImage());
            cartInfoResponse.setStoreName(storeProduct.getStoreName());

            // 获取对应的商品规格信息(只会有一条信息)
            List<StoreProductAttrValue> attrValueList = storeProductAttrValueService.getListByProductIdAndAttrId(storeCart.getProductId(),
                    storeCart.getProductAttrUnique(), Constants.PRODUCT_TYPE_NORMAL);
            log.debug("获取商品规格: {}", attrValueList);
            // 规格不存在即失效
            if (CollUtil.isEmpty(attrValueList)) {
                cartInfoResponse.setAttrStatus(false);
                response.add(cartInfoResponse);
                continue ;
            }
            StoreProductAttrValue attrValue = attrValueList.get(0);
            if (StrUtil.isNotBlank(attrValue.getImage())) {
                cartInfoResponse.setImage(attrValue.getImage());
            }
            cartInfoResponse.setAttrId(attrValue.getId());
            cartInfoResponse.setSuk(attrValue.getSuk());
            cartInfoResponse.setPrice(attrValue.getPrice());
            cartInfoResponse.setAttrId(attrValue.getId());
            cartInfoResponse.setAttrStatus(attrValue.getStock() > 0);
            cartInfoResponse.setStock(attrValue.getStock());

            if (ObjectUtil.isNotNull(userLevel)) {
                BigDecimal vipPrice = attrValue.getPrice().multiply(new BigDecimal(userLevel.getDiscount())).divide(new BigDecimal(100), 2 ,BigDecimal.ROUND_HALF_UP);
                cartInfoResponse.setVipPrice(vipPrice);
            }
            response.add(cartInfoResponse);
        }

        return new  PageResult(response, (int) storeCarts.getTotal());
    }

    /**
     * 通过id和uid获取购物车信息
     *
     * @param id  购物车id
     * @param uid 用户uid
     * @return StoreCart
     */
    @Override
    public StoreCart getByIdAndUid(Long id, Integer uid) {
        return dao.selectOne(new LambdaQueryWrapper<StoreCart>()
                .eq(StoreCart::getId,id)
                .eq(StoreCart::getUid,uid)
                .eq(StoreCart::getIsNew,false)
                .eq(StoreCart::getStatus,true));
    }

    /**
     * 购物车购买商品总数量
     * @param userId Integer 用户id
     * @param status 商品类型：true-有效商品，false-无效商品
     * @return Integer
     */
    private Integer getUserSumByStatus(Integer userId, Boolean status) {
        QueryWrapper<StoreCart> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("ifnull(sum(cart_num), 0) as cart_num");
        queryWrapper.eq("uid", userId);
        queryWrapper.eq("is_new", false);
        queryWrapper.eq("status", status);
        StoreCart storeCart = dao.selectOne(queryWrapper);
        if (ObjectUtil.isNull(storeCart)) {
            return 0;
        }
        return storeCart.getCartNum();
    }



    /**
     * 购物车商品数量（条数）
     * @param userId Integer 用户id
     * @param status Boolean 商品类型：true-有效商品，false-无效商品
     * @return Integer
     */
    private Integer getUserCountByStatus(Integer userId, Boolean status) {

        return dao.selectCount(new LambdaQueryWrapper<StoreCart>()
                .eq(StoreCart::getUid,userId)
                .eq(StoreCart::getStatus,status)
                .eq(StoreCart::getIsNew,false));
    }

    /**
     * 购物车基本查询
     * @param storeCart 购物车参数
     * @return 购物车结果数据
     */
    private List<StoreCart> getByEntity(StoreCart storeCart) {
        return dao.selectList(new LambdaQueryWrapper<StoreCart>().setEntity(storeCart));
    }
}
