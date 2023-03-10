package org.jiang.shpping.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jiang.shpping.dao.StoreProductReplyDao;
import org.jiang.shpping.dto.UserDTO;
import org.jiang.shpping.entity.StoreOrder;
import org.jiang.shpping.entity.StoreProductReply;
import org.jiang.shpping.exception.BizException;
import org.jiang.shpping.request.StoreProductReplyAddRequest;
import org.jiang.shpping.response.ProductDetailReplyResponse;
import org.jiang.shpping.response.ProductReplyResponse;
import org.jiang.shpping.response.StoreProductReplayCountResponse;
import org.jiang.shpping.service.StoreOrderInfoService;
import org.jiang.shpping.service.StoreOrderService;
import org.jiang.shpping.service.StoreProductReplyService;
import org.jiang.shpping.utils.Constants;
import org.jiang.shpping.utils.JanUtils;
import org.jiang.shpping.utils.UserHolder;
import org.jiang.shpping.vo.StoreOrderInfoOldVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@Service
public class StoreProductReplyServiceImpl extends ServiceImpl<StoreProductReplyDao, StoreProductReply> implements StoreProductReplyService {

    @Resource
    private StoreProductReplyDao dao;

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private StoreOrderInfoService storeOrderInfoService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public StoreProductReplayCountResponse getH5Count(Integer id) {
        //????????????
        Integer sumCount = getCountByScore(id, "all");

        // ????????????
        Integer goodCount = getCountByScore(id, "good");
        // ????????????
        Integer mediumCount = getCountByScore(id, "medium");
        // ????????????
        Integer poorCount = getCountByScore(id, "poor");
        // ?????????
        String replyChance = "0";
        if (sumCount > 0 && goodCount > 0) {
            replyChance = String.format("%.2f", ((goodCount.doubleValue() / sumCount.doubleValue())));
        }

        // ????????????(???????????? + ????????????)/2
        Integer replyStar = 0;
        if (sumCount > 0) {
            replyStar = getSumStar(id);
        }
         StoreProductReplayCountResponse Response = new StoreProductReplayCountResponse();

        Response.setSumCount(Long.valueOf(sumCount));
        Response.setGoodCount(Long.valueOf(goodCount));
        Response.setInCount(Long.valueOf(mediumCount));
        Response.setPoorCount(Long.valueOf(poorCount));
        Response.setReplyChance(replyChance);
        Response.setReplyStar(replyStar);


        return Response;
    }

    /**
     * H5????????????????????????
     *
     * @param id ????????????
     * @return ProductDetailReplyResponse
     */
    @Override
    public ProductDetailReplyResponse getH5ProductReply(Integer id) {

        ProductDetailReplyResponse response = new ProductDetailReplyResponse();

        Integer sumCount = getCountByScore(id, "all");
        if (sumCount.equals(0)) {
            response.setSumCount(0);
            response.setReplyChance("0");
            return response;
        }

        Integer goodCount = getCountByScore(id, "good");
        String replyChance = "0";
        if (sumCount > 0 && goodCount > 0) {
            replyChance = String.format("%.2f", ((goodCount.doubleValue() / sumCount.doubleValue())));
        }

        //????????????????????????
        LambdaQueryWrapper<StoreProductReply> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StoreProductReply::getProductId,id); //????????????id??????
        lqw.eq(StoreProductReply::getIsDel,false);
        lqw.orderByDesc(StoreProductReply::getId);
        lqw.last("limit 1");

        StoreProductReply storeProductReply = dao.selectOne(lqw);
        ProductReplyResponse productReplyResponse = new ProductReplyResponse();
        BeanUtils.copyProperties(storeProductReply, productReplyResponse);

        productReplyResponse.setPics(JanUtils.stringToArrayStr(storeProductReply.getPics()));

        //??????
        String nickname = storeProductReply.getNickname();
        if (StrUtil.isNotBlank(nickname)) {
            if (nickname.length() == 1) {
                nickname = nickname.concat("**");
            } else if (nickname.length() == 2) {
                nickname = nickname.substring(0, 1) + "**";
            } else {
                nickname = nickname.substring(0, 1) + "**" + nickname.substring(nickname.length() - 1);
            }
            productReplyResponse.setNickname(nickname);
        }

        // ?????? = ??????????????? + ??????????????? / 2
        BigDecimal sumScore = new BigDecimal(storeProductReply.getProductScore() + storeProductReply.getServiceScore());


        BigDecimal divide = sumScore.divide(BigDecimal.valueOf(2L), 0, BigDecimal.ROUND_DOWN);
        productReplyResponse.setScore(divide.intValue());

        response.setSumCount(sumCount);
        response.setReplyChance(replyChance);
        response.setProductReply(productReplyResponse);


