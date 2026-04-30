<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="400px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <el-date-picker
      class="w100"
      v-model="yearMonth"
      type="month"
      value-format="yyyy-MM"
      :clearable="false"
      :disabled="loading"
    />

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

import { syncOutSaleOrder } from "@/api/monthplan/monthSaleOrderPlan";

export default {
  components: {},
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      yearMonth: null,
    };
  },
  computed: {
    title: function () {
      return "抓取";
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await syncOutSaleOrder(params);
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
    show() {
      this.visible = true;
      this.yearMonth =  moment().format("yyyy-MM")
    },
    hide() {
      this.yearMonth = null;
      this.visible = false;
    },
    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    handleConfirm() {
      const date = this.yearMonth.split("-");
      this.save({
        years: date[0],
        months: date[1],
      });
    },
  },
};
</script>
