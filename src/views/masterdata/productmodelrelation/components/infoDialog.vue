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
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";
import moldLedgerSelect from "@/views/components/moldLedgerSelect.vue";
import { editRelation } from "@/api/maindata/relation";

export default {
  components: { infoForm,materialCodeSelect,moldLedgerSelect },
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
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        samePatternPanel: [
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
        mainPattern: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        mouldCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        materialDesc: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        shareMouldCode: [
          {
            required: false,
            message: this.$t("common.rule.input"),
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
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.moldLedger.mouldCode"),
          prop: "mouldCode",
          render: (form) => {
            return (
              <moldLedgerSelect
                key={form.mouldCode}
                v-model={form.mouldCode}
                onChange={this.handleMoldLedgerChange}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.rubberMaterial.shareMouldCode"),
          prop: "shareMouldCode",
          render: (form) => {
            return (
              <moldLedgerSelect
                key={form.shareMouldCode}
                v-model={form.shareMouldCode}
              />
            );
          },
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
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
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          prop: "materialDesc",
          disabled:true
        },
        {
          label: this.$t("ui.data.column.moldLedger.mainPattern"),
          prop: "mainPattern",
          disabled:true
        },
        {
          prop: "isSamePatternPanel",
          label: this.$t("ui.data.column.monthplan.samePatternPanel"),
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          label: this.$t("common.remark"),
          prop: "remark",
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editRelation(params);
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
    handleMaterialCodeChange(val, row) {
      if (val) {
        this.$set(this.form, "materialDesc", row.materialDesc);
      } else {
        this.$set(this.form, "materialDesc", "");
      }
    },
    handleMoldLedgerChange(val, row) {
      if (val) {
        this.$set(this.form, "mainPattern", row.mainPattern);
      } else {
        this.$set(this.form, "mainPattern", "");
      }
    },
  },
};
</script>
