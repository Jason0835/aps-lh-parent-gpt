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

import { getSpecDesc, editSapSpecMoldUse } from "@/api/cx/sapSpecMoldUse";

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
        sapCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.sapSpecMoldUse.sapCode"),
          prop: "sapCode",
          span: 24,
          required: true,
          maxlength: "20",
          listeners: {
            blur: this.handleSapCodeBlur,
          },
        },
        {
          label: this.$t("ui.data.column.sapSpecMoldUse.specDesc"),
          prop: "specDesc",
          span: 24,
          // disabled: true,
        },
        {
          label: this.$t("ui.data.column.sapSpecMoldUse.embryoCode"),
          prop: "embryoCode",
          span: 24,
          maxlength: "20",
        },
        {
          label: this.$t("ui.data.column.sapSpecMoldUse.moldNum"),
          prop: "moldNum",
          span: 24,
          type: "number",
          mix: 0,
          max: 99999999,
          precision: 0,
        },
        // {
        //   label: this.$t("ui.common.column.remark"),
        //   prop: "remark",
        //   span: 24,
        //   type: "textarea",
        // },
      ],
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.sapSpecMoldUse.modelName")
      );
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editSapSpecMoldUse(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    async getSpecDesc() {
      try {
        const res = await getSpecDesc({
          sapCode: this.form.sapCode,
        });
        if (res && res.specDesc) {
          this.$set(this.form, "specDesc", res.specDesc);
        } else {
          this.$set(this.form, "specDesc", "");

          this.$alert(res.msg, {
            type: "error",
          });
        }
      } catch (error) {
        console.error(error);
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
        //
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
    handleSapCodeBlur() {
      if (this.form.sapCode) {
        this.getSpecDesc();
      }
    },
  },
};
</script>
