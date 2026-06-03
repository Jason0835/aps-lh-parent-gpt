import { listEnabledMachines } from "@/api/tq/machine";

const state = {
  machines: []
};

const mutations = {
  SET_MACHINES: (state, list) => {
    state.machines = list;
  }
};

const actions = {
  getMachineList({ commit, state }) {
    return new Promise((resolve, reject) => {
      listEnabledMachines()
        .then((res) => {
          commit("SET_MACHINES", res || []);
          resolve(res);
        })
        .catch((error) => {
          console.error("加载胎圈机台列表失败:", error);
          commit("SET_MACHINES", []);
          resolve({});
        });
    });
  }
};

export default {
  namespaced: true,
  state,
  mutations,
  actions
};
