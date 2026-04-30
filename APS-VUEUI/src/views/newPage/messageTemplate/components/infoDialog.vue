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

// import { editCxSpecifyMachine } from "@/api/cx/cxSpecifyMachine";
import {
  editTemplate
} from "@/api/newPage/messageTemplate";
import machineSelect from "@/views/components/machineSelect.vue";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm, machineSelect },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {

        templateCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        templateName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        beginDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        endDate: [
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
          prop: "templateCode",
          label: this.$t("ui.data.messageTemplate.templateCode"),
          maxlength:50
        },
        {
          prop: "templateName",
          label: this.$t("ui.data.messageTemplate.templateName"),
          maxlength:100
        },
        {
          prop: "title",
          label: this.$t("ui.data.messageTemplate.title"),
        },
        {
          prop: "content",
          label: this.$t("common.messageTask.msgContent"),
        },

        {
          prop: "remark",
          label: this.$t("ui.remark"),
          maxlength: 200,
        },
      ];
    },
  },
  methods: {
    handleMachineTypeChange() {
      this.$set(this.form, "machineCode", "");
    },
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editTemplate(params);
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
        this.form = {};
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
    handleMaterialCodeChange() {},
  },
};
</script>
