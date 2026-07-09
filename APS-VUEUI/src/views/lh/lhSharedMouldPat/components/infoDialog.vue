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
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";

import { editSharedMouldPat, checkUniqueSharedMouldPat } from "@/api/lh/sharedMouldPat";


export default {
  components: { infoForm, materialCodeSelect },
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
        materialCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        mouldNo: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            max: 64,
            message: this.$t("ui.data.alert.lhSharedMouldPat.mouldNoOverflow"),
            trigger: "blur",
          },
        ],
        patternBlock: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            max: 64,
            message: this.$t("ui.data.alert.lhSharedMouldPat.patternBlockOverflow"),
            trigger: "blur",
          },
        ],
        specifications: [
          {
            max: 300,
            message: this.$t("ui.data.alert.lhSharedMouldPat.specificationsOverflow"),
            trigger: "blur",
          },
        ],
        mainPattern: [
          {
            max: 50,
            message: this.$t("ui.data.alert.lhSharedMouldPat.mainPatternOverflow"),
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
          label: this.$t("ui.data.column.lhSharedMouldPat.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "materialCode",
          align: "center",
          label: this.$t("ui.data.column.lhSharedMouldPat.materialCode"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.materialCode}
                v-model={form.materialCode}
                onChange={this.handleMaterialCodeChange}
              />
            );
          },
        },
        {
          prop: "materialDesc",
          align: "center",
          label: this.$t("ui.data.column.lhSharedMouldPat.materialDesc"),
          disabled: true,
        },
        {
          prop: "specifications",
          align: "center",
          label: this.$t("ui.data.column.lhSharedMouldPat.specifications"),
        },
        {
          prop: "mainPattern",
          align: "center",
          label: this.$t("ui.data.column.lhSharedMouldPat.mainPattern"),
        },
        {
          prop: "mouldType",
          align: "center",
          label: this.$t("ui.data.column.lhSharedMouldPat.mouldType"),
          type: "select",
          dictData: this.parentDict.type.biz_mould_Type,
          filterable: true,
        },
        {
          prop: "mouldNo",
          align: "center",
          label: this.$t("ui.data.column.lhSharedMouldPat.mouldNo"),
        },
        {
          prop: "patternBlock",
          align: "center",
          label: this.$t("ui.data.column.lhSharedMouldPat.patternBlock"),
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 900,
        },
      ];
    },
  },
  methods: {
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
        const res = await editSharedMouldPat(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
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
        const checkData = {
          factoryCode: params.factoryCode,
          materialCode: params.materialCode,
          mainPattern: params.mainPattern,
          mouldNo: params.mouldNo,
          id: this.form.id || null,
        };
        const checkRes = await checkUniqueSharedMouldPat(checkData);
        if (checkRes.data && checkRes.data.exist) {
          this.$modal.msgError(this.$t("ui.data.alert.lhSharedMouldPat.notUnique"));
          return;
        }
        this.save(params);
      });
    },
  },
};
</script>
