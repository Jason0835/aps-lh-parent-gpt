<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
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
      label-width="160px"
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
import { mapState } from "vuex";
import { savePrecisionPlan } from "@/api/lh/precisionPlan";
import infoForm from "@/views/components/infoForm.vue";

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
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        actualDate: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
      },
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.lhPrecisionPlan.factoryCode"),
          type: "select",
          dicData: this.parentDict.biz_factory_name,
          disabled: true,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.lhPrecisionPlan.machineCode"),
          disabled: true,
        },
        {
          prop: "precisionType",
          label: this.$t("ui.data.column.lhPrecisionPlan.precisionType"),
          type: "select",
          dicData: this.parentDict.MACHINE_ACCURACY_TYPE,
          disabled: true,
        },
        {
          prop: "planDate",
          label: this.$t("ui.data.column.lhPrecisionPlan.planDate"),
          type: "date",
          disabled: false,
        },
        {
          prop: "actualDate",
          label: this.$t("ui.data.column.lhPrecisionPlan.actualDate"),
          type: "date",
          disabled: false,
        },
        {
          prop: "dueDate",
          label: this.$t("ui.data.column.lhPrecisionPlan.dueDate"),
          disabled: true,
        },
        {
          prop: "daysToDue",
          label: this.$t("ui.data.column.lhPrecisionPlan.daysToDue"),
          disabled: true,
        },
        {
          prop: "dataSource",
          label: this.$t("ui.data.column.lhPrecisionPlan.dataSource"),
          type: "select",
          dicData: this.parentDict.lh_precision_data_source,
          disabled: true,
        },
        {
          prop: "remark",
          label: this.$t("ui.data.column.remark"),
          disabled: false,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        const res = await savePrecisionPlan(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      } else {
        this.form = {
          factoryCode: "",
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
