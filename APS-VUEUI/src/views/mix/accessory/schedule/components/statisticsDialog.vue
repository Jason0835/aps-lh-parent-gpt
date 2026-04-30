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
    <page-table
      :columns="columns"
      :searchColumns="searchColumns"
      :toolbar="false"
    >
    </page-table>
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


import { statistics } from "@/api/schedule/glueScheduleResult";

import PageTable from '@/components/Table/PageTable.vue';

export default {
  components: { PageTable },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      rules: {
        // specName: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
      },
      columns: [
        {
          label: this.$t("schedule.materialScheduleResult.machineName"),
          prop: "machineName",
          span: 24,

        },
        {
          label: this.$t("schedule.materialScheduleResult.classShift"),
          prop: "classShift",
          span: 24,

        },
              {
          label: this.$t("schedule.materialScheduleResult.demandQty.statistics"),
          prop: "demandQtyStatistics",
          span: 24,
          type: "select",
        },
        {
          label: this.$t("schedule.materialScheduleResult.totalPlanQty.statistics"),
          prop: "totalPlanQtyStatistics",
          span: 24,
          maxlength: "20",
          listeners: {
            blur: this.toUpperCase,
          },
        },

        {
          label: this.$t("schedule.materialScheduleResult.midPlanQty.statistics"),
          prop: "midPlanQtyStatistics",
          span: 24,
        },
        {
          label: this.$t("schedule.materialScheduleResult.nightPlanQty.statistics"),
          prop: "nightPlanQtyStatistics",
          span: 24,
        },
        {
          label: this.$t("schedule.materialScheduleResult.dayPlanQty.statistics"),
          prop: "dayPlanQtyStatistics",
          span: 24,
          maxlength: "100",
        },

      ],
      searchColumns: [
        {
          label: this.$t("schedule.materialScheduleResult.machineName.short"),
          prop: "machineCode",
          type: "select"
        },

      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("schedule.materialScheduleResult.modelName");
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let valid = await validateAdd(params);
        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.data.column.scheduleResult.isContinueAdd")
          ).then(async () => {
            let result = await save(params);
            this.loading = false;
            if (result.code == 200) {
              this.$emit("success");
              this.hide();
            }
          });
        } else {
          let result = await save(params);
          this.loading = false;
          if (result.code == 200) {
            this.$emit("success");
            this.hide();
          }
          this.loading = false;
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    async getList() {
      try {
        const res = await statistics();
        this.tableData = res.rows;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }

    },

    //utils
    show(data, editType) {
      this.visible = true;
      this.getList();
    },
    hide() {
      this.form = {};
      // this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    toUpperCase(e) {
      let value = e.target.value;
      if (value.length) {
        this.form.treadCode = value.toUpperCase();
      }
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(() => {
        this.save()
      });
      // this.$refs.form.validate((valid) => {
      //   if (valid) {
      //     this.save({
      //       ...this.form,
      //     });
      //   }
      // });
    },
  },
};
</script>
