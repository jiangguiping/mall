package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.entity.StoreProductReply;
import org.jiang.shpping.request.StoreProductReplyAddRequest;
import org.jiang.shpping.response.ProductDetailReplyResponse;
import org.jiang.shpping.response.StoreProductReplayCountResponse;

public interface StoreProductReplyService extends IService<StoreProductReply> {

    /**
     * H5商品评论统计
     * @param id 商品编号
     * @return MyRecord
     */
    StoreProductReplayCountResponse getH5Count(Integer id);

    /**
     * H5商品详情评论信息
     * @param id 商品编号
     * @return ProductDetailReplyResponse
     */
    ProductDetailReplyResponse getH5ProductReply(Integer id);

    /**
     * 创建订单商品评价
     * @param request 请求参数
     * @return Boolean
     */
    Boolean create(StoreProductReplyAddRequest request);

    /**
     * 查询是否已经回复
     * @param unique skuId
     * @param orderId 订单id
     * @return Boolean
     */
    Boolean isReply(String unique, Integer orderId);
}
