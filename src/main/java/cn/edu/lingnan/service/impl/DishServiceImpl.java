package cn.edu.lingnan.service.impl;

import cn.edu.lingnan.common.R;
import cn.edu.lingnan.dto.DishDto;
import cn.edu.lingnan.entity.Category;
import cn.edu.lingnan.entity.Dish;
import cn.edu.lingnan.entity.DishFlavor;
import cn.edu.lingnan.mapper.DishMapper;
import cn.edu.lingnan.service.CategoryService;
import cn.edu.lingnan.service.DishFlavorService;
import cn.edu.lingnan.service.DishService;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private DishMapper dishMapper;

    /**
     * 分页获取菜品 Dish
     * @param page 从第几页开始
     * @param pageSize 一页多少条
     * @param name 搜索框输入内容
     * @return Page,内含list，total
     */
    @Override
    public R<Page> list(Integer page, Integer pageSize, String name) {
        log.info("获取菜品,第{}页/{}个",page,pageSize);
        //创建分页page
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        //查询数据库
        page(pageInfo,new LambdaQueryWrapper<Dish>().like(StringUtils.isNotEmpty(name), Dish::getName, name).orderByDesc(Dish::getUpdateTime));
        //创建分页DTOPage
        Page<DishDto> dishDtoPageInfo = new Page<>();
        //复制参数
        BeanUtil.copyProperties(pageInfo,dishDtoPageInfo,"records");
        //dish->dishDto
        List<Dish> dishList = pageInfo.getRecords();
        List<DishDto> dishDtoList = dishList.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtil.copyProperties(item, dishDto);
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());
        //封装dtoPage
        dishDtoPageInfo.setRecords(dishDtoList);
        if (dishDtoPageInfo==null) {
            return R.error("查询失败！");
        }
        return R.success(dishDtoPageInfo);
    }

    /**
     * 修改数据时---回显数据  Dish
     * @param id
     * @return
     */
    @Override
    public R<DishDto> getOneById(Long id) {
        Dish dish = getById(id);
        List<DishFlavor> dishFlavorList = dishFlavorService.list(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, id));
        DishDto dishDto = BeanUtil.copyProperties(dish, DishDto.class);
        dishDto.setFlavors(dishFlavorList);
        log.info("菜品数据回显,id:{}",id);
        return R.success(dishDto);
    }

    /**
     * 添加菜品 Dish
     * @param dishDto
     * @return
     */
    @Override
    @Transactional
    public R<String> saveWithDishDto(DishDto dishDto) {
        //保存进数据库
        boolean isSuccessDish = save(dishDto);
        if (!isSuccessDish) {
            return R.error("添加菜品失败！");
        }
        //封装 口味 的列表进Dto
        List<DishFlavor> flavors = dishDto.getFlavors().stream().map(dishFlavor -> {
            dishFlavor.setDishId(dishDto.getId());
            return dishFlavor;
        }).collect(Collectors.toList());
        //保存口味信息进数据库
        boolean isSuccessFlavor = dishFlavorService.saveBatch(flavors);
        if (!isSuccessFlavor) {
            return R.error("添加菜品口味失败！");
        }
        log.info("添加{}菜品及口味",dishDto.getName());
        return R.success("添加菜品成功！");
    }

    /**
     * 修改菜品信息 Dish
     * @param dishDto
     * @return
     */
    @Override
    @Transactional
    public R<String> updateWithDishDto(DishDto dishDto) {
        //修改菜品数据
        boolean isSuccessDish = updateById(dishDto);
        if (!isSuccessDish) {
            return R.error("修改菜品信息失败！");
        }
        //删除原来的口味数据
        dishFlavorService.remove(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId,dishDto.getId()));
        //获取dishDto中的口味数据
        List<DishFlavor> flavors = dishDto.getFlavors().stream().map(dishFlavor -> {
            dishFlavor.setDishId(dishDto.getId());
            return dishFlavor;
        }).collect(Collectors.toList());
        //新增口味数据
        boolean isSuccessFlavor = dishFlavorService.saveBatch(flavors);
        if (!isSuccessFlavor) {
            return R.error("修改菜品口味失败！");
        }
        log.info("修改{}菜品数据",dishDto.getName());
        return R.success("修改菜品信息成功！");
    }

    /**
     * （批量）停售或起售 Dish
     * @param status
     * @param ids
     * @return
     */
    @Override
    public R<String> updateStatusBatchByIds(Integer status, long[] ids) {
        Integer count = dishMapper.updateStatusBatchByIds(status, ids);
        if (count==0){
            return R.error(status==0?"停售失败！":"启售失败！");
        }
        log.info("id为{}的菜品{}",ids,status==0?"停售":"起售");
        return R.success(status==0?"停售成功！":"启售成功！");
    }

    /**
     * 批量删除菜品数据 Dish
     * @param ids
     * @return
     */
    @Override
    @Transactional
    public R<String> removeDishAntFlavorByIds(List<Long> ids) {
        List<String> list = dishMapper.getStatusDish(ids);
        if (!list.isEmpty()) {
            return R.error(list.toString()+"菜品正在售卖，不能删除！");
        }
        boolean isSuccessDish = removeByIds(ids);
        if (!isSuccessDish) {
            return R.error("删除失败！");
        }
        boolean isSuccessFlavor = dishFlavorService.removeFlavorByDishId(ids);
        if (!isSuccessFlavor) {
            return R.error("删除失败！");
        }
        log.info("删除id为{}的菜品",ids);
        return R.success("删除成功！");
    }

    /**
     * 管理端 添加套餐时获取菜品数据 Dish
     * 用户端 菜品列表展示与菜品详细的展示 Dish
     * @param categoryId
     * @return
     */
    @Override
    public R<List> listDishByCategoryId(Long categoryId, String name, Integer status) {
        status = status==null?1:status;
        List<Dish> dishList = null;
        List<DishDto> dishDtoList = null;
        if (categoryId!=null){
            dishList = list(new LambdaQueryWrapper<Dish>().eq(Dish::getCategoryId, categoryId).eq(Dish::getStatus,status));
            dishDtoList = dishList.stream().map(dish -> {
                DishDto dishDto = new DishDto();
                BeanUtil.copyProperties(dish,dishDto);
                Category category = categoryService.getById(categoryId);
                if (category != null) {
                    String categoryName = category.getName();
                    dishDto.setCategoryName(categoryName);
                }
                List<DishFlavor> dishFlavorList = dishFlavorService.list(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, dish.getId()));
                dishDto.setFlavors(dishFlavorList);
                return dishDto;
            }).collect(Collectors.toList());
        }
        if (name!=null && name!=""){
            dishList = list(new LambdaQueryWrapper<Dish>().like(StringUtils.isNotEmpty(name),Dish::getName,name).eq(Dish::getStatus,status));
            dishDtoList = dishList.stream().map(dish -> {
                DishDto dishDto = new DishDto();
                BeanUtil.copyProperties(dish,dishDto);
                Category category = categoryService.getById(categoryId);
                if (category != null) {
                    String categoryName = category.getName();
                    dishDto.setCategoryName(categoryName);
                }
                List<DishFlavor> dishFlavorList = dishFlavorService.list(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, dish.getId()));
                dishDto.setFlavors(dishFlavorList);
                return dishDto;
            }).collect(Collectors.toList());
        }

        if (dishDtoList ==null || dishDtoList.isEmpty()) {
            return R.error("查询所有菜品失败！");
        }
        return R.success(dishDtoList);
    }
}
