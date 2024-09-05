<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1000px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :defaultValue="defaultValue"
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
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import {
  validateProduction,
  editProductStatus,
} from "@/api/cx/productStatus.js";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      defaultValue: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      rules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        cxMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        storageLocation: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.scheduleResult.productionDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
          required: true,
        },

        {
          label: this.$t("ui.data.column.productStatus.bomDataVersion"),
          prop: "bomDataVersion",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.productStatus.sapCode"),
          prop: "sapCode",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.productStatus.embryoCode"),
          prop: "embryoCode",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          type: "select",
          required: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.storageLocation"),
          prop: "storageLocation",
          type: "select",
          dictData: [], // STORAGE_LOCATION
          required: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1PlanQty"),
          prop: "class1PlanQty",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2PlanQty"),
          prop: "class2PlanQty",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3PlanQty"),
          prop: "class3PlanQty",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4PlanQty"),
          prop: "class4PlanQty",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class5PlanQty"),
          prop: "class5PlanQty",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.productStatus.modalName");
    },
  },
  methods: {
    // api
    async valid() {
      try {
        this.loading = true;
        const valid = await validateProduction(params);
        if (valid.msg) {
          this.$confirm(valid.msg).then(() => {
            this.save(params);
          });
        } else {
          this.save(params);
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    async save(params) {
      try {
        const data = await editProductStatus(params);
        this.$modal.msgSuccess(data.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(data, editType) {
      this.visible = true;
      this.editType = editType;
      if (data) {
        this.isEdit = true;
        this.defaultValue = {
          ...data,
        };
      }
    },
    hide() {
      this.defaultValue = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((valid) => {
        if (!valid) {
          return;
        }
        this.valid({
          ...this.form,
        });
      });
    },
  },
};
</script>
