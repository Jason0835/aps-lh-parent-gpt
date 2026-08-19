import { listMachine } from "@/api/dj/machine";


const state = {
  machines: []
}

const mutations = {
  SET_MACHINES: (state, list) => {
      state.machines = list;
  },

}

const actions = {
  getMachineList({ commit, state }, params) {
    return new Promise((resolve, reject) => {
      listMachine(params)
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
