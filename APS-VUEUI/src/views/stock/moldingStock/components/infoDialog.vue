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

import { numberEmpty } from "@/utils/index";

import infoForm from "@/views/components/infoForm.vue";

import { editStock, getProductEmbryoVersions } from "@/api/cx/stock";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        stockDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        embryoCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        bomDataVersion: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        stockNum: [
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
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.shiftLimit.modelName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.stock.stockDate"),
          prop: "stockDate",
          span: 24,
          required: true,
          type: "date",
          valueFormat: "yyyy-MM-dd",
          disabled: this.isEdit,
        },
        {
          label: this.$t("ui.data.column.stock.embryoCode"),
          prop: "embryoCode",
          span: 24,
          required: true,
          disabled: this.isEdit,
          maxlength: "50",
          // listener: {
          //   change: this.handleEmbryoCode,
          // },
        },
        // {
        //   label: this.$t("ui.data.column.productConstruction.embryoVersion"),
        //   prop: "bomDataVersion",
        //   span: 24,
        //   required: true,
        //   disabled: this.isEdit,
        // },
        {
          label: this.$t("ui.data.column.stock.stockNumAvailable"),
          prop: "stockNum",
          span: 24,
          required: true,
          disabled: this.isEdit,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        // {
        //   label: this.$t("ui.data.column.stock.unavailableStock"),
        //   prop: "unavailableStock",
        //   span: 24,
        //   required: true,
        //   disabled: this.isEdit,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        {
          label: this.$t("ui.data.column.stock.modifyNum"),
          prop: "modifyNum",
          span: 24,
          type: "number",
          min: -999999,
          max: 999999,
          precision: 0,
          required: true,
        },
        {
          label: this.$t("ui.data.column.stock.badNum"),
          prop: "badNum",
          span: 24,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
          required: true,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "300",
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editStock(params);
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
          modifyNum: numberEmpty(data.modifyNum),
          badNum: numberEmpty(data.badNum),
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
    },

    handleEmbryoCode(val) {
      console.log(val);
      if (!val) {
        return;
      }
      getProductEmbryoVersions({ embryoCode: val })
        .then((res) => {
          console.log(res);
        })
        .catch((e) => {
          console.error(e);
        });
    },
  },
};
</script>
