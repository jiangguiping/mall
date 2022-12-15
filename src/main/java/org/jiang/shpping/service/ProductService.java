package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.entity.StoreProduct;
import org.jiang.shpping.request.ProductRequest;
import org.jiang.shpping.response.IndexProductResponse;
import org.jiang.shpping.response.ProductDetailReplyResponse;
import org.jiang.shpping.response.ProductDetailResponse;
import org.jiang.shpping.response.StoreProductReplayCountResponse;
import org.jiang.shpping.vo.CategoryTreeVo;
import org.jiang.shpping.vo.PageResult;

import java.util.List;

public interface ProductService extends IService<StoreProduct> {
    List<StoreProduct> test();

    PageResult<StoreProduct> findH5List(ProductRequest request, PageDTO pageDTO);

    /**
     * 商品评论数量
     * @param id 商品id
     * @return StoreProductReplayCountResponse
     */
    StoreProductReplayCountResponse getReplyCount(Integer id);

    ProductDetailReplyResponse getProductReply(Integer id);

    /**
     * 获取商品详情
     * @param id 商品编号
     * @param type normal-正常，void-视频
     * @return 商品详情信息
     */
    ProductDetailResponse getDetail(Integer id, String type);

    /**
     * 获取商品SKU详情
     * @param id 商品编号
     * @return 商品详情信息
     */
    ProductDetailResponse getSkuDetail(Integer id);

    /**
     * 获取购物车商品信息
     * @param productId 商品编号
     * @return StoreProduct
     */
    StoreProduct getCartByProId(Integer productId);

    /**
     * 商品分类
     * @return List
     */
    List<CategoryTreeVo> getCategory();

    PageResult<IndexProductResponse> getHotProductList(PageDTO pageDTO);

    /**
     * 添加/扣减库存
     * @param id 商品id
     * @param num 数量
     * @param type 类型：add—添加，sub—扣减
     */
    Boolean operationStock(Integer id, Integer num, String type);
}
