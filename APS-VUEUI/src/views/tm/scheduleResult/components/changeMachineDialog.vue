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
            v-for="item in availableMachines"
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
import {listTmShiftConfig} from "@/api/tm/shiftConfig";
import {resolveErrorMessage} from "@/utils/errorMessage";

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
      shiftConfigs: [],
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.tm.machines,
    }),
    title: function () {
      return this.$t("ui.data.column.tm.scheduleResult.modelName");
    },
    availableMachines() {
      const requiredShiftOrders = [...new Set(this.tableRows.flatMap(row =>
        Array.from({length: 6}, (item, index) => index + 1)
          .filter(shiftOrder => Number(row[`class${shiftOrder}PlanQty`] || 0) > 0)
      ))];
      return this.machines.filter(machine => {
        const openShiftCodes = this.machineOpenShiftCodes(machine);
        return requiredShiftOrders.every(shiftOrder => {
          const shiftConfig = this.shiftConfigs.find(item => Number(item.shiftOrder) === shiftOrder);
          return shiftConfig && openShiftCodes.includes(String(shiftConfig.shiftCode || "").trim());
        });
      });
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;

        const result = await batchChangeMachine(params.machineCode, {
          selects: JSON.stringify(this.tableRows),
        });
        if (result && result.taskId) {
          this.$emit("success", result);
          this.hide();
        }

        this.loading = false;
      } catch (error) {
        this.$modal.alertError(resolveErrorMessage(
          error,
          this.$t("ui.data.column.tm.scheduleResult.operationFailed")
        ));
        this.loading = false;
      }
    },

    async show(data) {
      this.visible = true;
      this.tableRows = data;
      this.shiftConfigs = [];
      const firstRow = data[0] || {};
      try {
        const response = await listTmShiftConfig({
          factoryCode: firstRow.factoryCode,
          pageNum: 1,
          pageSize: 100,
        });
        this.shiftConfigs = response.rows || [];
      } catch (error) {
        this.$modal.alertError(resolveErrorMessage(
          error,
          this.$t("ui.data.column.tm.scheduleResult.operationFailed")
        ));
      }
    },
    hide() {
      this.form = {};
      this.tableRows = [];
      this.shiftConfigs = [];
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
    machineOpenShiftCodes(machine) {
      if (!machine || !machine.openShiftCode) return [];
      return [...new Set(String(machine.openShiftCode).split(",").map(item => item.trim()).filter(Boolean))];
    },
  },
};
</script>
