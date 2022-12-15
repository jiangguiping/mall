package org.jiang.shpping.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.StoreProductRelationDao;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.entity.StoreProductRelation;
import org.jiang.shpping.response.UserRelationResponse;
import org.jiang.shpping.exception.BizException;
import org.jiang.shpping.request.UserCollectAllRequest;
import org.jiang.shpping.request.UserCollectRequest;
import org.jiang.shpping.service.StoreProductRelationService;
import org.jiang.shpping.utils.BeanCopyUtils;
import org.jiang.shpping.utils.JanUtils;
import org.jiang.shpping.utils.UserHolder;
import org.jiang.shpping.vo.PageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class StoreProductRelationServiceImpl extends ServiceImpl<StoreProductRelationDao, StoreProductRelation>
        implements StoreProductRelationService {
    @Resource
    private StoreProductRelationDao dao;



    /**
     * 获取用户当前是否喜欢该商品
     * @param userId 用户id
     * @param productId 商品id
     * @return 是否喜欢标识
     */
    @Override
    public List<StoreProductRelation> getLikeOrCollectByUser(Integer userId, Integer productId, boolean isLike) {
        String typeValue = isLike ? "like":"collect";
        return dao.selectList(new LambdaQueryWrapper<StoreProductRelation>()
                .eq(StoreProductRelation::getProductId,productId)
                .eq(StoreProductRelation::getUid,userId)
                .eq(StoreProductRelation::getType,typeValue));
    }

    /**
     * 添加收藏
     *
     * @param request 收藏参数
     */
    @Override
    public Boolean add(UserCollectRequest request) {
        StoreProductRelation storeProductRelation = new StoreProductRelation();
        BeanUtils.copyProperties(request, storeProductRelation);
        storeProductRelation.setUid(UserHolder.getUser().getUid());
        return save(storeProductRelation);
    }

    /**
     * 批量收藏
     *
     * @param request 收藏参数
     * @return Boolean
     */
    @Override
    public Boolean all(UserCollectAllRequest request) {
        Integer[] arr = request.getProductId();
        if(arr.length < 1){
            throw new BizException("请选择产品");
        }
        List<Integer> list = JanUtils.arrayUnique(arr);
        Integer uid = UserHolder.getUser().getUid();
        deleteAll(request, uid, "collect");  //先删除所有已存在的
        ArrayList<StoreProductRelation> storeProductRelationList = new ArrayList<>();
        for (Integer productId: list) {
            StoreProductRelation storeProductRelation = new StoreProductRelation();
            storeProductRelation.setUid(uid);
            storeProductRelation.setType("collect");
            storeProductRelation.setProductId(productId);
            storeProductRelation.setCategory(request.getCategory());
            storeProductRelationList.add(storeProductRelation);
        }
        return saveBatch(storeProductRelationList);
    }

    /**
     * 取消收藏
     *
     * @param requestJson 收藏idsJson
     * @return Boolean
     */
    @Override
    public Boolean delete(String requestJson) {
        JSONObject jsonObject = JSONObject.parseObject(requestJson);
        if (StrUtil.isBlank(jsonObject.getString("ids"))) {
            throw new BizException("收藏id不能为空");
        }
        List<Integer> idList = JanUtils.stringToArray(jsonObject.getString("ids"));

        if (CollUtil.isEmpty(idList)) {
            throw new BizException("收藏id不能为空");
        }

         Integer uid = UserHolder.getUser().getUid();
         int delete = dao.delete(new LambdaQueryWrapper<StoreProductRelation>()
                .in(StoreProductRelation::getId, idList)
                .eq(StoreProductRelation::getUid, uid));

        return delete > 0;
    }

    /**
     * 根据商品Id取消收藏
     *
     * @param proId 商品Id
     * @return Boolean
     */
    @Override
    public Boolean deleteByProIdAndUid(Integer proId) {
        Integer uid = UserHolder.getUser().getUid();
         int delete = dao.delete(new LambdaQueryWrapper<StoreProductRelation>()
                 .in(StoreProductRelation::getProductId, proId)
                 .eq(StoreProductRelation::getUid, uid));

        return delete > 0;
    }

    /**
     * 获取用户收藏列表
     *
     * @param pageDTO@return List<UserRelationResponse>
     */
    @Override
    public PageResult<UserRelationResponse> getUserList(PageDTO pageDTO) {
        Page<StoreProductRelation> page =new Page<>();
        Integer uid = UserHolder.getUser().getUid();
        LambdaQueryWrapper<StoreProductRelation> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StoreProductRelation::getUid,uid);
        Page<StoreProductRelation> pages = dao.selectPage(page, lqw);
        if (CollUtil.isEmpty(pages.getRecords())) {
            throw new BizException("没有数据");
        }
        List<UserRelationResponse> userRelationResponses = BeanCopyUtils.copyList(pages.getRecords(), UserRelationResponse.class);
        return new PageResult<>(userRelationResponses,(int) pages.getTotal());
    }

    /**
     * 取消收藏产品
     * @param request UserCollectAllRequest 参数
     * @param collect 类型
     */
    private void deleteAll(UserCollectAllRequest request, Integer uid, String collect) {

        dao.delete(new LambdaQueryWrapper<StoreProductRelation>()
                .in(StoreProductRelation::getProductId,Arrays.asList(request.getProductId()))
                .eq(StoreProductRelation::getCategory,request.getCategory())
                .eq(StoreProductRelation::getUid,uid)
                .eq(StoreProductRelation::getType,collect));
    }
}
