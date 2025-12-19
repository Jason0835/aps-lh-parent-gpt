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

import { editMdmMoldingMachineClsB } from "@/api/monthplan/mdmMoldingMachineClsB";
export default {
  components: { infoForm },
  props: {
    id: String,
  },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        proSize: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        productionQuotaQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        moldingSulfurizationRatio: [
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
        {
          prop: "proSize",
          label: this.$t("ui.data.column.docMoldingMachineClsB.productSize"),
          type: "number",
          min: 0,
          max: 9999999,
          attrs: {
            controls: false,
          },
          precision: 0,
        },
        {
          prop: "productionQuotaQty",
          label: this.$t(
            "ui.data.column.docMoldingMachineClsB.productionQuotaQty"
          ),
          type: "number",
          min: 0,
          max: 9999999,
          attrs: {
            controls: false,
          },
          precision: 0,
        },
        {
          prop: "moldingSulfurizationRatio",
          label: this.$t(
            "ui.data.column.docMoldingMachineClsB.moldingSulfurizationRatio"
          ),
          type: "number",
          min: 0,
          max: 99.99,
          attrs: {
            controls: false,
          },
          precision: 2,
        },
        {
          label: this.$t("common.remark"),
          prop: "remark",
          type: "textarea",
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMdmMoldingMachineClsB(params);
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
          proSize: this.numberEmpty(data.proSize),
          productionQuotaQty: this.numberEmpty(data.productionQuotaQty),
          moldingSulfurizationRatio: this.numberEmpty(
            data.moldingSulfurizationRatio
          ),
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
        params.moldingMachineClassId = this.id;
        this.save(params);
      });
    },
  },
};
</script>
