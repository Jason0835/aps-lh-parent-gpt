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
import { mapState } from "vuex";

import {
  editApsMoldAdjustPlan,
  getBeforeSpecDesc,
  getAfterSpecDesc,
} from "@/api/lh/lhApsMoldAdjustPlan";

import infoForm from "@/views/components/infoForm.vue";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      requireMap: {
        tireRoughStock: true,
        changeMoldTime: true,
        useMoldNumber: true,
      },
    };
  },
  computed: {
    ...mapState({
      curingMachines: (state) => state.curing.machines,
    }),
    title: function () {
      return this.$t("修改");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.lhMachineCode"),
          prop: "lhMachineCode",
          type: "select",
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
          type: "select",
          dictData: this.parentDict.type.MOLD_CHANGE_TYPE,
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
      ];
    },
    rules() {
      return {
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
            required: this.requireMap.tireRoughStock,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        changeMoldTime: [
          {
            required: this.requireMap.changeMoldTime,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        useMoldNumber: [
          {
            required: this.requireMap.useMoldNumber,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      };
    },
  },
  watch: {
    "form.changeType": function (changeType) {
      this.requireMap.useMoldNumber = false;

      //库存数必填
      if (
        changeType === "1" ||
        changeType === "2" ||
        changeType === "3" ||
        changeType === "4" ||
        changeType === "7" ||
        changeType === "9" ||
        changeType === "11"
      ) {
        this.requireMap.changeMoldTime = false;
        this.requireMap.tireRoughStock = true;
      }
      if (
        changeType === "5" ||
        changeType === "6" ||
        changeType === "8" ||
        changeType === "12"
      ) {
        this.requireMap.tireRoughStock = false;
        this.requireMap.changeMoldTime = true;
      }
      if (
        !(
          changeType === "7" ||
          changeType === "8" ||
          changeType === "9" ||
          changeType === "10"
        )
      ) {
        this.requireMap.useMoldNumber = true;
      }
      if (changeType === "10") {
        this.requireMap.changeMoldTime = false;
        this.requireMap.tireRoughStock = false;
      }
    },
  },

  methods: {
    // api
    async getBeforeSpecDesc() {
      try {
        const res = await getBeforeSpecDesc({
          beforeSapCode: this.form.beforeSapCode,
          beforeEmbryoCode: this.form.beforeEmbryoCode,
        });
        this.form.beforeSpecDesc = res.beforeSpecDesc;
      } catch (error) {
        console.error(error);
      }
    },
    async getAfterSpecDesc() {
      try {
        const res = await getAfterSpecDesc({
          beforeSapCode: this.form.after,
          beforeEmbryoCode: this.form.beforeEmbryoCode,
        });
        this.form.beforeSpecDesc = res.beforeSpecDesc;
      } catch (error) {
        console.error(error);
      }
    },

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
        this.form = {
          ...data,
        };
      }
    },
    hide() {
      this.form = {};
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
