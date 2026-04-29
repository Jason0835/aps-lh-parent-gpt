<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="150px"
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
import { saveToolingCartCapacity } from "@/api/tq/toolingCartCapacity";
import { listAllTooling } from "@/api/tq/tooling";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      toolingLoading: false,
      visible: false,
      isEdit: false,
      form: {},
      toolingList: [],
      rules: {
        cartCode: [
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
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.tq.toolingCartCapacity.column.modalName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.tq.toolingCartCapacity.column.cartCode"),
          prop: "cartCode",
          span: 24,
          required: true,
          type: "select",
          dictData: this.toolingList,
          filterable: true,
          loading: this.toolingLoading,
          props: {
            label: "toolingCode",
            value: "toolingCode",
          },
        },
        {
          label: this.$t("ui.tq.toolingCartCapacity.column.materialCode"),
          prop: "materialCode",
          span: 24,
          required: true,
          maxlength: "60",
        },
        {
          label: this.$t("ui.tq.toolingCartCapacity.column.cartCapacity"),
          prop: "cartCapacity",
          span: 24,
          type: "number",
          defaultValue: 50,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "500",
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        const res = await saveToolingCartCapacity(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    async loadToolingList() {
      this.toolingLoading = true;
      try {
        const res = await listAllTooling();
        const list = Array.isArray(res) ? res : (res.data || res.rows || []);
        this.toolingList = list;
      } catch (error) {
        console.log(error);
      } finally {
        this.toolingLoading = false;
      }
    },
    async show(data) {
      this.visible = true;
      await this.loadToolingList();
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
        if (data.cartCode) {
          const exists = this.toolingList.some(item => item.toolingCode === data.cartCode);
          if (!exists) {
            this.toolingList.unshift({
              toolingCode: data.cartCode,
              toolingName: data.cartCode,
            });
          }
        }
      } else {
        this.isEdit = false;
        this.form = {
          cartCapacity: 50,
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
