package org.jiang.shpping.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jiang.shpping.dao.CategoryDao;
import org.jiang.shpping.entity.Category;
import org.jiang.shpping.service.CategoryService;
import org.jiang.shpping.vo.CategoryTreeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, Category> implements CategoryService {

    @Autowired
    private CategoryDao dao;

    @Override
    public List<Category> getChildVoListByPid(Integer cid) {
        LambdaQueryWrapper<Category> categoryLambdaQueryWrapper = new LambdaQueryWrapper<>();
        categoryLambdaQueryWrapper.eq(Category::getStatus,1);
        categoryLambdaQueryWrapper.like(Category::getPath,"/"+cid+"/");
        return dao.selectList(categoryLambdaQueryWrapper);
    }

    /**
     * 带结构的无线级分类
     * @author Mr.Zhang
     * @since 2020-04-16
     */

    @Override
    public List<CategoryTreeVo> getListTree(Integer type, Integer status, String name) {
        return getTree(type, status,name,null);
    }

    /**
     * 带结构的无线级分类
     * @author Mr.Zhang
     * @since 2020-04-16
     */
    private List<CategoryTreeVo> getTree(Integer type, Integer status,String name, List<Integer> categoryIdList) {
        //循环数据 把数据对象变成带list结构的vo
        ArrayList<CategoryTreeVo> treeList = new ArrayList<>();
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Category::getType,type);
        if (null != categoryIdList && categoryIdList.size() > 0) {
            lambdaQueryWrapper.in(Category::getId,categoryIdList);
        }
        if (status >=0) {
            lambdaQueryWrapper.eq(Category::getStatus,status);
        }
        if (StringUtils.isNotBlank(name)) { //根据名称模糊搜索
            lambdaQueryWrapper.like(Category::getName,name);
        }
        lambdaQueryWrapper.orderByDesc(Category::getSort);
        lambdaQueryWrapper.orderByAsc(Category::getId);
        List<Category> allTree = dao.selectList(lambdaQueryWrapper);
        if(allTree == null){
            return null;
        }
        // 根据名称搜索特殊处理 这里仅仅处理两层搜索后有子父级关系的数据
        if(com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(name) && allTree.size() >0){
            List<Category> searchCategory = new ArrayList<>();
            List<Integer> categoryIds = allTree.stream().map(Category::getId).collect(Collectors.toList());

            List<Integer> pidList = allTree.stream().filter(c -> c.getPid() > 0 && !categoryIds.contains(c.getPid()))
                    .map(Category::getPid).distinct().collect(Collectors.toList());
            if (CollUtil.isNotEmpty(pidList)) {
                pidList.forEach(pid -> {
                    searchCategory.add(dao.selectById(pid));
                });
            }
            allTree.addAll(searchCategory);
        }

        for (Category category: allTree) {
            CategoryTreeVo categoryTreeVo = new CategoryTreeVo();
            BeanUtils.copyProperties(category, categoryTreeVo);
            treeList.add(categoryTreeVo);
        }


        //返回
        Map<Integer, CategoryTreeVo> map = new HashMap<>();
        //ID 为 key 存储到map 中
        for (CategoryTreeVo categoryTreeVo1 : treeList) {
            map.put(categoryTreeVo1.getId(), categoryTreeVo1);
        }

        List<CategoryTreeVo> list = new ArrayList<>();
        for (CategoryTreeVo tree : treeList) {
            //子集ID返回对象，有则添加。
            CategoryTreeVo tree1 = map.get(tree.getPid());
            if(tree1 != null){
                tree1.getChild().add(tree);
            }else {
                list.add(tree);
            }
        }
        System.out.println("无限极分类 : getTree:" + JSON.toJSONString(list));
        return list;

    }
}
