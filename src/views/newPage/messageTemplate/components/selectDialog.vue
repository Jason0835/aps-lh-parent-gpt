<template>
  <el-dialog
    title="关联用户"
    :visible="visible"
    width="900px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
  <div style="height:400px;">
    <page-table
      :calcHeight="true"
      tableRef="cxFixedMachineMainTable"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
      :toolbar="false"
    >
    </page-table>
  </div>

    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>


import { listTemplate, removeTemplate } from "@/api/newPage/messageTemplate";
import {addUser, changeUserStatus, delUser, deptTreeSelect, getUser, listUser, resetUserPwd, updateUser,} from "@/api/system/user";
//components

export default {


  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      visible: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {

    columns() {
      let columns = [
        { type: "selection", fixed: "left" },

        {
          prop: "userName",
          label: this.$t("登录账号"),
        },

        {
          prop: "nickName",
          label: this.$t("用户名称"),
        },
        {
          prop: "deptName",
          label: this.$t("部门名称"),
        },
        {
          prop: "phonenumber",
          label: this.$t("手机号码"),
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "userName",
          label: this.$t("登录账号"),
        },

        {
          prop: "nickName",
          label: this.$t("用户名称"),
        },

        {
          prop: "phonenumber",
          label: this.$t("手机号码"),
        },
      ];
    },
  },
  methods: {
    show(data) {
      this.visible = true;
      this.getList()
    },
    hide() {
      this.visible = false;
    },
    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },

    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };

      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }

      if (params.createTime && params.createTime[0]) {
        params.beginDate = params.createTime[0];
        params.endDate = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    async handleConfirm() {
      try {
        this.loading = true;

        // const res = await editTemplate(params);
        // this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listUser(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>