        return response;
    }

    /**
     * ?????????????????????
     * @param unique skuId
     * @param orderId ??????id
     * @return ????????????
     */
    @Override
    public Boolean isReply(String unique, Integer orderId) {
        LambdaQueryWrapper<StoreProductReply> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StoreProductReply::getUnique, unique);
        lqw.eq(StoreProductReply::getOid, orderId);
        List<StoreProductReply> replyList = dao.selectList(lqw);
        if (CollUtil.isEmpty(replyList)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * ????????????????????????
     *
     * @param request ????????????
     * @return Boolean
     */
    @Override
    public Boolean create(StoreProductReplyAddRequest request) {
         UserDTO user = UserHolder.getUser();
        StoreOrder storeOrder = storeOrderService.getByOderId(request.getOrderNo());
        if (ObjectUtil.isNull(storeOrder) || !storeOrder.getUid().equals(user.getUid())) {
            throw new BizException("??????????????????");
        }
        StoreProductReply storeProductReply = new StoreProductReply();
        BeanUtils.copyProperties(request, storeProductReply);
        storeProductReply.setOid(storeOrder.getId());
        Integer count = checkIsReply(storeProductReply);

        storeProductReply.setNickname(user.getNickname());
        if (StringUtils.isNotBlank(request.getPics())) {
            String pics = request.getPics().replace("[\"","").replace("\"]","")
                    .replace("\"","");
        }
        Boolean execute = transactionTemplate.execute(e -> {
            save(storeProductReply);
            //??????????????????
            completeOrder(storeProductReply, count, storeOrder);
            return Boolean.TRUE;
        });
        if (!execute) {
            throw new BizException("??????????????????");
        }
        return execute;
    }

    /**
     * ????????????????????????????????????????????????
     * @author Mr.Zhang
     * @since 2020-06-03
     * @return Integer
     */
    private void completeOrder(StoreProductReply storeProductReply, Integer count, StoreOrder storeOrder) {
        Integer replyCount = getReplyCountByEntity(storeProductReply, true);

        if (replyCount.equals(count)) {
            //????????????????????????
            storeOrder.setStatus(Constants.ORDER_STATUS_INT_COMPLETE);
            storeOrderService.updateById(storeOrder);
//            redisUtil.lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_COMPLETE_BY_USER, storeOrder.getId());
        }
    }
    /**
     * ????????????id  ??????id  ??????id ??????????????????
     * @author Mr.Zhang
     * @since 2020-06-03
     * @return Integer
     */
    private Integer getReplyCountByEntity(StoreProductReply request, boolean isAll) {
        LambdaQueryWrapper<StoreProductReply> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(StoreProductReply::getOid, request.getOid());
//        .eq(StoreProductReply::getUnique, request.getUnique());
        if (null != request.getUid()) {
            lambdaQueryWrapper.eq(StoreProductReply::getUid, request.getUid());
        }
        if (!isAll) {
            lambdaQueryWrapper.eq(StoreProductReply::getProductId, request.getProductId());

        }
        return dao.selectCount(lambdaQueryWrapper);
    }


    /**
     * ????????????????????????????????????
     * @author Mr.Zhang
     * @since 2020-06-03
     * @return Integer
     */
    private Integer checkIsReply(StoreProductReply storeProductReply) {

        //??????????????????
        List<StoreOrderInfoOldVo> orderInfoVoList = storeOrderInfoService.getOrderListByOrderId(storeProductReply.getOid());
        if (null == orderInfoVoList || orderInfoVoList.size() < 1) {
            throw new BizException("????????????????????????");
        }

        boolean findResult = false;
        for (StoreOrderInfoOldVo orderInfoVo : orderInfoVoList) {
//            Integer productId = orderInfoVo.getInfo().getInteger("product_id");
            Integer productId = orderInfoVo.getInfo().getProductId();
            if (productId < 1) {
                continue;
            }

            if (storeProductReply.getProductId().equals(productId)) {
                findResult = true;
                break;
            }
        }

        if (!findResult) {
            throw new BizException("????????????????????????");
        }

        //?????????????????????
        Integer replyCount = getReplyCountByEntity(storeProductReply, false);
        if (replyCount > 0) {
            throw new BizException("??????????????????");
        }

        return orderInfoVoList.size();
    }

    /**
     * ????????????
     * @return Integer
     */
    private Integer getSumStar(Integer productId) {
        QueryWrapper<StoreProductReply> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("IFNULL(sum(product_score),0) as product_score", "IFNULL(sum(service_score),0) as service_score");
        queryWrapper.eq("is_del", 0);
        queryWrapper.eq("product_id", productId);
        StoreProductReply storeProductReply = dao.selectOne(queryWrapper);
        if (ObjectUtil.isNull(storeProductReply)) {
            return 0;
        }
        if (storeProductReply.getProductScore() == 0 || storeProductReply.getServiceScore() == 0) {
            return 0;
        }
        // ?????? = ??????????????? + ??????????????? / 2
        BigDecimal sumScore = new BigDecimal(storeProductReply.getProductScore() + storeProductReply.getServiceScore());
        BigDecimal divide = sumScore.divide(BigDecimal.valueOf(2L), 0, BigDecimal.ROUND_DOWN);
        return divide.intValue();
    }

    private Integer getCountByScore(Integer id, String type) {
        LambdaQueryWrapper<StoreProductReply> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StoreProductReply::getProductId,id);
        lqw.eq(StoreProductReply::getIsDel,false);
        switch (type) {
            case "all":
                break;
            case "good":
                lqw.apply( " (product_score + service_score) >= 8");
                break;
            case "medium":
                lqw.apply( " (product_score + service_score) < 8 and (product_score + service_score) > 4");
                break;
            case "poor":
                lqw.apply( " (product_score + service_score) <= 4");
                break;
        }

        return dao.selectCount(lqw);
    }
}
