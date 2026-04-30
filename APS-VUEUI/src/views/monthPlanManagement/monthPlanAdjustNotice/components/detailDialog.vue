<template>
  <el-dialog
    :title="title"
    :visible="visible"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
    width="80%"
  >
    <div class="adjust" v-loading="loading">
      <div class="content">
        <page-table
          tableRef="MonthPlanAdjustNoticeDetailTable"
          :calcHeight="true"
          :columns="tableColumns"
          :data="tableData"
          :toolbar="false"
        >
        </page-table>
      </div>
    </div>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import { getAdjustDetail } from "@/api/factory/monthPlanAdjustNotice";
import { matchMouldConfiguration } from "@/api/maindata/relation.js";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      tableData: [],
      info: null,
    };
  },
  computed: {
    title: function () {
      return this.$t("common.button.detail");
    },
    tableColumns() {
      const columns = [
        {
          label: "排产制造单号",
          prop: "productionNo",
          width: 180,
        },
        {
          label: "开始调整日期",
          prop: "startDate",
          width: 160,
        },
        {
          label: "分厂",
          prop: "factoryCode",
          formatter: (row) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          prop: "year",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.month"),
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.productDesc"),
          width: 300,
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.proSize"),
          minWidth: 80,
        },

        // {
        //   prop: "specCode",
        //   label: this.$t("ui.data.column.monthPlanAdjustNotice.specCode"),
        // },
        // {
        //   prop: "mouldNum",
        //   label: this.$t("ui.data.column.monthPlanAdjustNotice.mouldNum"),
        // },
        // {
        //   prop: "mouldNo",
        //   label: this.$t("ui.data.column.monthPlanAdjustNotice.mouldNo"),
        // },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.locationType"),
          width: 100,
          formatter: (row) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_stor_type,
              row.locationType
            );
          },
        },
        {
          prop: "channel",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.channel"),
          width: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_channel_type,
              value
            );
          },
        },
        // {
        //   prop: "brand",
        //   label: this.$t("ui.data.column.monthPlanAdjustNotice.brand"),
        // },
        {
          prop: "adjustQty",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.adjustQty"),
        },
      ];

      return columns;
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await confirmAdjust(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    async getDetail() {
      try {
        this.loading = true;
        const res = await getAdjustDetail(this.info);
        this.tableData = res.rows || [];
        // console.log(res.rows);
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    async getMouldNo() {
      try {
        const res = await matchMouldConfiguration({
          factoryCode: this.form.factoryCode,
          productCode: this.form.productCode,
          month: this.form.month,
          year: this.form.year,
        });
        console.log(res);
        // this.form.brand = res.brand;
        if (res.mouldConfigurationList && res.mouldConfigurationList[0]) {
          let mould = res.mouldConfigurationList[0];
          this.form.mouldNo = mould.mouldNo;
          this.specCodeList = (mould.specCodeList || []).map((val) => {
            return {
              label: val,
              value: val,
            };
          });
          if (mould.specCodeList && mould.specCodeList[0]) {
            let specCode = mould.specCodeList[0];
            this.$set(this.form, "specCode", specCode);
            // this.form.specCode = specCode;
          }
        }
      } catch (error) {
        console.log(error);
      }
    },

    //utils
    show(data) {
      this.visible = true;
      console.log(data);
      if (data) {
        this.isEdit = true;
        this.info = data;
        this.getDetail(this.info);
      }
    },
    hide() {
      this.info = null;
      this.isEdit = false;
      this.visible = false;
    },
    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    handleReduce() {
      const day = moment(this.info.startDate).format("DD");
      console.log(day);
      let adjustNumber = this.form.adjustNumber;
      // let sum = ajs;
      for (let i = 20; i <= 31; i++) {
        let key = `day${i}`;

        let num = this.currentRow[key];
        if (num && !isNaN(num) && num > 0) {
          if (num > adjustNumber) {
            this.currentRow[key] = num - adjustNumber;
            adjustNumber = 0;
            break;
          } else {
            adjustNumber -= num;
            this.currentRow[key] = 0;
          }
        }
      }

      if (this.confirmSubtractMap[this.currentRow.productionNo]) {
        let adjustNumber =
          this.confirmSubtractMap[this.currentRow.productionNo].adjustNumber;
        adjustNumber -= this.form.adjustNumber;
        this.confirmSubtractMap[this.currentRow.productionNo].adjustNumber =
          adjustNumber;
      } else {
        this.confirmSubtractMap[this.currentRow.productionNo] = {
          productionNo: this.currentRow.productionNo,
          startDate: this.form.startDate,
          adjustNumber: 0 - this.form.adjustNumber,
        };
      }

      // this.selection.map(() => ())
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });
        let array = params.yearMonth.split("-");
        params.year = array[0];
        params.month = array[1];

        this.save(params);
      });
    },
  },
};
</script>
<style lang="scss" scoped>
.adjust {
  display: flex;
  flex-direction: column;
  height: 100%;
  .header {
    flex: 0 0 auto;
    margin-bottom: 20px;
  }
  .content {
    flex: 1 1 auto;
    height: 40vh;
  }
}

.heard-content {
  display: flex;
  align-items: center;
  .item {
    display: flex;
    align-items: center;
    margin-right: 10px;
    margin-bottom: 5px;
    .label {
      flex: 0 0 auto;
      &::after {
        content: "：";
      }
    }
  }
  .btn {
    margin-right: 10px;
  }
}
</style>