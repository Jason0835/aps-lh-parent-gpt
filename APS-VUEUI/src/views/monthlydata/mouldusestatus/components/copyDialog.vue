<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="400px"
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

import { mergeMouldUseStatus } from "@/api/lean/mouldusestatus";
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
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        fromYearMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        toYearMonth: [
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
      return this.$t("ui.frame.btn.copy");
    },
    columns() {
      return [
        // {
        //   prop: "mouldCode",
        //   label: this.$t("ui.data.column.mouldusestatus.mouldCode"),
        //   disabled: true,
        // },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.mouldusestatus.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.mouldusestatus.fromYearMonth"),
          prop: "fromYearMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        {
          label: this.$t("ui.data.column.mouldusestatus.toYearMonth"),
          prop: "toYearMonth",
          disabled: false,
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await mergeMouldUseStatus(params);
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
          // mouldCode: data.mouldCode,
          factoryCode: data.factoryCode,
          fromYearMonth: `${data.year}-${data.month}`,
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
        if (params.fromYearMonth) {
          const arr = params.fromYearMonth.split("-");
          params.fromyear = arr[0];
          params.frommonth = arr[1];
          params.fromYearMonth = undefined;
        }
        if (params.toYearMonth) {
          const arr = params.toYearMonth.split("-");
          params.toyear = arr[0];
          params.tomonth = arr[1];
          params.toYearMonth = undefined;
        }

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
