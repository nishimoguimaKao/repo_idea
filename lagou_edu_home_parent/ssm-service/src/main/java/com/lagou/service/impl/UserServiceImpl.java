package com.lagou.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lagou.dao.UserMapper;
import com.lagou.domain.*;
import com.lagou.service.UserService;
import com.lagou.utils.Md5;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;


    /*
        用户分页&多条件组合查询
     */
    @Override
    public PageInfo findAllUserByPage(UserVo userVo) {

        PageHelper.startPage(userVo.getCurrentPage(),userVo.getPageSize());
        List<User> allUserByPage = userMapper.findAllUserByPage(userVo);

        PageInfo<User> pageInfo = new PageInfo<>(allUserByPage);
        return pageInfo;
    }

    /*
        用户登陆
    */
    @Override
    public User login(User user) throws Exception {

        User user1 = userMapper.login(user);

        if (user1!=null && Md5.verify(user.getPassword(),"lagou",user1.getPassword())){
            return user1;
        } else {
            return null;
        }

    }

    /*
        根据ID查询用户当前角色 分配角色（回显）
     */
    @Override
    public List<Role> findUserRelationRoleById(int id) {
        return userMapper.findUserRelationRoleById(id);
    }

    /*

     */
    @Override
    public void userContextRole(UserVo userVo) {

        // 1.根据用户id清空中间表关系
        userMapper.deleteUserContextRole(userVo.getUserId());

        // 2.再重新建立关系
        for (Integer integer : userVo.getRoleIdList()) {

            // 封装数据
            User_Role_relation user_role_relation = new User_Role_relation();
            user_role_relation.setUserId(userVo.getUserId());
            user_role_relation.setRoleId(integer);

            Date date = new Date();
            user_role_relation.setCreatedTime(date);
            user_role_relation.setUpdatedTime(date);
            user_role_relation.setCreatedBy("system");
            user_role_relation.setUpdatedby("system");

            userMapper.userContextRole(user_role_relation);
        }
    }

    /*
        获取用户权限，进行菜单动态显示
     */
    @Override
    public ResponseResult getUserPermissions(Integer userId) {

        // 1.获取当前用户拥有的角色
        List<Role> roleList = userMapper.findUserRelationRoleById(userId);

        // 2.获取角色id，保存到List集合中
        ArrayList<Integer> roleIds = new ArrayList<>();

        for (Role role : roleList) {
            roleIds.add(role.getId());
        }

        // 3.根据角色id查询父菜单
        List<Menu> parentMenu = userMapper.findParentMenuByRoleId(roleIds);

        // 4.查询并且封装父菜单关联的子菜单
        for (Menu menu : parentMenu) {
            List<Menu> subMenu = userMapper.findSubMenuByPid(menu.getId());
            menu.setSubMenuList(subMenu);
        }

        // 5.获取资源信息
        List<Resource> resourceList = userMapper.findResourceByRoleId(roleIds);

        // 6.封装数据并返回
        Map<String, Object> map = new HashMap<>();
        map.put("menuList",parentMenu);
        map.put("resourceList",resourceList);

        return new ResponseResult(true,200,"获取用户权限信息成功！",map);
    }
}
