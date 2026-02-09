<template>
  <el-dialog
    title="失败原因"
    :visible="visible"
    width="1000px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <div style="height: 400px;overflow: auto;">
      <el-table :data="tableData" style="width: 100%;height: 400px;overflow: auto;"  v-loading="loading" border >
        <el-table-column prop="materialDesc" :label="this.$t('物料描述')" show-overflow-tooltip width="320">

        </el-table-column>
        <el-table-column prop="checkContent" :label="this.$t('ui.data.checkDialog.reason')" show-overflow-tooltip >
          <template v-slot="scope">
            <div  class="error-message" v-html="scope.row.checkContent"></div>
          </template>
        </el-table-column>
      </el-table>

    </div>
    <div ref="pageRef" class="page-table-page">
            <el-pagination
              style="text-align:right;margin-top:5px"
              background
              layout="total, sizes, prev, pager, next, jumper"
              :currentPage=pages.current
              :pageSize=pages.pageSize
              :total=pages.total
              :pageSizes=pages.pageSizes
             @size-change="handleSizeChange"
      @current-change="handleCurrentChange"

            ></el-pagination>
          </div>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="hide">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import { checkReason } from "@/api/factory/console";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      actionData: {},
      tableData: [],
      pages: {
        current: 1,
        pageSize: 10,
        total: 0,
        pageSizes: [10, 20, 50, 100],
      },
    };
  },
  computed: {},
  methods: {
    //utils
    show(data) {
      this.pages={
        current: 1,
        pageSize: 10,
        total: 0,
        pageSizes: [10, 20, 50, 100],
      }
      this.visible = true;
      this.actionData = data;
      this.getReasonList(data);
    },
    handleSizeChange(val) {
      this.$set(this.pages, "pageSize", val);
      this.$set(this.pages, "current", 1);

      this.getReasonList(this.actionData);
    },
    handleCurrentChange(val) {
      this.$set(this.pages, "current", val);
      this.getReasonList(this.actionData);
    },
    async getReasonList(data) {
      this.loading = true;
      let parms = {
        pageNum: this.pages.current,
        pageSize: this.pages.pageSize,
        checkItem: "06",
        ...data,
      };
      try {
        let res = await checkReason(parms);
        this.tableData = res.rows;
        this.$set(this.pages, "total", res.total);
      } catch (err) {
        console.log(err);
      } finally {
        this.loading = false;
      }
    },
    hide() {
      this.visible = false;
    },

    handleConfirm() {},
  },
};
</script>
