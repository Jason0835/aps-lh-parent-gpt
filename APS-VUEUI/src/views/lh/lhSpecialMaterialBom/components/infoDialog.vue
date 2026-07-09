<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
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
      label-width="120px"
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
import structureSelect from "@/views/components/structureSelect.vue";
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";

import { editSpecialMaterialBom } from "@/api/lh/specialMaterialBom";

export default {
  components: { infoForm, structureSelect, materialCodeSelect },
  inject: ["parentDict"],
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
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        category: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
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
          label: this.$t("ui.data.column.lhSpecialMaterialBom.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "structureName",
          align: "center",
          label: this.$t("ui.data.column.lhSpecialMaterialBom.structureName"),
          render: (form) => {
            return (
              <structureSelect
                key={form.structureName}
                v-model={form.structureName}
                factoryCode={form.factoryCode || "116"}
                machineType="CX"
                clearable
                onChange={this.handleStructureChange}
              />
            );
          },
        },
        {
          prop: "materialCode",
          align: "center",
          label: this.$t("ui.data.column.lhSpecialMaterialBom.materialCode"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.materialCode}
                v-model={form.materialCode}
                structureName={form.structureName}
                onChange={this.handleMaterialCodeChange}
              />
            );
          },
        },
        {
          prop: "materialDesc",
          align: "center",
          label: this.$t("ui.data.column.lhSpecialMaterialBom.materialDesc"),
          disabled: true,
        },
        {
          prop: "category",
          align: "center",
          label: this.$t("ui.data.column.lhSpecialMaterialBom.category"),
          type: "select",
          dictData: this.parentDict.type.lh_special_material_category,
          filterable: true,
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 300,
        },
      ];
    },
  },
  methods: {
    handleStructureChange(val, row) {
      if (val && row) {
        this.$set(this.form, "structureName", row.structureName);
      } else {
        this.$set(this.form, "structureName", "");
      }
    },
    handleMaterialCodeChange(val, row) {
      if (val && row) {
        this.$set(this.form, "materialCode", row.materialCode);
        this.$set(this.form, "materialDesc", row.materialDesc || "");
      } else {
        this.$set(this.form, "materialCode", "");
        this.$set(this.form, "materialDesc", "");
      }
    },
    async save(params) {
      try {
        this.loading = true;
        const res = await editSpecialMaterialBom(params);
        if (res.code === 200) {
          this.$modal.msgSuccess(res.msg);
          this.$emit("success");
          this.hide();
        }
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form && this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        if (!params.structureName && !params.materialCode) {
          this.$modal.msgError(this.$t("ui.data.alert.lhSpecialMaterialBom.needOne"));
          return;
        }
        this.save(params);
      });
    },
  },
};
</script>
