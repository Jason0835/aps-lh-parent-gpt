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

import { editMdmModelInfo } from "@/api/maindata/mdmModelInfo";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        mouldStatus: "1"
      },
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        mouldNo: [
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
        specifications: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        pattern: [
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
          label: this.$t("ui.data.column.modelinfo.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "mouldNo",
          label: this.$t("ui.data.column.modelinfo.mouldNo"),
        },
        {
          label: this.$t("ui.data.column.modelinfo.mouldCode"),
          prop: "mouldCode",
          span: 24,
          required: true,
          disabled: false,
        },
        {
          label: this.$t("ui.data.column.modelinfo.specifications"),
          prop: "specifications",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.data.column.modelinfo.pattern"),
          prop: "pattern",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.data.column.modelinfo.mouldType"),
          prop: "mouldType",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.biz_mould_Type,
        },
        {
          label: this.$t("ui.data.column.modelinfo.mouldRemark"),
          prop: "mouldRemark",
          span: 24,
          required: false,
        },
        {
          label: this.$t("ui.data.column.modelinfo.mouldSleeve"),
          prop: "mouldSleeve",
          span: 24,
          required: false,
        },
        {
          label: this.$t("ui.data.column.modelinfo.mouldAirType"),
          prop: "mouldAirType",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.biz_mould_air_type,
        },
        {
          label: this.$t("ui.data.column.modelinfo.mouldStatus"),
          prop: "mouldStatus",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.biz_available_status,
          clearable: false
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMdmModelInfo(params);
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
          mouldStatus: data.mouldStatus + ""
        };
      }
    },
    hide() {
      // this.form = {};
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
