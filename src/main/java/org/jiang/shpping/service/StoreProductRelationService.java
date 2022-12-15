package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.entity.StoreProductRelation;
import org.jiang.shpping.response.UserRelationResponse;
import org.jiang.shpping.request.UserCollectAllRequest;
import org.jiang.shpping.request.UserCollectRequest;
import org.jiang.shpping.vo.PageResult;

import java.util.List;

public interface StoreProductRelationService extends IService<StoreProductRelation> {

    List<StoreProductRelation> getLikeOrCollectByUser(Integer userId, Integer productId, boolean isLike);

    /**
     * 添加收藏
     * @param request 收藏参数
     */
    Boolean add(UserCollectRequest request);

    /**
     * 批量收藏
     * @param request 收藏参数
     * @return Boolean
     */
    Boolean all(UserCollectAllRequest request);

    /**
     * 取消收藏
     * @param requestJson 收藏idsJson
     * @return Boolean
     */
    Boolean delete(String requestJson);

    /**
     * 根据商品Id取消收藏
     * @param proId 商品Id
     * @return Boolean
     */
    Boolean deleteByProIdAndUid(Integer proId);

    /**
     * 获取用户收藏列表
     * @param PageDTO 分页参数
     * @return List<UserRelationResponse>
     */
    PageResult<UserRelationResponse> getUserList(PageDTO pageDTO);
}
