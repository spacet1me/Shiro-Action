package im.zhaojun.service;

import im.zhaojun.mapper.MenuMapper;
import im.zhaojun.model.Menu;
import im.zhaojun.model.User;
import im.zhaojun.model.vo.MenuTreeVO;
import im.zhaojun.util.MenuVOConvert;
import im.zhaojun.util.TreeUtil;
import org.apache.shiro.SecurityUtils;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@CacheConfig(cacheNames = "menu", keyGenerator = "springCacheKeyGenerator")
public class MenuService {

    @Resource
    private MenuMapper menuMapper;

    /**
     * 获取所有菜单(导航菜单和按钮)
     */
    @Cacheable
    public List<Menu> selectAll() {
        return menuMapper.selectAll();
    }

    /**
     * 获取所有导航菜单
     */
    @Cacheable
    public List<Menu> selectAllMenuAndPage() {
        return menuMapper.selectAllMenu();
    }

    public Menu selectOne(Integer id) {
        return menuMapper.selectByPrimaryKey(id);
    }

    /**
     * 获取所有菜单 (树形结构)
     */
    @Cacheable
    public List<MenuTreeVO> getALLMenuTreeVO() {
        List<Menu> menus = selectAllMenuAndPage();
        List<MenuTreeVO> menuTreeVOS = MenuVOConvert.menuToTreeVO(menus);
        return TreeUtil.toTree(menuTreeVOS);
    }

    /**
     * 获取当前登陆用户拥有的树形菜单
     */
    @Cacheable
    public List<MenuTreeVO> selectCurrentUserMenuTreeVO() {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        List<Menu> menus = menuMapper.selectMenuByUserName(user.getUsername());
        List<MenuTreeVO> menuTreeVOS = MenuVOConvert.menuToTreeVO(menus);
        return TreeUtil.toTree(menuTreeVOS);
    }

    @CacheEvict(allEntries = true)
    public int add(Menu menu) {
        menuMapper.insert(menu);
        return menu.getMenuId();
    }

    @CacheEvict(allEntries = true)
    public boolean update(Menu menu) {
        return menuMapper.updateByPrimaryKey(menu) == 1;
    }


    /**
     * 删除当前菜单以及其子菜单
     */
    @CacheEvict(allEntries = true)
    public boolean deleteByIDAndChildren(Integer id) {
        List<Integer> childIDList = menuMapper.selectChildrenID(id);
        for (Integer childID : childIDList) {
            deleteByIDAndChildren(childID);
        }
        return menuMapper.deleteByPrimaryKey(id) == 1;
    }

    /**
     * 从数据库加载权限列表
     */
    public Map<String, String> getUrlPermsMap() {
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();

        // 系统默认过滤器
        filterChainDefinitionMap.put("/favicon.ico", "anon");
        filterChainDefinitionMap.put("/css/**", "anon");
        filterChainDefinitionMap.put("/fonts/**", "anon");
        filterChainDefinitionMap.put("/images/**", "anon");
        filterChainDefinitionMap.put("/js/**", "anon");
        filterChainDefinitionMap.put("/lib/**", "anon");
        filterChainDefinitionMap.put("/login", "anon");
        filterChainDefinitionMap.put("/register", "anon");

        // 验证码
        filterChainDefinitionMap.put("/captcha", "anon");
        // 检查用户名是否存在
        filterChainDefinitionMap.put("/checkUser", "anon");
        // 激活账号
        filterChainDefinitionMap.put("/active", "anon");
        filterChainDefinitionMap.put("/user/1/disable", "perms[xxxx]");
        List<Menu> menus = selectAll();
        for (Menu menu : menus) {
            String url = menu.getUrl();
            if (url != null) {
                if (menu.getMethod() != null && !"".equals(menu.getMethod())) {
                    url += ("==" + menu.getMethod());
                }
                String perms = "perms[" + menu.getPerms() + "]";
                filterChainDefinitionMap.put(url, perms);
            }
        }

        filterChainDefinitionMap.put("/**", "authc");
        return filterChainDefinitionMap;
    }

    public int count() {
        return menuMapper.count();
    }
}