package org.jiang.shpping.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jiang.shpping.constants.CategoryConstants;
import org.jiang.shpping.dao.StoreProductDao;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.dto.UserDTO;
import org.jiang.shpping.entity.*;
import org.jiang.shpping.exception.BizException;
import org.jiang.shpping.request.ProductRequest;
import org.jiang.shpping.response.*;
import org.jiang.shpping.service.*;
import org.jiang.shpping.utils.BeanCopyUtils;
import org.jiang.shpping.utils.Constants;
import org.jiang.shpping.utils.JanUtils;
import org.jiang.shpping.utils.UserHolder;
import org.jiang.shpping.vo.CategoryTreeVo;
import org.jiang.shpping.vo.PageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends ServiceImpl<StoreProductDao,StoreProduct>implements ProductService {

    @Resource
    private StoreProductDao dao;



    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StoreProductReplyService storeProductReplyService;

    @Autowired
    private SystemUserLevelService systemUserLevelService;

    @Autowired
    private StoreProductAttrValueService storeProductAttrValueService;

    @Autowired
    private StoreProductRelationService storeProductRelationService;

    @Autowired
    private UserService userService;


    @Autowired
    private StoreProductAttrService attrService;






    @Override
    public List<StoreProduct> test() {
         List<StoreProduct> storeProducts = dao.selectList(new LambdaQueryWrapper<>());
        return storeProducts;
    }



    @Override
    public PageResult<StoreProduct> findH5List(ProductRequest request, PageDTO pageDTO) {
        Page<StoreProduct> page =new Page<>(pageDTO.getPage(), pageDTO.getLimit());
        LambdaQueryWrapper<StoreProduct> lqw = new LambdaQueryWrapper<>();
        lqw.select(StoreProduct::getId, StoreProduct::getStoreName, StoreProduct::getImage, StoreProduct::getPrice,
                 StoreProduct::getSales, StoreProduct::getFicti, StoreProduct::getUnitName, StoreProduct::getStock);
        lqw.eq(StoreProduct::getIsRecycle,false);
        lqw.eq(StoreProduct::getIsDel,false);
        lqw.eq(StoreProduct::getMerId,false);
        lqw.gt(StoreProduct::getStock,0);
        lqw.eq(StoreProduct::getIsShow, true);
        //分类id不为空
        if(ObjectUtil.isNotNull(request.getCid()) && request.getCid() > 0) {
            //查找当前类下单所有子类
            List<Category> childVoListByPid = categoryService.getChildVoListByPid(request.getCid());
            List<Integer> categoryIdList = childVoListByPid.stream().map(Category::getId).collect(Collectors.toList());

            categoryIdList.add(request.getCid());
            lqw.in(StoreProduct::getCateId,categoryIdList);
//            lqw.apply(JanUtils.getFindInSetSql("cate_id", (ArrayList<Integer>) categoryIdList));
        }

        if (StrUtil.isNotBlank(request.getKeyword())) {
            if (JanUtils.isString2Num(request.getKeyword())) {
                Integer productId = Integer.valueOf(request.getKeyword());
                lqw.like(StoreProduct::getId,productId);
            }else {
                lqw.like(StoreProduct::getStoreName,request.getKeyword());
            }
        }
        //如果查询条件中有排序部分
        if (StrUtil.isNotBlank(request.getSalesOrder())) { //销量排序
            if (request.getSalesOrder().equals(Constants.SORT_DESC)) {
                lqw.last(" order by (sales + ficti) desc, sort desc, id desc");
            }else {
                lqw.last(" order by (sales + ficti) asc, sort asc, id asc");
            }
        }else {
            if (StrUtil.isNotBlank(request.getPriceOrder())) {  //价格排序
                if (request.getPriceOrder().equals(Constants.SORT_DESC)) {
                    lqw.orderByDesc(StoreProduct::getPrice);
                }else {
                    lqw.orderByAsc(StoreProduct::getPrice);
                }
            }
            lqw.orderByDesc(StoreProduct::getSort);
            lqw.orderByDesc(StoreProduct::getId);
        }

        Page<StoreProduct> productPage = dao.selectPage(page, lqw);
//        List<StoreProduct> userAddressdao = BeanCopyUtils.copyList(productPage.getRecords(),StoreProduct.class);

        return new PageResult<>(productPage.getRecords(), (int) productPage.getTotal());
    }

    /**
     * 商品评论数量
     *
     * @param id 商品id
     * @return StoreProductReplayCountResponse
     */
    @Override
    public StoreProductReplayCountResponse getReplyCount(Integer id) {
        StoreProductReplayCountResponse myRecord = storeProductReplyService.getH5Count(id);
        return myRecord;
    }

    /**
     * 商品详情评论
     * @param id 商品id
     * @return ProductDetailReplyResponse
     * 评论只有一条，图文
     * 评价总数
     * 好评率
     */
    @Override
    public ProductDetailReplyResponse getProductReply(Integer id) {
        return storeProductReplyService.getH5ProductReply(id);
    }

    /**
     * 获取商品详情
     *
     * @param id   商品编号
     * @param type normal-正常，void-视频
     * @return 商品详情信息
     */
    @Override
    public ProductDetailResponse getDetail(Integer id, String type) {

        UserDTO user = null;
        try {
            user = UserHolder.getUser();
        }catch (Exception e) {
            e.printStackTrace();
        }

        SystemUserLevel userLevel = null;
        if (ObjectUtil.isNotNull(user) && user.getLevel() >0) {
            userLevel = systemUserLevelService.getByLevelId(user.getLevel());
        }
        ProductDetailResponse productDetailResponse = new ProductDetailResponse();
        //查询商品
        StoreProduct storeProduct = getH5Detail(id);
        if (ObjectUtil.isNotNull(userLevel)) {
            storeProduct.setVipPrice(storeProduct.getPrice());
        }

        productDetailResponse.setProductInfo(storeProduct);

        // 获取商品规格
        List<StoreProductAttr> attrList = attrService.getListByProductIdAndType(storeProduct.getId(), Constants.PRODUCT_TYPE_NORMAL);
        // 根据制式设置attr属性
        productDetailResponse.setProductAttr(attrList);

        // 根据制式设置sku属性
        HashMap<String, Object> skuMap = new HashMap<>();
        List<StoreProductAttrValue> storeProductAttrValues = storeProductAttrValueService.getListByProductIdAndType(storeProduct.getId(), Constants.PRODUCT_TYPE_NORMAL);
        for (StoreProductAttrValue storeProductAttrValue : storeProductAttrValues) {
            StoreProductAttrValueResponse atr = new StoreProductAttrValueResponse();
            BeanUtils.copyProperties(storeProductAttrValue, atr);
            // 设置会员价
            if (ObjectUtil.isNotNull(userLevel)) {
                atr.setVipPrice(atr.getPrice());
            }
            skuMap.put(atr.getSuk(), atr);
        }
        productDetailResponse.setProductValue(skuMap);

        //用户收藏 分销反佣
        if (ObjectUtil.isNull(user)) {
            //查询用户是否收藏
            User info = userService.getInfo();
            productDetailResponse.setUserCollect(storeProductRelationService.getLikeOrCollectByUser(user.getUid(), id,false).size() > 0);
        }else {
            productDetailResponse.setUserCollect(false);
        }

        // 商品浏览量+1
        StoreProduct updateProduct = new StoreProduct();
        updateProduct.setId(id);
        updateProduct.setBrowse(storeProduct.getBrowse() + 1);
        updateById(updateProduct);
        return productDetailResponse;
    }

    /**
     * 获取商品SKU详情
     *
     * @param id 商品编号
     * @return 商品详情信息
     */
    @Override
    public ProductDetailResponse getSkuDetail(Integer id) {
        ProductDetailResponse productDetailResponse = new ProductDetailResponse();
        //查询商品
        StoreProduct storeProduct = getH5Detail(id);
        List<StoreProductAttr> attrList = attrService.getListByProductIdAndType(storeProduct.getId(), Constants.PRODUCT_TYPE_NORMAL);
        // 根据制式设置attr属性
        productDetailResponse.setProductAttr(attrList);

        //根据制式设置sku属性
        HashMap<String, Object> skuMap = new HashMap();
        List<StoreProductAttrValue> storeProductAttrValues = storeProductAttrValueService.getListByProductIdAndType(storeProduct.getId(), Constants.PRODUCT_TYPE_NORMAL);
        for (StoreProductAttrValue storeProductAttrValue : storeProductAttrValues) {
            StoreProductAttrValueResponse atr = new StoreProductAttrValueResponse();
            BeanUtils.copyProperties(storeProductAttrValue, atr);
//            // 设置会员价
//            if (ObjectUtil.isNotNull(userLevel)) {
//                BigDecimal vipPrice = atr.getPrice().multiply(new BigDecimal(userLevel.getDiscount())).divide(new BigDecimal(100), 2 ,BigDecimal.ROUND_HALF_UP);
//                atr.setVipPrice(vipPrice);
//            }
            skuMap.put(atr.getSuk(), atr);
        }
        productDetailResponse.setProductValue(skuMap);

        return productDetailResponse;
    }

    /**
     * 获取购物车商品信息
     *
     * @param productId 商品编号
     * @return StoreProduct
     */
    @Override
    public StoreProduct getCartByProId(Integer productId) {
        return dao.selectOne(new LambdaQueryWrapper<StoreProduct>()
                .select(StoreProduct::getId,
                        StoreProduct::getImage,
                        StoreProduct::getStoreName).eq(StoreProduct::getId,productId));
    }

    /**
     * 商品分类
     *
     * @return List
     */
    @Override
    public List<CategoryTreeVo> getCategory() {
        List<CategoryTreeVo> listTree =categoryService.getListTree(CategoryConstants.CATEGORY_TYPE_PRODUCT, 1, "");
        for (int i =0;i<listTree.size();) {
             CategoryTreeVo categoryTreeVo = listTree.get(i);
            if (!categoryTreeVo.getPid().equals(0)) {
                listTree.remove(i);
                continue;
            }
            i++;
        }
        return listTree;
    }

    @Override
    public PageResult<IndexProductResponse> getHotProductList(PageDTO pageDTO) {
        return null;
    }

    /**
     * 添加/扣减库存
     * @param id 商品id
     * @param num 数量
     * @param type 类型：add—添加，sub—扣减
     */
    @Override
    public Boolean operationStock(Integer id, Integer num, String type) {
        UpdateWrapper<StoreProduct> updateWrapper = new UpdateWrapper<>();
        if (type.equals("add")) {
            updateWrapper.setSql(StrUtil.format("stock = stock + {}", num));
            updateWrapper.setSql(StrUtil.format("sales = sales - {}", num));
        }
        if (type.equals("sub")) {
            updateWrapper.setSql(StrUtil.format("stock = stock - {}", num));
            updateWrapper.setSql(StrUtil.format("sales = sales + {}", num));
            // 扣减时加乐观锁保证库存不为负
            updateWrapper.last(StrUtil.format(" and (stock - {} >= 0)", num));
        }
        updateWrapper.eq("id", id);
        boolean update = update(updateWrapper);
        if (!update) {
            throw new BizException("更新普通商品库存失败,商品id = " + id);
        }
        return update;
    }


    /**
     * 获取移动端商品详情
     * @param id 商品id
     * @return StoreProduct
     */
    public StoreProduct getH5Detail(Integer id) {
        StoreProduct storeProduct = dao.selectOne(new LambdaQueryWrapper<StoreProduct>().select(StoreProduct::getId, StoreProduct::getImage, StoreProduct::getStoreName, StoreProduct::getSliderImage,
                        StoreProduct::getOtPrice, StoreProduct::getStock, StoreProduct::getSales, StoreProduct::getPrice,
                        StoreProduct::getFicti, StoreProduct::getIsSub, StoreProduct::getStoreInfo, StoreProduct::getBrowse, StoreProduct::getUnitName)
                .eq(StoreProduct::getId, id)
                .eq(StoreProduct::getIsRecycle, false)
                .eq(StoreProduct::getIsShow, true)
                .eq(StoreProduct::getIsDel, false));
        if (ObjectUtil.isNull(storeProduct)) {
            throw new BizException(StrUtil.format("未找到编号为{}的商品", id));
        }
        return storeProduct;
    }
}
