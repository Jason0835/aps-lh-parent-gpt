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

import { editMonthStock, getProductEmbryoVersions } from "@/api/cx/monthStock";

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
            message: this.$t("common.rule.input"),
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
      versions: [],
    };
  },
  computed: {
    title: function () {
      return (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) + this.$t("ui.data.column.cx.monthStock.modelName");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.cx.monthStock.stockMonth"),
          prop: "stockMonth",
          span: 24,
          required: true,
          disabled: this.isEdit,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.cx.monthStock.embryoCode"),
          prop: "embryoCode",
          span: 24,
          required: true,
          disabled: this.isEdit,
          maxlength: "50",
          listeners: {
            change: this.handleEmbryoCode,
          },
        },
        {
          label: this.$t("ui.data.column.productConstruction.embryoVersion"),
          prop: "bomDataVersion",
          span: 24,
          required: true,
          disabled: this.isEdit,
          render: (form) => {
            if (this.isEdit) {
              return (
                <el-input v-model={form.bomDataVersion} disabled></el-input>
              );
            }
            return (
              <el-select v-model={form.bomDataVersion}>
                {this.versions.map((item) => {
                  return (
                    <el-option
                      key={item.embryoVersion}
                      value={item.embryoVersion}
                      label={item.embryoVersion}
                    ></el-option>
                  );
                })}
              </el-select>
            );
          },
        },
        {
          label: this.$t("ui.data.column.cx.monthStock.stockNum"),
          prop: "stockNum",
          span: 24,
          required: true,
          disabled: this.isEdit,
          type: "number",
          min: 0,
          max: 999999999,
          precision: 0,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
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
        const res = await editMonthStock(params);
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
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },

    handleEmbryoCode(val) {
      if (!val) {
        this.versions = [];
        return;
      }
      getProductEmbryoVersions({ embryoCode: val })
        .then((res) => {
          this.versions = res;
          if (res.length) {
            this.form.bomDataVersion = res[0].embryoVersion;
          }
        })
        .catch((e) => {
          console.error(e);
          this.versions = [];
        });
    },
  },
};
</script>
