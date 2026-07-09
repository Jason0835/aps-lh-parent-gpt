<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1000px"
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
      label-width="120px"
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

import { batchChangeMachine, getCurrentShift } from "@/api/dj/djScheduleResult.js";
import { listMachine } from "@/api/dj/machine";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {},
      tableRows: [],
      shiftList: [],
      machines: [],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.djScheduleResult.modalName");
    },
    columns() {
      const cols = [];

      // 基础信息标题
      cols.push({
        label: this.$t("ui.data.column.scheduleResult.baseInfo") + (this.tableRows.length > 1 ? "（" + this.tableRows.length + "）" : ""),
        type: "title",
      });

      // 单条记录时展示详细信息
      if (this.tableRows.length === 1) {
        const row = this.tableRows[0];

        // 从选中记录中提取数据作为默认值
        this.form.scheduleDate = row.scheduleDate;
        this.form.machineCode = row.machineName || row.machineCode;
        this.form.paddingName = row.paddingName;

        cols.push({
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 12,
          disabled: true,
        });
        cols.push({
          label: this.$t("ui.data.column.dj.scheduleResult.paddingName"),
          prop: "paddingName",
          span: 12,
          disabled: true,
        });
        cols.push({
          label: this.$t("ui.data.column.dj.scheduleResult.sourceMachine"),
          prop: "machineCode",
          span: 12,
          disabled: true,
        });
        cols.push({
          label: this.$t("ui.data.column.dj.scheduleResult.targetMachine"),
          prop: "newMachineCode",
          span: 12,
          type: "select",
          required: true,
          dictData: this.machines,
          props: {
            label: "machineName",
            value: "machineCode",
          },
        });

        // 动态生成三个班信息
        const planQtyLabel = this.$t("ui.data.column.dj.scheduleResult.planQty");
        const seqLabel = this.$t("ui.data.column.dj.scheduleResult.sequence");
        const analysisLabel = this.$t("ui.data.column.dj.scheduleResult.analysis");
        const shiftLabels = this.shiftList.map((s) => s.label || "");

        for (let i = 0; i < 3; i++) {
          const label = shiftLabels[i] || "class" + (i + 1);
          const classIdx = i + 1;

          cols.push({
            type: "title",
            label: label,
          });

          // 计划量（只读）
          this.form["class" + classIdx + "PlanQty"] = row["class" + classIdx + "PlanQty"];
          cols.push({
            label: planQtyLabel,
            prop: "class" + classIdx + "PlanQty",
            span: 12,
            disabled: true,
          });

          // 生产顺序（只读）
          this.form["class" + classIdx + "Sequence"] = row["class" + classIdx + "Sequence"];
          cols.push({
            label: seqLabel,
            prop: "class" + classIdx + "Sequence",
            span: 12,
            disabled: true,
          });

          // 原因分析（只读）
          this.form["class" + classIdx + "Analysis"] = row["class" + classIdx + "Analysis"];
          cols.push({
            label: analysisLabel,
            prop: "class" + classIdx + "Analysis",
            span: 24,
            disabled: true,
          });
        }
      }

      return cols;
    },
  },
  methods: {
    // api
    loadCurrentShift() {
      getCurrentShift().then((res) => {
        if (res) {
          this.shiftList = res.shifts || [];
        }
      }).catch(() => {});
    },
    loadMachines() {
      listMachine().then((res) => {
        const seen = new Set();
        this.machines = (res.rows || []).filter((m) => {
          if (seen.has(m.id)) {
            return false;
          }
          seen.add(m.id);
          return true;
        });
      });
    },

    async save(params) {
      try {
        this.loading = true;

        const result = await batchChangeMachine(params.newMachineCode, {
          selects: JSON.stringify(this.tableRows)
        });
        if (result.code == 200) {
          this.$modal.msgSuccess(this.$t("common.msg.ajax.operation.success"));
          this.$emit("success");
          this.hide();
        }

        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      this.tableRows = data;
      this.form = {};
      this.isEdit = true;
      this.loadCurrentShift();
      this.loadMachines();
    },
    hide() {
      this.form = {};
      this.tableRows = [];
      this.shiftList = [];
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      if (this.tableRows.length === 1) {
        const row = this.tableRows[0];
        if (String(row.machineCode) === String(this.form.newMachineCode)) {
          this.hide();
          return;
        }
      }

      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
