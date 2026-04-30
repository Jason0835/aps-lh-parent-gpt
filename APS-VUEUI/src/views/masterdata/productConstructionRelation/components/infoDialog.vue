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

import {
 editMdmProductConstruction
} from "@/api/maindata/mdmProductConstruction";

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
        productCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
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
    moldingMethodList: function() {
      return this.parentDict.type.molding_method.filter((item) => {
        console.log(item);
        return item.value === '1' || item.value === '2'
      })
    },
    columns() {
      return [
      {
          prop: "productCode",
          label: this.$t("ui.data.column.producconstructionrela.productCode"),
          disabled: true,
        },
        {
          prop: "factoryCode",
          label: this.$t(
            "ui.data.column.producconstructionrela.factoryCode"
          ),
          disabled: true,
        },
        {
          prop: "specCode",
          label: this.$t("ui.data.column.productmodelrelation.specCode"),
        },
        {
          prop: "constructionCode",
          label: this.$t("ui.data.column.producconstructionrela.constructionCode"),
        },
        {
          prop: "embryoCode",
          label: this.$t("ui.data.column.producconstructionrela.embryoCode"),
        },
        {
          prop: "productionVersion",
          label: this.$t("ui.data.column.producconstructionrela.productionVersion"),
        },
        {
          prop: "bomVersion",
          label: this.$t("ui.data.column.producconstructionrela.bomVersion"),
        },
        {
          prop: "mouldClampingPressure",
          label: this.$t("ui.data.column.producconstructionrela.mouldClampingPressure"),
        },
        {
          prop: "mouldMethod",
          label: this.$t("ui.data.column.producconstructionrela.mouldMethod"),
          type: "select",
          dictData: this.moldingMethodList,
        },
        {
          prop: "curingTime",
          label: this.$t("ui.data.column.producconstructionrela.curingTime"),
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          prop: "curingTime2",
          label: this.$t("ui.data.column.producconstructionrela.curingTime2"),
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,

        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMdmProductConstruction(params);
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

        this.save(params);
      });
    },
  },
};
</script>
