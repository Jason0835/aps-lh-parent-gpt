<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :defaultValue="defaultValue"
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
import infoForm from "@/views/components/infoForm.vue";
import { editApsMoldAdjustPlan } from "@/api/lh/lhApsMoldAdjustPlan";
import CuringMachineSelect from "@/views/components/CuringMachineSelect.vue";
export default {
  components: { infoForm, CuringMachineSelect },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      defaultValue: {},
      rules: {
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        tireRoughStock: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.lhMachineCode"),
          prop: "lhMachineCode",
          render: (form) => {
            return (
              <CuringMachineSelect
                v-model={form.lhMachineCode}
                label={form.lhMachineName}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.planDate"),
          prop: "planDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.changeMoldTime"),
          prop: "changeMoldTime",
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd hh:mm",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.changeType"),
          prop: "changeType",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.beforeSapCode"),
          prop: "beforeSapCode",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.beforeEmbryoCode"),
          prop: "beforeEmbryoCode",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.beforeSpecDesc"),
          prop: "beforeSpecDesc",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.tireRoughStock"),
          prop: "tireRoughStock",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.useMoldNumber"),
          prop: "useMoldNumber",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.leftRightMold"),
          prop: "leftRightMold",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.afterSapCode"),
          prop: "afterSapCode",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.afterEmbryoCode"),
          prop: "afterEmbryoCode",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.afterSpecDesc"),
          prop: "afterSpecDesc",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("修改APS模具变动单");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const data = await editApsMoldAdjustPlan(params);
        this.$modal.msgSuccess(data.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.defaultValue = {
          ...data,
        };
      }
    },
    hide() {
      this.defaultValue = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
      // this.$refs.form.validate((valid) => {
      //   if (valid) {
      //     this.save({
      //       ...this.form,
      //     });
      //   }
      // });
    },
  },
};
</script>
