<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <el-form
      class="form-item-height"
      ref="form"
      :model="form"
      :rules="rules"
      label-position="top"
      label-width="80px"
    >
      <el-form-item
        :label="$t('ui.data.column.scheduleResult.produceLine')"
        prop="machineCode"
      >
        <el-select
          class="w100"
          :placeholder="$t('ui.data.column.selectMachineName')"
          v-model="form.machineCode"
          filterable
        >
          <el-option
            v-for="item in machines"
            :key="item.machineCode"
            :value="item.machineCode"
            :label="item.machineName"
          ></el-option>
        </el-select>
      </el-form-item>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import {mapState} from "vuex";

import {batchChangeMachine} from "@/api/tm/tmScheduleResult.js";

export default {
  data() {
    return {
      loading: false,
      visible: false,
      form: {},
      rules: {
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      tableRows: [],
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.tm.machines,
    }),
    title: function () {
      return this.$t("ui.data.column.tm.scheduleResult.modelName");
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;

        const result = await batchChangeMachine(params.machineCode, {
          selects: JSON.stringify(this.tableRows),
        });
        if (result.code == 200) {
          this.$modal.msgSuccess(this.$t("common.message.success"));
          this.$emit("success");
          this.hide();
        }

        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    show(data) {
      this.visible = true;
      this.tableRows = data;
    },
    hide() {
      this.form = {};
      this.tableRows = [];
      this.$refs.form.resetForm();
      this.visible = false;
    },

    handleConfirm() {
      if (this.tableRows.length === 1) {
        if (this.tableRows[0].machineCode === this.form.machineCode) {
          this.hide();
          return;
        }
      }

      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save({
            ...this.form,
          });
        }
      });
    },
  },
};
</script>
