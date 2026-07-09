import { listTcMachineInfo } from "@/api/tc/machineInfo";


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
      listTcMachineInfo()
        .then((res) => {
          commit("SET_MACHINES", res.rows);
          resolve(res);
        })
        .catch((error) => {
          reject(error);
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
