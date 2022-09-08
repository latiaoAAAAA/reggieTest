package cn.edu.lingnan.service.impl;

import cn.edu.lingnan.common.R;
import cn.edu.lingnan.dto.DishDto;
import cn.edu.lingnan.dto.SetmealDto;
import cn.edu.lingnan.entity.*;
import cn.edu.lingnan.mapper.SetmealMapper;
import cn.edu.lingnan.service.CategoryService;
import cn.edu.lingnan.service.SetmealDishService;
import cn.edu.lingnan.service.SetmealService;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 分页获取套餐信息
     * @param page
     * @param pageSize
     * @param name
     * @return
     */ //61d20592-b37f-4d72-a864-07ad5bb8f3bb.jpg
    @Override
    public R<Page> list(Integer page, Integer pageSize, String name) {
        log.info("获取菜品，第{}页的{}个",page,pageSize);
        Page<Setmeal> pageInfo = new Page<>(page,pageSize);
        page(pageInfo,new LambdaQueryWrapper<Setmeal>().like(StringUtils.isNotEmpty(name), Setmeal::getName, name).orderByDesc(Setmeal::getUpdateTime));
        Page<SetmealDto> setmealDtoPageInfo = new Page<>();

        BeanUtil.copyProperties(pageInfo,setmealDtoPageInfo,"records");

        List<Setmeal> setmealList = pageInfo.getRecords();
        List<SetmealDto> setmealDtoList = setmealList.stream().map((setmeal) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtil.copyProperties(setmeal, setmealDto);
            Long categoryId = setmeal.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        setmealDtoPageInfo.setRecords(setmealDtoList);

        if (setmealDtoPageInfo==null) {
            return R.error("查询失败！");
        }
        return R.success(setmealDtoPageInfo);
    }

    /**
     * 添加套餐
     * @param setmealDto
     * @return
     */
    @Override
    public R<String> saveWithSetmealDto(SetmealDto setmealDto) {
        boolean isSuccessSetmeal = save(setmealDto);
        if (!isSuccessSetmeal) {
            return R.error("添加套餐失败！");
        }
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes().stream().map(setmealDish -> {
            setmealDish.setSetmealId(setmealDto.getId());
            return setmealDish;
        }).collect(Collectors.toList());
        boolean isSuccessSetmealDish = setmealDishService.saveBatch(setmealDishes);
        if (!isSuccessSetmealDish) {
            return R.error("添加套餐中的菜品失败！");
        }
        return R.success("添加套餐成功！");
    }

    /**
     * 套餐信息回显
     * @param id
     * @return
     */
    @Override
    public R<SetmealDto> getOneById(Long id) {
        Setmeal setmeal = getById(id);
        List<SetmealDish> setmealDishList = setmealDishService.list(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, id));
        SetmealDto setmealDto = BeanUtil.copyProperties(setmeal, SetmealDto.class);
        setmealDto.setSetmealDishes(setmealDishList);
        return R.success(setmealDto);
    }

    /**
     * 修改套餐信息
     * @param setmealDto
     * @return
     */
    @Override
    public R<String> updateWithSetmealDto(SetmealDto setmealDto) {
        boolean isSuccessSetmeal = updateById(setmealDto);
        if (!isSuccessSetmeal) {
            return R.error("修改套餐信息失败！");
        }
        //删除原来的菜品信息
        setmealDishService.remove(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId,setmealDto.getId()));
        //获取setmealDto中的菜品信息
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes().stream().map(setmealDish -> {
            setmealDish.setSetmealId(setmealDto.getId());
            return setmealDish;
        }).collect(Collectors.toList());
        //新增菜品信息
        boolean isSuccessSetmealDish = setmealDishService.saveBatch(setmealDishes);
        if (!isSuccessSetmealDish) {
            return R.error("修改套餐菜品失败！");
        }
        return R.success("修改套餐信息成功！");
    }

    /**
     * 批量停售或起启售
     * @param status
     * @param ids
     * @return
     */
    @Override
    public R<String> updateStatusBatchByIds(Integer status, long[] ids) {
        Integer count = setmealMapper.updateStatusBatchByIds(status, ids);
        if (count==0){
            return R.error(status==0?"停售失败！":"启售失败！");
        }
        return R.success(status==0?"停售成功！":"启售成功！");
    }

    /**
     * 批量删除套餐数据
     * @param ids
     * @return
     */
    @Override
    public R<String> removeDishAntFlavorByIds(List<Long> ids) {
        boolean isSuccessSetmeal = removeByIds(ids);
        if (!isSuccessSetmeal) {
            return R.error("删除失败！");
        }
        boolean isSuccessSetmealDish = setmealDishService.removeSetmealDishByDishId(ids);
        if (!isSuccessSetmealDish) {
            return R.error("删除失败！");
        }
        return R.success("删除成功！");
    }
}
