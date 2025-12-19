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
import { saveMdmMoldingMachine } from "@/api/monthplan/mdmMoldingMachine";

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
        cxMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        maxDayCapacity: [
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
        jobType: [
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
          prop: "cxMachineCode",
          label: this.$t("setting.machine.machineCode"),
          maxlength:64,
        },
        {
          prop: "cxMachineBrandCode",
          label: this.$t("ui.data.column.docMoldingMachine.moldingMachineClassName"),
          type: "select",
          dictData: this.parentDict.type.biz_machine_brand,
        },
        {
          prop: "cxMachineTypeCode",
          label: this.$t("ui.data.column.curingPlan.cxMachineTypeCode"),
          type: "select",
          dictData: this.parentDict.type.biz_class_type,
        },
        {
          prop: "rollOverType",
          label: this.$t("ui.data.column.capsuleChuck.rollOverType"),
          maxlength:64,
        },
        {
          prop: "isZeroRack",
          label: this.$t("ui.data.column.capsuleChuck.isZeroRack"),
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          prop: "lhMachineMaxQty",
          label: this.$t("ui.data.column.capsuleChuck.lhMachineMaxQty"),
          type: "number",
        },
        {
          prop: "maxDayCapacity",
          label: this.$t("ui.data.column.capsuleChuck.maxDayCapacity"),
          type: "number",
        },

        {
          prop: "remark",
          label: this.$t("common.remark"),
          maxlength:256,
        },

      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await saveMdmMoldingMachine(params);
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
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
