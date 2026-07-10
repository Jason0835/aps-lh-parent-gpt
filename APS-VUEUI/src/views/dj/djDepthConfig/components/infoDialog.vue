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
import { saveDepthConfig } from "@/api/dj/depthConfig";

export default {
  components: { infoForm },
  dicts: ["machine_range"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        machineQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            validator: (rule, value, callback) => {
              if (value !== '' && value !== null && value !== undefined && Number(value) <= 0) {
                callback(new Error('数值必须大于0'));
              } else {
                callback();
              }
            },
            trigger: 'blur',
          },
        ],
        machineRange: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        depthClassQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            validator: (rule, value, callback) => {
              if (value !== '' && value !== null && value !== undefined && Number(value) <= 0) {
                callback(new Error('数值必须大于0'));
              } else {
                callback();
              }
            },
            trigger: 'blur',
          },
        ],
      },
    };
  },
  computed: {
    columns() {
      const machineRangeDict = this.dict?.type?.machine_range || [];
      return [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          span: 12,
          type: "select",
          dictData: this.dict?.type?.biz_factory_name,
          filterable: true,
          required: true,
          hidden: true,
        },
        {
          label: this.$t("ui.dj.depthConfig.column.machineRange"),
          prop: "machineRange",
          span: 12,
          type: "select",
          dictData: machineRangeDict,
          required: true,
        },
        {
          label: this.$t("ui.dj.depthConfig.column.machineQty"),
          prop: "machineQty",
          span: 12,
          type: "number",
          min: 1,
          required: true,
        },
        {
          label: this.$t("ui.dj.depthConfig.column.depthClassQty"),
          prop: "depthClassQty",
          span: 24,
          type: "number",
          min: 1,
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
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.dj.depthConfig.column.modalName")
      );
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        let res;
        res = await saveDepthConfig(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    show(data, defaultFactoryCode) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: defaultFactoryCode || '',
          machineRange: 'EQ',
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
