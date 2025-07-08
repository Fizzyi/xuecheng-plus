package com.xuecheng.content.service.lmpl;


import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        // 1. 查询所有以id为根节点的分类（包括根节点本身）
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);

        // 2. 把所有分类（除了根节点）放到一个Map里，key是分类id，value是分类对象
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream()
                .filter(item -> !id.equals(item.getId()))
                .collect(Collectors.toMap(
                        key -> key.getId(),
                        value -> value,
                        (key1, key2) -> key2 // 如果有重复的 key，保留后面的 value
                ));

        // 3. 用来存放最终要返回的树形结构的根节点（即一级分类）
        List<CourseCategoryTreeDto> categoryTreeDtos = new ArrayList<>();

        // 4. 遍历所有分类（除了根节点）
        courseCategoryTreeDtos.stream()
                .filter(item -> !id.equals(item.getId()))
                .forEach(item -> {
                    // 4.1 如果当前节点的父id等于根节点id，说明它是一级分类，加入到返回列表
                    if (item.getParentid().equals(id)) {
                        categoryTreeDtos.add(item);
                    }
                    // 4.2 找到当前节点的父节点对象
                    CourseCategoryTreeDto courseCategoryTreeDto = mapTemp.get(item.getParentid());
                    if (courseCategoryTreeDto != null) {
                        // 4.3 如果父节点还没有子节点列表，先创建一个
                        if (courseCategoryTreeDto.getChildTreeNodes() == null) {
                            courseCategoryTreeDto.setChildTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                        }
                        // 4.4 把“父节点自己”加到自己的子节点列表里
                        courseCategoryTreeDto.getChildTreeNodes().add(item);
                    }
                });

        // 5. 返回一级分类（每个一级分类下会有自己的子分类，形成树结构）
        return categoryTreeDtos;
    }
}
