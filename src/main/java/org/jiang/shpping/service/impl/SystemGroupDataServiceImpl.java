package org.jiang.shpping.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.SystemGroupDataDao;
import org.jiang.shpping.entity.SystemGroupData;
import org.jiang.shpping.request.SystemFormItemCheckRequest;
import org.jiang.shpping.request.SystemGroupDataSearchRequest;
import org.jiang.shpping.service.SystemGroupDataService;
import org.jiang.shpping.utils.JanUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class SystemGroupDataServiceImpl extends ServiceImpl<SystemGroupDataDao, SystemGroupData> implements SystemGroupDataService {

    @Resource
    private SystemGroupDataDao dao;


    /**
     * 通过gid获取列表 推荐二开使用
     *
     * @param gid Integer group id
     * @param cls
     * @return List<T>
     */
    @Override
    public <T> List<T> getListByGid(Integer gid, Class<T> cls) {
        SystemGroupDataSearchRequest systemGroupDataSearchRequest = new SystemGroupDataSearchRequest();
        systemGroupDataSearchRequest.setGid(gid);
        systemGroupDataSearchRequest.setStatus(true);
        List<SystemGroupData> list = getList(systemGroupDataSearchRequest);

        List<T> arrayList = new ArrayList<>();
        if (list.size() < 1) {
            return null;
        }

        for (SystemGroupData systemGroupData : list) {
            JSONObject jsonObject = JSONObject.parseObject(systemGroupData.getValue());
            List<SystemFormItemCheckRequest> systemFormItemCheckRequestList = JanUtils.jsonToListClass(jsonObject.getString("fields"), SystemFormItemCheckRequest.class);
            if (systemFormItemCheckRequestList.size() < 1) {
                continue;
            }
            HashMap<String, Object> map = new HashMap<>();
            T t;
            for (SystemFormItemCheckRequest systemFormItemCheckRequest : systemFormItemCheckRequestList) {
                map.put(systemFormItemCheckRequest.getName(), systemFormItemCheckRequest.getValue());
            }
            map.put("id", systemGroupData.getId());
            t = JanUtils.mapToObj(map, cls);
            arrayList.add(t);
        }

        return arrayList;
    }


    public List<SystemGroupData> getList(SystemGroupDataSearchRequest request) {


        //带 SystemGroupData 类的多条件查询
        LambdaQueryWrapper<SystemGroupData> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        SystemGroupData model = new SystemGroupData();
        BeanUtils.copyProperties(request, model);
        lambdaQueryWrapper.setEntity(model);
        lambdaQueryWrapper.orderByAsc(SystemGroupData::getSort).orderByAsc(SystemGroupData::getId);
        return dao.selectList(lambdaQueryWrapper);
    }
}
