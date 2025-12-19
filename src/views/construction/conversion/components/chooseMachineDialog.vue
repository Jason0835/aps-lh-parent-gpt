<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="500px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="100px"
      v-loading="loading"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import { getMachineInfoListByHalfPartType } from "@/api/cx/conversion";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        machineId: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      machines: [],
      index: null,
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.conversion.modelName");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.machine.machineName"),
          prop: "machineId",
          span: 24,
          disabled: false,
          type: "select",
          dictData: this.machines,
          listeners: {
            change: this.handleMachineChange,
          },
        },
      ];
    },
  },
  watch: {},

  methods: {
    // api
    async getMachineInfoListByHalfPartType() {
      try {
        this.loading = true;
        const res = await getMachineInfoListByHalfPartType({
          halfPartType: this.form.halfPartType,
        });
        console.log(res);
        this.machines = res.map((row) => {
          return {
            value: row.machineId,
            label: row.machineName,
          };
        });

        this.loading = false;
      } catch (e) {
        console.log(e);
        this.loading = false;
      }
    },

    async save(params) {
      try {
        this.loading = true;

        const res = await updateProductionStage(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    //utils
    show(data, index) {
      this.index = index;
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
        this.getMachineInfoListByHalfPartType();
      } else {
        //
      }
    },
    hide() {
      this.index = null;
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleMachineChange(val) {
      if (val) {
        const machine = this.machines.find((row) => row.value === val);
        if (machine) {
          this.form.machineName = machine.label;
        }
      }
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(() => {
        this.$emit(
          "success",
          {
            machineId: this.form.machineId,
            machineName: this.form.machineName,
          },
          this.index
        );

        this.$nextTick(() => {
          this.hide();
        })
      });
    },
  },
};
</script>


