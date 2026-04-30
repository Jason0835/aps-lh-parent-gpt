import { listMachine } from "@/api/lh/machine";
// import { listVulcanizingMachine } from "@/api/mdm/vulcanizingMachine"


const state = {
  machines: []
}

const mutations = {
  SET_MACHINES: (state, list) => {
      state.machines = list;
  },

}

const actions = {
  getMachineList({ commit, state }) {
    return new Promise((resolve, reject) => {
      listMachine()
        .then((res) => {
          commit("SET_MACHINES", res.rows || []);
          resolve(res);
        })
        .catch((error) => {
          console.error("Failed to load curing machines:", error);
          // 即使API失败也继续，不阻断页面
          commit("SET_MACHINES", []);
          resolve({});
        });
    });
  },
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
