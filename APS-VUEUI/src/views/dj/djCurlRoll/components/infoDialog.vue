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
import { mapState } from "vuex";

import infoForm from "@/views/components/infoForm.vue";

import { getConfigKey } from "@/api/system/config";
import { saveCurlRoll } from "@/api/dj/curlRoll";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      factoryCode: "",
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
        paddingCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        curlLength: [
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
    ...mapState({
      machines: (state) => state.tread.machines,
    }),
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.dj.curlRoll.column.paddingCode"),
          prop: "paddingCode",
          span: 24,
        },
        {
          label: this.$t("ui.dj.curlRoll.column.curlLength"),
          prop: "curlLength",
          span: 24,
          required: true,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
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

        const res = await saveCurlRoll(params);
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
      // 获取当前工厂编码（保存时需要带工厂参数）
      if (!this.factoryCode) {
        getConfigKey("sys.factory.code").then((response) => {
          this.factoryCode = response.msg;
          // 新增时默认选中默认工厂（工厂字段不可编辑）
          if (!data) {
            this.form = { ...this.form, factoryCode: response.msg };
          }
        });
      } else if (!data) {
        // 新增时默认选中默认工厂（工厂字段不可编辑）
        this.form = { ...this.form, factoryCode: this.factoryCode };
      }
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
      this.$refs.form.triggerConfirm((params) => {
        this.save({
          ...params,
          factoryCode: params.factoryCode || this.factoryCode,
        });
      });
    },
  },
};
</script>
