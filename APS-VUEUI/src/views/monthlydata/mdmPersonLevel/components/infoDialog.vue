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
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import { editMdmPersonLevel } from "@/api/monthplan/mdmPersonLevel";

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
        time: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        levelCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) + this.$t("人员档设定")
      );
    },
    columns() {
      return [
        {
          label: this.$t("年月"),
          prop: "time",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        {
          label: "人员档",
          prop: "levelCode",
          type: "select",
          dictData: this.parentDict.type.biz_personnel_type,
        },
        {
          label: "分厂",
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.constructionInfo.mouldMethod"),
          prop: "methodType",
          type: "select",
          dictData: this.parentDict.type.molding_method,
        },
        {
          prop: "machineNumber",
          label: this.$t("机台数量"),
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t("系数"),
          prop: "factor",
          span: 24,
          type: "number",
          min: 0.01,
          max: 9999.99,
          precision: 2,
        },
        // {
        //   label: this.$t("ui.common.column.remark"),
        //   prop: "remark",
        //   span: 24,
        //   type: "textarea",
        // },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMdmPersonLevel(params);
        this.$modal.msgSuccess('操作成功');
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
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
          machineNumber: this.numberEmpty(data.machineNumber),
          factor: this.numberEmpty(data.factor),
          time: `${data.year}-${data.month}`,
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
    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });
        if (params.time) {
          let arr = params.time.split("-");
          params.year = arr[0];
          params.month = arr[1];
          params.time = undefined;
        }

        this.save(params);
      });
    },
  },
};
</script>
