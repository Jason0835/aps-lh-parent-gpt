import path from "path";
import { login, logout, getInfo, refreshToken } from "@/api/login";
import { getToken, setToken, setExpiresIn, removeToken } from "@/utils/auth";
import { changeLang, getLoginUser } from "@/api/user";
import cache from "@/plugins/cache";
import {isExternal} from "@/utils/validate";
import Cookies from "js-cookie";


function filterMenus(list) {
  return list.filter((item) => {
    if (item.children) {
      item.children = filterMenus(item.children);
    }
    if (!item.path) {
      console.warn("path 为空:", item);
      return false;
    }
    return true;
  });
}

function getColMenuCatchKey(userId) {
  let key = "COLLECT_MENUS";
  if (userId || userId === 0) {
    key += `_${userId}`;
  }
  return key;
}

function resolvePath(basePath, routePath, routeQuery) {
  if (isExternal(routePath)) {
    return routePath;
  }
  if (isExternal(basePath)) {
    return basePath;
  }
  if (routeQuery) {
    let query = JSON.parse(routeQuery);
    return { path: path.resolve(basePath, routePath), query: query };
  }
  return path.resolve(basePath, routePath);
}

function handleFilterByPath(menus, path, basePath) {
  const list = [];
  for (let i = 0; i < menus.length; i++) {
    if (!menus[i].hidden) {
      const fullPath = resolvePath(
        basePath || "",
        menus[i].path,
        menus[i].query
      );
      if (fullPath === path) {
        list.push({
          ...menus[i],
          children: null,
          fullPath,
        });
      } else if (menus[i].children && menus[i].children.length > 0) {
        const children = handleFilterByPath(
          menus[i].children,
          path,
          resolvePath(basePath || "", menus[i].path)
        );
        list.push(...children);
      }
    }
  }
  return list;
}

const user = {
  state: {
    token: getToken(),
    id: "",
    name: "",
    avatar: "",
    roles: [],
    permissions: [],
    collectMenu: [],
    collectPaths: '',
    nickName: "",
    dept: {
      deptId:'',
      deptName:'',
    }
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token;
    },
    SET_EXPIRES_IN: (state, time) => {
      state.expires_in = time;
    },
    SET_ID: (state, id) => {
      state.id = id;
    },
    SET_NAME: (state, name) => {
      state.name = name;
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar;
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles;
    },
    SET_PERMISSIONS: (state, permissions) => {
      state.permissions = permissions;
    },
    SET_COLLECT_MENU: (state, menus) =>  {
      state.collectMenu = menus;
    },
    SET_COLLECT_MENU_PATH: (state, paths) =>  {
      state.collectPaths = paths;
      cache.local.set(getColMenuCatchKey(state.id), paths.join(','))
    },
    SET_NICKNAME: (state, nickName) => {
      state.nickName = nickName;
    },
    SET_DEPT: (state,dept) => {
      state.dept.deptId = dept?.deptId;
      state.dept.deptName = dept?.deptName;
    }
  },

  actions: {
    // 登录
    Login({ commit, rootState }, userInfo) {
      const username = userInfo.username.trim();
      const password = userInfo.password;
      const code = userInfo.code;
      const uuid = userInfo.uuid;
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid)
          .then((res) => {
            if (res.code === 200) {
              // changeLang(rootState.app.language);
              Cookies.set("dept", "" )
              resolve();
            }
          })
          .catch((error) => {
            reject(error);
          });
      });
    },

    // 获取用户信息
    GetInfo({ commit, state }) {
      return new Promise((resolve, reject) => {
        getLoginUser()
          .then((res) => {
            const user = res.user;
            const avatar = require("@/assets/images/default-avator.png")
            
            const menus = filterMenus(res.menus);
            commit("app/SET_MENUS", menus);
            if (user.roles && user.roles.length > 0) {
              // 验证返回的roles是否是一个非空数组
              commit("SET_ROLES", res.roles);
              commit("SET_PERMISSIONS", res.permissions || []);
            } else {
              commit("SET_ROLES", ["ROLE_DEFAULT"]);
            }
            commit("SET_ID", user.userId);
            commit("SET_NAME", user.userName);
            commit("SET_AVATAR", avatar);
            commit("SET_NICKNAME", user.nickName);
            commit("SET_DEPT",user.dept);
            resolve(res);
          })
          .catch((error) => {
            reject(error);
          });
      });
    },

    // 刷新token
    RefreshToken({ commit, state }) {
      return new Promise((resolve, reject) => {
        refreshToken(state.token)
          .then((res) => {
            setExpiresIn(res.data);
            commit("SET_EXPIRES_IN", res.data);
            resolve();
          })
          .catch((error) => {
            reject(error);
          });
      });
    },

    // 退出系统
    LogOut({ commit, state }) {
      return new Promise((resolve, reject) => {
        logout(state.token)
          .then(() => {
            Cookies.remove("dept");
            commit("SET_TOKEN", "");
            commit("SET_ROLES", []);
            commit("SET_PERMISSIONS", []);
            removeToken();
            resolve();
          })
          .catch((error) => {
            // reject(error);
            location.reload();
          });
      });
    },

    // 前端 登出
    FedLogOut({ commit }) {
      return new Promise((resolve) => {
        commit("SET_TOKEN", "");
        removeToken();
        resolve();
      });
    },

    handleCollectMenu({ commit, state, rootState, dispatch }, path) {
      const collectPaths = [...state.collectPaths]
      if (path && !collectPaths.includes(path)) {
        collectPaths.push(path)
      }
      commit('SET_COLLECT_MENU_PATH', collectPaths)
      dispatch('updateCollectMenu')
    },

    handleREMOVECollectMenu({ commit, state, rootState, dispatch }, path) {
      const collectPaths = [...state.collectPaths]
      const index = collectPaths.indexOf(path)
      if (index !== -1) {
        collectPaths.splice(index, 1)
      }
      commit('SET_COLLECT_MENU_PATH', collectPaths)
      dispatch('updateCollectMenu')
    },

    // 更新收藏菜单
    updateCollectMenu({ commit, state, rootState }) {
      const menus = rootState.permission.sidebarRouters
      if (!state.id || !menus) {
        return
      }
      const list = [];
      const str = cache.local.get(getColMenuCatchKey(state.id));
      const paths = str ? str.split(",") : [];
      for (let i = 0; i < paths.length; i++) {
        const arr = handleFilterByPath(menus, paths[i]);
        if (arr[0]) {
          list.push(arr[0]);
        }
      }
      commit('SET_COLLECT_MENU', list)
      commit('SET_COLLECT_MENU_PATH', paths)
    }
  },
};

export default user;
