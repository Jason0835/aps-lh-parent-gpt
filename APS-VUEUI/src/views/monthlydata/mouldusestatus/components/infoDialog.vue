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

import { editMouldusestatus } from "@/api/lean/mouldusestatus";
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
            message: this.$t("common.rule.input"),
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
        mouldStatus: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        mouldCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        // {
        //   prop: "year",
        //   label: this.$t("ui.data.column.mouldusestatus.year"),
        // },
        // {
        //   prop: "month",
        //   label: this.$t("ui.data.column.mouldusestatus.month"),
        // },
        {
          label: this.$t("ui.data.colume.yearMonth"),
          prop: "time",
          disabled: this.isEdit,
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },

        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.mouldusestatus.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "mouldStatus",
          label: this.$t("ui.data.column.mouldusestatus.mouldStatus"),
          type: "select",
          dictData: this.parentDict.type.biz_available_status,
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.mouldusestatus.mouldCode"),
          disabled: this.isEdit,
          maxlength: "40",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "1000",
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMouldusestatus(params);
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
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          time: `${data.year}-${data.month}`,
          mouldStatus: data.mouldStatus + "",
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
        const arr = params.time.split("-");

        params.year = arr[0];
        params.month = arr[1];
        params.time = undefined;

        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });
        this.save(params);
      });
    },
  },
};
</script>
