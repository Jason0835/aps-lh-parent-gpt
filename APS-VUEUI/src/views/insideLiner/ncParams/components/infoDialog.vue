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
import { addParams, editParams } from "@/api/nc/params";

export default {
  components: { infoForm },
  inject: {
    parentDict: {
      default: () => ({}),
    },
  },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        paramCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        paramName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.nc.params.column.factoryCode"),
          prop: "factoryCode",
          span: 12,
          required: true,
          type: "select",
          dictData: this.parentDict.type?.biz_factory_name,
          disabled: true,
        },
        {
          label: this.$t("ui.nc.params.column.productTypeCode"),
          prop: "productTypeCode",
          span: 12,
          required: false,
          type: "select",
          dictData: this.parentDict.type?.biz_product_type,
          disabled: true,
        },
        {
          label: this.$t("ui.nc.params.column.paramCode"),
          prop: "paramCode",
          span: 12,
          maxlength: "100",
          required: true,
          disabled: true,
        },
        {
          label: this.$t("ui.nc.params.column.paramName"),
          prop: "paramName",
          span: 12,
          maxlength: "100",
          required: true,
          disabled: true,
        },
        {
          label: this.$t("ui.nc.params.column.dataType"),
          prop: "dataType",
          span: 12,
          required: false,
          disabled: true,
        },
        {
          label: this.$t("ui.nc.params.column.defauleValue"),
          prop: "defauleValue",
          span: 12,
          required: false,
          disabled: true,
        },
        {
          label: this.$t("ui.nc.params.column.paramValue"),
          prop: "paramValue",
          span: 24,
          required: false,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "300",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.nc.params.column.modalName")
      );
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        let res;
        if (this.isEdit) {
          res = await editParams(params);
        } else {
          res = await addParams(params);
        }
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

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
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
