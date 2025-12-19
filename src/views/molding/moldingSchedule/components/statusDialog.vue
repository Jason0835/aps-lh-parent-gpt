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
import { mapState } from "vuex";

import { modifyStatus } from "@/api/cx/cxScheduleResult";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
        taskType: "1",
      },
      rules: {

        taskType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        productionStatus: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],

      },
      embryoVersions: [],
      moldingMachines: [],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.cxAutoPlan");
    },
    columns() {
      return [
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.taskType"),
        //   prop: "taskType",
        //   span: 24,
        //   type: "select",
        //   dictData: this.parentDict.type.TASK_TYPE,
        // },
        {
          label: this.$t("ui.data.column.cxScheduleResult.productionStatus"),
          prop: "productionStatus",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.PRODUCTION_STATUS,
        },
        {
          label: this.$t("ui.data.column.stock.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
        },
      ];
    },
  },
  methods: {
    // api

    async save(params) {
      try {
        this.loading = true;
        const res = await modifyStatus(params);
        this.$modal.msgSuccess(res.msg);

        this.loading = false;
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
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
      }
    },
    hide() {
      // this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);

    },
    toUpperCase(e) {
      let value = e.target.value;
      if (value.length) {
        e.target.value = value.toUpperCase();
      }
    },

    onEmbryoCodeChange(val) {
      getProductEmbryoVersions({ embryoCode: val })
        .then((res) => {
          this.embryoVersions = res.map((row) => {
            return {
              label: row.embryoVersion,
              value: row.embryoVersion,
            };
          });
        })
        .catch((e) => {
          console.error(e);
          this.embryoVersions = [];
        });
      getCxMachines({ embryoCode: val })
        .then((res) => {
          this.moldingMachines = res.map((row) => {
            return {
              machineCode: row.machineCode,
              machineName: row.machineName,
            };
          });
        })
        .catch((e) => {
          console.error(e);
          this.moldingMachines = [];
        });
    },
  },
};
</script>
