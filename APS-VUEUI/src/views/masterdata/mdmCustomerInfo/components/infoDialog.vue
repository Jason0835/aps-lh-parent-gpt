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
import infoForm from "@/views/components/infoForm.vue";

import { editMdmCustomerInfo } from "@/api/monthplan/mdmCustomerInfo";
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
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        customCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        customName: [
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
          prop: "factoryCode",
          label: this.$t("ui.data.column.mdmCustomerInfo.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "customCode",
          label: this.$t("ui.data.column.mdmCustomerInfo.customCode"),
        },
        {
          prop: "customName",
          label: this.$t("ui.data.column.mdmCustomerInfo.customName"),
        },
        {
          prop: "isImportantCustom",
          label: this.$t("ui.data.column.mdmCustomerInfo.isImportantCustom"),
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
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

        const res = await editMdmCustomerInfo(params);
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
          isImportantCustom: data.isImportantCustom + ""
        };
      } else {
        this.form = {
          isImportantCustom: "0"
        }
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
