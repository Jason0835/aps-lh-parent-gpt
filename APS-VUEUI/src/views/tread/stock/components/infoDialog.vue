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
import Big from "big.js";

import { numberEmpty } from "@/utils/index";

import infoForm from "@/views/components/infoForm.vue";

import { editStock } from "@/api/tm/stock";

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
        materialCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        stockNum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        rollStockNum: [
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
    title: function () {
      return this.$t("ui.frame.page.stock.title");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.stock.stockDate"),
          prop: "stockDate",
          span: 24,
          required: true,
          disabled: this.editType === "2",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.quota.treadCode"),
          prop: "materialCode",
          span: 24,
          required: true,
          disabled: this.editType === "2",
          maxlength: "50",
        },
        // {
        //   label: this.$t("ui.data.column.stock.stockNum.roll"),
        //   prop: "rollStockNum",
        //   span: 24,
        //   required: true,
        //   disabled: this.editType === "1" || this.editType === "2",
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("ui.data.column.stock.modifyNum.roll"),
        //   prop: "rollModifyNum",
        //   span: 24,
        //   disabled: this.editType === "1",
        //   type: "number",
        //   min: -999999,
        //   max: 999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("ui.data.column.stock.badNum.roll"),
        //   prop: "rollBadNum",
        //   span: 24,
        //   disabled: this.editType === "1",

        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        {
          label: this.$t("ui.data.column.stock.stockNum.meter"),
          prop: "stockNum",
          span: 24,
          required: true,
          disabled: this.editType === "1" || this.editType === "2",
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.stock.modifyNum.meter"),
          prop: "modifyNum",
          span: 24,
          disabled: this.editType === "1",
          type: "number",
          min: -999999,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.stock.badNum.meter"),
          prop: "badNum",
          span: 24,
          disabled: this.editType === "1",
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          disabled: this.editType === "2",
          type: "textarea",
          maxlength: "300",
          disabled: this.limit,
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
    show(data, editType) {
      this.visible = true;
      this.editType = editType;

      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          rollStockNum: numberEmpty(data.rollStockNum),
          rollModifyNum: numberEmpty(data.rollModifyNum),
          rollBadNum: numberEmpty(data.rollBadNum),
          stockNum: numberEmpty(data.stockNum),
          modifyNum: numberEmpty(data.modifyNum),
          badNum: numberEmpty(data.badNum),
        };
      }
    },
    hide() {
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
      this.editType = null;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });

        let num =
          Number(params.stockNum) +
          Number(params.modifyNum) -
          Number(params.badNum);
        if (num < 0) {
          this.$modal.msgError(
            this.$t("ui.data.column.stock.stockNumValidate")
          );
          return;
        }

        this.save({
          ...params,
          editType: this.editType,
        });
      });
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
