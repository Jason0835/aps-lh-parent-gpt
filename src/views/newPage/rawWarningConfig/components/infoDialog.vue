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

import {
  saveRawWarningConfig,
} from "@/api/maindata/rawWarningConfig";

import infoForm from "@/views/components/infoForm.vue";

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
        enabled: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        materialCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        materialName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        deviationLower: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        deviationUpper: [
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
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
      {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "enabled",
          label: this.$t("ui.data.column.rawMaterial.enabled"),
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
        },

        {
          prop: "materialCode",
          label: this.$t("ui.data.column.rawMaterial.materialCode"),
          maxlength: 50,
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.rawMaterial.materialName"),
          maxlength: 50,
        },
        {
          prop: "deviationUpper",
          label: this.$t("ui.data.column.rawMaterial.deviationUpper"),
          type: "number",
          min: 0,
          max:999999,
        },
        {
          prop: "deviationLower",
          label: this.$t("ui.data.column.rawMaterial.deviationLower"),
          type: "number",
          min: 0,
          max:999999,
        },
        {
          prop: "warningLevel",
          label: this.$t("ui.data.column.rawMaterial.warningLevel"),
          type: "select",
          dictData: this.parentDict.type.warn_level,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          maxlength: 200,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await saveRawWarningConfig(params);
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
          enabled: data.enabled+'',
        };
      } else {
        this.form = {
          factoryCode: "116",
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
  },
};
</script>
