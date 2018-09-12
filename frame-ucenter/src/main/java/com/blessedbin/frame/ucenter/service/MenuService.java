package com.blessedbin.frame.ucenter.service;

import com.blessedbin.frame.common.Pagination;
import com.blessedbin.frame.common.ui.CascaderNode;
import com.blessedbin.frame.ucenter.entity.dto.MenuTreeDto;
import com.blessedbin.frame.ucenter.entity.pojo.Menu;
import com.blessedbin.frame.ucenter.modal.SysPermission;
import com.blessedbin.frame.ucenter.modal.SysRolePermission;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.EMPTY_LIST;

/**
 * Created by xubin on 2018/7/9.
 *
 * @author 37075
 * @date 2018/7/9
 * @time 14:35
 * @tool intellij idea
 */
@Service
@Log4j2
public class MenuService {

    @Autowired
    private PermissionService permissionService;


    @Autowired
    private ObjectMapper objectMapper;


    private List<Menu> allMenus(){
        List<SysPermission> permissions = permissionService.selectByType(SysPermission.TYPE_MENU);
        return permissions.stream().map(permission -> {
            try {
                Menu menu = objectMapper.readValue(permission.getAdditionInformation(), Menu.class);
                menu.setId(permission.getPermissionId());
                return menu;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
    }


    public List<MenuTreeDto>  getMenuTree(){
        return buildMenuTree(allMenus(),-1);
    }

    private List<MenuTreeDto> buildMenuTree(List<Menu> menus,Integer pid){
        return menus.stream().filter(menu -> menu.getPid().equals(pid)).map(menu -> {
            MenuTreeDto dto = new MenuTreeDto();
            BeanUtils.copyProperties(menu,dto);
            dto.setChildren(buildMenuTree(menus,menu.getId()));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 获取用户菜单列表
     * @param uuid 用户唯一编码
     * @return
     */
    public List<MenuTreeDto> getUserMenu(String uuid){
        Assert.notNull(uuid,"uuid is not null");

        return getMenuTree();
        // TODO 逻辑方面的优化，Role信息从参数获取或者从缓存获取，而不是每次查询数据库
        /*List<SysRole> roles = roleMapper.selectRolesByUUid(uuid);
        boolean b = roles.stream().map(SysRole::getRoleKey).anyMatch(s -> "ROLE_ADMIN".equals(s));
        List<SysMenu> menus;
        if(b){
            menus = menuMapper.selectAllMenus();
        }else {
            // 去重，TODO 在数据库中做去重操作
            menus = menuMapper.selectMenusByUuidAndEnabled(uuid).stream()
                    .distinct().collect(Collectors.toList());
        }

        return menus.stream().filter(menu -> menu.getPid() == null)
                .map(menu -> {
                    MenuTreeDto dto = new MenuTreeDto();
                    BeanUtils.copyProperties(menu,dto);
                    dto.setChildren(buildUserMenuTree(menus,menu.getPermissionId()));
                    return dto;
                }).collect(Collectors.toList());*/

    }


    public List<CascaderNode> getCascaders() {
        List<CascaderNode> nodes = new ArrayList<>();
        nodes.add(CascaderNode.builder().value("-1").label("一级菜单").build());
        List<CascaderNode> cascaderNodes = buildCascader(null);
        if (cascaderNodes != null) {
            nodes.addAll(cascaderNodes);
        }
        return nodes;
    }

    private List<CascaderNode> buildCascader(Integer parentId) {
       /* return getAllByPid(parentId).stream().map(menu -> CascaderNode.builder()
                    .value(String.valueOf(menu.getPermissionId()))
                    .label(menu.getTitle())
                    .children(buildCascader(menu.getPermissionId()))
                    .build())
                .collect(Collectors.toList());*/
       return EMPTY_LIST;
    }



    /**
     * 添加菜单
     * @param menu 要添加的数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void addMenu(Menu menu) {

        SysPermission permission = new SysPermission();
        permission.setName(menu.getTitle());
        permission.setCreateTime(new Date());
        permission.setUpdateTime(new Date());
        permission.setType(SysPermission.TYPE_MENU);
        permission.setSort(menu.getSort());
        permission.setCode("menu::" + menu.getName());

        try {
            permission.setAdditionInformation(objectMapper.writeValueAsString(menu));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        permissionService.insertSelective(permission);
    }


    @Transactional(rollbackFor = Exception.class)
    public int deleteByPk(Integer id) {
        return permissionService.deleteByPk(id);
    }

    /**
     * TODO 更新字段的逻辑问题
     * @param menu 要修改的对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateByPkSelective(Menu menu) {

        SysPermission prePermission = permissionService.selectByPk(menu.getId());

        //TODO
    }

    /**
     * TODO
     * 查找某角色下对应的已选菜单权限,只返回叶子节点
     * @param roleId
     * @return
     */
    public List<SysRolePermission> selectRolePermissionsByRoleId(Integer roleId) {
        return null;
    }


    /**
     * TODO
     * 检查菜单是否有子菜单
     * @param menuId menu id
     * @return 若有子菜单，返回true，否则返回false
     */
    public boolean hasChildren(Integer menuId){
        return true;
    }

    /**
     * TODO
     * 检验功能点ID是否合法
     * @param menuId
     * @return
     */
    public boolean checkActionExistsByPk(Integer menuId) {
        return true;
    }

    /**
     * 保存权限点和API的关系
     * @param actionId
     * @param selected
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveActionApiRelation(Integer actionId, List<Integer> selected) {

        //TODO
        log.debug("成功更新{}条数据");

    }

    /**
     *
     * 获取menu
     * @param id menu的Id
     * @return
     */
    public Menu getMenu(Integer id) {
        SysPermission permission = permissionService.selectByPk(id);
        Menu menu = null;
        try {
            menu = objectMapper.readValue(permission.getAdditionInformation(), Menu.class);
            menu.setId(permission.getPermissionId());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return menu;
    }
}
