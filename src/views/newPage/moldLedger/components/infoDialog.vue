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
  editMdmModelInfo
} from "@/api/maindata/mdmModelInfo";
// import { editCxSpecifyMachine } from "@/api/cx/cxSpecifyMachine";

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
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        mouldCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
            maxlength:40,
          },
        ],
        embryoCode: [
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
        lineType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        mouldStatus: [
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
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },

        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.moldLedger.mouldCode"),
          maxlength:40
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.reportClassAccuracy.materialCode"),
          maxlength:64
        },
        {
          prop: "logisticsStatus",
          label: this.$t("ui.data.column.moldLedger.logisticsStatus"),
          type: "select",
          dictData: this.parentDict.type.logistics_status,
        },
        {
          prop: "mainPattern",
          label: this.$t("ui.data.column.moldLedger.mainPattern"),
          maxlength:64
        },
        {
          prop: "mouldType",
          label: this.$t("ui.data.column.modelinfo.mouldType"),
          type: "select",
          dictData: this.parentDict.type.biz_mould_Type
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.moldLedger.pattern"),
          maxlength:64
        },
        {
          prop: "shellStandard",
          label: this.$t("ui.data.column.moldLedger.shellStandard"),
          maxlength:64
        },
        {
          prop: "mouldStatus",
          label: this.$t("ui.data.column.docVulcanizationMachStatus.status"),
          type: "select",
          dictData: this.parentDict.type.biz_available_status
        },
        {
          prop: "remark",
          label: this.$t("ui.remark"),
          maxlength:500
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMdmModelInfo(params);
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
        console.log(this.form);
        this.form.mouldStatus = this.form.mouldStatus+''
      } else {
        this.form = {
          factoryCode: "",
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
